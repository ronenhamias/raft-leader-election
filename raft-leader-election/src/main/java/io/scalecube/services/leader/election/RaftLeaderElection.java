package io.scalecube.services.leader.election;

import io.scalecube.services.Microservices;
import io.scalecube.services.ServiceCall;
import io.scalecube.services.ServiceHeaders;
import io.scalecube.services.ServiceInstance;
import io.scalecube.services.annotations.Inject;
import io.scalecube.services.leader.election.api.AppendEntriesRequest;
import io.scalecube.services.leader.election.api.AppendEntriesResponse;
import io.scalecube.services.leader.election.api.AppendStatus;
import io.scalecube.services.leader.election.api.EntryRequest;
import io.scalecube.services.leader.election.api.EntryResponse;
import io.scalecube.services.leader.election.api.Leader;
import io.scalecube.services.leader.election.api.LeaderElectionGossip;
import io.scalecube.services.leader.election.api.LeaderElectionService;
import io.scalecube.services.leader.election.api.LogicalTimestamp;
import io.scalecube.services.leader.election.api.MemberLog;
import io.scalecube.services.leader.election.api.RaftLog;
import io.scalecube.services.leader.election.api.VoteRequest;
import io.scalecube.services.leader.election.api.VoteResponse;
import io.scalecube.services.leader.election.state.State;
import io.scalecube.services.leader.election.state.StateMachine;
import io.scalecube.transport.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RaftLeaderElection implements LeaderElectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LeaderElectionService.class);

  private StateMachine stateMachine;

  @Inject
  private Microservices microservices;

  private ServiceCall dispatcher;

  private final Config raftLogBuilder;

  private RaftLog raftLog;

  private JobScheduler timeoutScheduler;

  private JobScheduler heartbeatScheduler;

  private String memberId;

  private int timeout;

  private final AtomicReference<String> currentLeader = new AtomicReference<String>("");

  private int heartbeatInterval;

  private Members members;

  private long electionTimeout;

  public RaftLeaderElection(Config config) {
    this.raftLogBuilder = config;
  }

  public void start() throws Exception {
    this.members = Members.create();
    this.memberId = microservices.cluster().member().id();
    this.raftLogBuilder.builder().memberId(memberId);
    this.raftLog = this.raftLogBuilder.builder().build();
    this.electionTimeout = this.raftLogBuilder.electionTimeout();
    this.heartbeatInterval = this.raftLogBuilder.heartbeatInterval();
    this.timeout = calculateTimeout(this.raftLogBuilder.timeout());

    this.stateMachine = StateMachine.builder()
        .init(State.INACTIVE)
        .addTransition(State.INACTIVE, State.FOLLOWER)
        .addTransition(State.FOLLOWER, State.CANDIDATE)
        .addTransition(State.CANDIDATE, State.FOLLOWER)
        .addTransition(State.CANDIDATE, State.LEADER)
        .addTransition(State.LEADER, State.FOLLOWER)
        .build();

    this.stateMachine.on(State.FOLLOWER, becomeFollower());
    this.stateMachine.on(State.CANDIDATE, becomeCandidate());
    this.stateMachine.on(State.LEADER, becomeLeader());

    this.raftLog.nextTerm();

    this.dispatcher = microservices.dispatcher().create();
    this.stateMachine.transition(State.FOLLOWER, raftLog.currentTerm());
    
    this.timeoutScheduler = new JobScheduler(onHeartbeatNotRecived());
    this.heartbeatScheduler = new JobScheduler(sendAppendEntriesRequest());
  }

  private int calculateTimeout(int timeout) {
    return new Random().nextInt(timeout - (timeout / 2)) + (timeout / 2);
  }

  private boolean isLeader() {
    return stateMachine.currentState().equals(State.LEADER);
  }

  @Override
  public CompletableFuture<Leader> leader() {
    return CompletableFuture.completedFuture(new Leader(this.memberId, this.currentLeader.get()));
  }



  private Consumer onHeartbeatNotRecived() {
    return toCandidate -> {
      this.timeoutScheduler.stop();
      raftLog.nextTerm();
      this.stateMachine.transition(State.CANDIDATE, raftLog.currentTerm());
      LOGGER.info("member: [{}] didnt recive heartbeat until timeout: [{}ms] became: [{}]", this.memberId, timeout,
          stateMachine.currentState());
    };
  }

  @Override
  public CompletableFuture<EntryResponse> append(EntryRequest request) {
    if (this.isLeader()) {
      // first append to my log.
      raftLog.append(request.data());

      // append to the members log.
      sendAppendEntriesRequest();

      return CompletableFuture.completedFuture(new EntryResponse(
          AppendStatus.AcceptedNotCommited,
          this.currentLeader.get()));

    } else {
      return CompletableFuture.completedFuture(new EntryResponse(
          AppendStatus.NotLeader,
          this.currentLeader.get()));
    }
  }

  /*
   * 1. Reply false if term < currentTerm (§5.1) 2. Reply false if log doesn’t contain an entry at prevLogIndex whose
   * term matches prevLogTerm (§5.3) 3. If an existing entry conflicts with a new one (same index but different terms),
   * delete the existing entry and all that follow it (§5.3) 4. Append any new entries not already in the log 5. If
   * leaderCommit > commitIndex, set commitIndex = min(leaderCommit, index of last new entry) Append Entries RPC 1.
   * Reply false if term < currentTerm (§5.1) 2. Reply false if log doesn’t contain an entry at prevLogIndex whose
   * term matches prevLogTerm (§5.3) 3. If an existing entry conflicts with a new one (same index but different terms),
   * delete the existing entry and all that follow it (§5.3) 4. Append any new entries not already in the log 5. If
   * leaderCommit > commitIndex, set commitIndex = min(leaderCommit, index of last new entry)
   */
  @Override
  public CompletableFuture<AppendEntriesResponse> appendEntries(AppendEntriesRequest request) {
    LOGGER.debug("member [{}] recived heartbeat request: [{}]", this.memberId, request);
    this.timeoutScheduler.reset(this.timeout);

    if (raftLog.currentTerm().isBefore(request.prevLogTerm())) {
      LOGGER.info("member: [{}] currentTerm: [{}] is before: [{}] setting new seen term.", this.memberId,
          raftLog.currentTerm(), request.prevLogTerm());
      raftLog.currentTerm(request.prevLogTerm());
      if (!stateMachine.currentState().equals(State.CANDIDATE)) {
        stateMachine.transition(State.FOLLOWER, request.prevLogTerm());
      }
    }

    if (!stateMachine.currentState().equals(State.FOLLOWER)) {
      LOGGER.info("member [{}] currentState [{}] and recived heartbeat. becoming FOLLOWER.", this.memberId,
          stateMachine.currentState());
      stateMachine.transition(State.FOLLOWER, request.prevLogTerm());
    }

    this.currentLeader.set(request.leaderId());

    // consistency check did you have in the last log index an entry with this term?
    if (request.entries() != null && request.entries().length > 0) {
      this.raftLog.append(request.entries());
    }


    return CompletableFuture.completedFuture(new AppendEntriesResponse(
        this.memberId,
        this.raftLog.currentTerm().toLong(),
        this.raftLog.index(),
        this.raftLog.lastLogTerm() == request.prevLogTerm().toLong()));
  }

  /*
   * 1. Reply false if term < currentTerm (§5.1) 2. If votedFor is null or candidateId, and candidates log is at
   * least as up-to-date as receivers log, grant vote (§5.2, §5.4)
   */
  @Override
  public CompletableFuture<VoteResponse> onRequestVote(VoteRequest request) {

    LogicalTimestamp term = request.term();
    
    boolean termCheck = raftLog.currentTerm().isBefore(term);
    boolean logIsUpToDate = logIsUpToDate(request.lastLogIndex(),request.lastLogTerm().toLong());
    
    boolean voteGranted = termCheck && logIsUpToDate;
    
    LOGGER.info("member [{}:{}] recived vote request: [{}] voteGranted: [{} term pass:{} log pass:{}].", this.memberId,
        stateMachine.currentState(), request, voteGranted,termCheck,logIsUpToDate);

    return CompletableFuture.completedFuture(
        new VoteResponse(voteGranted, this.memberId));
  }

  private boolean logIsUpToDate(long lastLogIndex, long lastLogTerm) {
    return lastLogIndex <= raftLog.index() && lastLogTerm >= raftLog.lastLogTerm();
  }

  
  /**
   * find all leader election services that are remote and send current term to all of them.
   * 
   * @return consumer.
   */
  private Consumer sendAppendEntriesRequest() {

    return heartbeat -> {

      List<ServiceInstance> services = findPeersServiceInstances();
      CountDownLatch commitConcensus = new CountDownLatch((services.size() / 2) + 1);
      services.forEach(instance -> {

        members.set(instance.memberId(), MemberLog.create(
            raftLog.index() + 1,
            raftLog.currentTerm().toLong(),
            raftLog.commitedIndex()));

        LOGGER.debug("member: [{}] sending heartbeat: [{}].", this.memberId, instance.memberId());
        try {
          dispatcher.invoke(AppendEntriesRequest.builder()
              .term(raftLog.currentTerm())
              .leaderId(this.memberId)
              .leaderTerm(raftLog.currentTerm().toLong())
              .prevLogIndex(members.get(instance.memberId()).index())
              .prevLogTerm(raftLog.currentTerm().toLong())
              .entries(raftLog.entries(members.get(instance.memberId()).index()))
              .build(), instance)

              .whenComplete((success, error) -> {
                AppendEntriesResponse response = success.data();

                if (response.success()) {
                  // increment accepted entry for member

                  // do we have commit concensus? yes increment commit index.

                } else if (response.lastLogIndex() < raftLog.index()) {
                  this.members.set(response.memberId(),
                      members.updateIndex(response.memberId(), response.lastLogIndex()));
                }

                if (raftLog.currentTerm().isBefore(response.term())) {
                  LOGGER.info("member: [{}] currentTerm: [{}] is before: [{}] setting new seen term.", this.memberId,
                      raftLog.currentTerm(), response.term());
                  raftLog.currentTerm(response.term());
                }
              });


        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      });

    };
  }

  private void sendElectionCampaign() {
    List<ServiceInstance> services = findPeersServiceInstances();
    Quorum<VoteResponse> quorum = Quorum.of((services.size() / 2));

    services.forEach(instance -> {
      try {
        LOGGER.info("member: [{}] sending vote request to: [{}].", this.memberId, instance.memberId());
        dispatcher.invoke(composeRequest("vote", new VoteRequest(
            memberId,
            raftLog.currentTerm().toLong(),
            raftLog.lastLogTerm(),
            raftLog.index())), instance)
            .whenComplete((success, error) -> {
              VoteResponse vote = success.data();
              LOGGER.info("member: [{}] recived vote from [{}] response: [{}].", this.memberId,vote.memberId(), vote);
              if (vote.granted()) {
                quorum.collect(instance.memberId(), vote);
              }
            });

      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    });

    quorum.quorum(electionTimeout, TimeUnit.MILLISECONDS).whenComplete((response, error) -> {
      if (response != null) {
        stateMachine.transition(State.LEADER, raftLog.currentTerm());
        LOGGER.info("member: [{}] was elected - transition to state leader with term[{}].", this.memberId, raftLog.currentTerm());
      } else {
        stateMachine.transition(State.FOLLOWER, raftLog.currentTerm());
        LOGGER.info("member: [{}] failed to elect current term {}.", this.memberId, raftLog.currentTerm());
      }
    });


  }

  /**
   * node becomes leader when most of the peers granted a vote on election process.
   * 
   * @return
   */
  private Consumer becomeLeader() {
    return leader -> {
      
      LOGGER.info("member: [{}] has become: [{}].", this.memberId, stateMachine.currentState());
      timeoutScheduler.stop();
      heartbeatScheduler.start(heartbeatInterval);
      this.currentLeader.set(this.memberId);

      // spread the gossip about me as a new leader.
      this.microservices.cluster().spreadGossip(newLeaderElectionGossip(State.LEADER));
    };
  }

  private Message newLeaderElectionGossip(State state) {
    return Message.builder()
        .header(LeaderElectionGossip.TYPE, state.toString())
        .data(new LeaderElectionGossip(this.memberId, this.currentLeader.get(), raftLog.currentTerm().toLong(),
            this.microservices.cluster().address()))
        .build();
  }

  /**
   * node becomes candidate when no heartbeats received until timeout has reached. when at state candidate node is
   * ticking the term and sending vote requests to all peers. peers grant vote to candidate if peers current term is
   * older than node new term
   * 
   * @return
   */
  private Consumer becomeCandidate() {
   
    
    return election -> {
      raftLog.nextTerm();
      heartbeatScheduler.stop();
      LOGGER.info("member: [{}] has become: [{}].", this.memberId, stateMachine.currentState());
      sendElectionCampaign();
      // spread the gossip about me as candidate.
      this.microservices.cluster().spreadGossip(newLeaderElectionGossip(State.CANDIDATE));
    };
  }

  /**
   * node became follower when it initiates
   * 
   * @return
   */
  private Consumer becomeFollower() {
    return follower -> {
      LOGGER.info("member: [{}] has become: [{}].", this.memberId, stateMachine.currentState());
      heartbeatScheduler.stop();
      timeoutScheduler.start(this.timeout);

      // spread a gossip about me as follower.
      this.microservices.cluster().spreadGossip(newLeaderElectionGossip(State.FOLLOWER));
    };
  }

  private Message composeRequest(String action, Object data) {
    return Message.builder()
        .header(ServiceHeaders.SERVICE_REQUEST, LeaderElectionService.SERVICE_NAME)
        .header(ServiceHeaders.METHOD, action)
        .data(data).build();
  }

  private List<ServiceInstance> findPeersServiceInstances() {
    return this.microservices.services().stream()
        .filter(instance -> (LeaderElectionService.SERVICE_NAME.equals(instance.serviceName()) && !instance.isLocal()))
        .collect(Collectors.toList());
  }

  public void on(State state, Consumer func) {
    stateMachine.on(state, func);
  }



}
