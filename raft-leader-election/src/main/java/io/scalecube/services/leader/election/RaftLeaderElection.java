package io.scalecube.services.leader.election;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.scalecube.services.Microservices;
import io.scalecube.services.ServiceCall;
import io.scalecube.services.ServiceHeaders;
import io.scalecube.services.ServiceInstance;
import io.scalecube.services.leader.election.api.AppendStatus;
import io.scalecube.services.leader.election.api.EntryRequest;
import io.scalecube.services.leader.election.api.EntryResponse;
import io.scalecube.services.leader.election.api.HeartbeatRequest;
import io.scalecube.services.leader.election.api.HeartbeatResponse;
import io.scalecube.services.leader.election.api.Leader;
import io.scalecube.services.leader.election.api.LeaderElectionGossip;
import io.scalecube.services.leader.election.api.LeaderElectionService;
import io.scalecube.services.leader.election.api.LogEntry;
import io.scalecube.services.leader.election.api.LogicalTimestamp;
import io.scalecube.services.leader.election.api.MemberLog;
import io.scalecube.services.leader.election.api.RaftLog;
import io.scalecube.services.leader.election.api.VoteRequest;
import io.scalecube.services.leader.election.api.VoteResponse;

import io.scalecube.services.leader.election.state.State;
import io.scalecube.services.leader.election.state.StateMachine;
import io.scalecube.transport.Message;

public class RaftLeaderElection implements LeaderElectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LeaderElectionService.class);

  private final StateMachine stateMachine;

  private Microservices microservices;

  private final RaftLog raftLog;

  private final JobScheduler timeoutScheduler;

  private final JobScheduler heartbeatScheduler;

  private ServiceCall dispatcher;

  private String memberId;

  private final int timeout;

  private final AtomicReference<String> currentLeader = new AtomicReference<String>("");

  private final int heartbeatInterval;

  public RaftLeaderElection(Config config) {
    this.raftLog = config.raftLog();
    this.raftLog.nextTerm();
    this.heartbeatInterval = config.heartbeatInterval();

    this.timeout = new Random().nextInt(config.timeout() - (config.timeout() / 2)) + (config.timeout() / 2);

    this.stateMachine = StateMachine.builder()
        .init(State.INACTIVE)
        .addTransition(State.INACTIVE, State.FOLLOWER)
        .addTransition(State.FOLLOWER, State.CANDIDATE)
        .addTransition(State.CANDIDATE, State.LEADER)
        .addTransition(State.LEADER, State.FOLLOWER)
        .build();

    this.stateMachine.on(State.FOLLOWER, becomeFollower());
    this.stateMachine.on(State.CANDIDATE, becomeCandidate());
    this.stateMachine.on(State.LEADER, becomeLeader());

    this.timeoutScheduler = new JobScheduler(onHeartbeatNotRecived());
    this.heartbeatScheduler = new JobScheduler(sendHeartbeat());
  }

  private boolean isLeader() {
    return stateMachine.currentState().equals(State.LEADER);
  }

  @Override
  public CompletableFuture<Leader> leader() {
    return CompletableFuture.completedFuture(new Leader(this.memberId, this.currentLeader.get()));
  }

  public void start(Microservices seed) {
    this.microservices = seed;
    this.memberId = microservices.cluster().member().id();
    this.dispatcher = microservices.dispatcher().create();
    this.stateMachine.transition(State.FOLLOWER, raftLog.currentTerm());
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
      sendHeartbeat();

      return CompletableFuture.completedFuture(new EntryResponse(
          AppendStatus.AcceptedNotCommited,
          this.currentLeader.get()));

    } else {
      return CompletableFuture.completedFuture(new EntryResponse(
          AppendStatus.NotLeader,
          this.currentLeader.get()));
    }
  }

  @Override
  public CompletableFuture<HeartbeatResponse> onHeartbeat(HeartbeatRequest request) {
    LOGGER.debug("member [{}] recived heartbeat request: [{}]", this.memberId, request);
    this.timeoutScheduler.reset(this.timeout);

    if (raftLog.currentTerm().isBefore(request.term())) {
      LOGGER.info("member: [{}] currentTerm: [{}] is before: [{}] setting new seen term.", this.memberId,
          raftLog.currentTerm(), request.term());
      raftLog.currentTerm(request.term());
    }

    if (!stateMachine.currentState().equals(State.FOLLOWER)) {
      LOGGER.info("member [{}] currentState [{}] and recived heartbeat. becoming FOLLOWER.", this.memberId,
          stateMachine.currentState());
      stateMachine.transition(State.FOLLOWER, request.term());
    }

    this.currentLeader.set(request.memberId());
    this.raftLog.append(request.entries());
    return CompletableFuture.completedFuture(new HeartbeatResponse(this.memberId, raftLog.currentTerm().toBytes()));
  }

  @Override
  public CompletableFuture<VoteResponse> onRequestVote(VoteRequest request) {

    LogicalTimestamp term = LogicalTimestamp.fromBytes(request.term());

    boolean voteGranted = raftLog.currentTerm().isBefore(term);
    LOGGER.info("member [{}:{}] recived vote request: [{}] voteGranted: [{}].", this.memberId,
        stateMachine.currentState(), request, voteGranted);

    if (raftLog.currentTerm().isBefore(term)) {
      LOGGER.info("member: [{}] currentTerm: [{}] is before: [{}] setting new seen term.", this.memberId,
          raftLog.currentTerm(), term);
      raftLog.currentTerm(term);
    }

    return CompletableFuture.completedFuture(
        new VoteResponse(voteGranted, this.memberId));
  }



  /**
   * find all leader election services that are remote and send current term to all of them.
   * 
   * @return consumer.
   */
  private Consumer sendHeartbeat() {

    return heartbeat -> {

      List<ServiceInstance> services = findPeersServiceInstances();

      services.forEach(instance -> {
        if (raftLog.getMemberLog(instance.memberId()) == null) {
          raftLog.setMemberLog(instance.memberId(), new MemberLog());
        }
        LOGGER.debug("member: [{}] sending heartbeat: [{}].", this.memberId, instance.memberId());
        try {
          dispatcher.invoke(HeartbeatRequest.builder()
              .term(raftLog.currentTerm())
              .memberId(this.memberId)
              .nextIndex(raftLog.getMemberLog(instance.memberId()).logIndex())
              .entries(raftLog.replicateEntries(instance.memberId()).get())
              .build(), instance)

              .whenComplete((success, error) -> {
                HeartbeatResponse response = success.data();

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
    CountDownLatch consensus = new CountDownLatch((services.size() / 2));

    services.forEach(instance -> {
      try {
        LOGGER.info("member: [{}] sending vote request to: [{}].", this.memberId, instance.memberId());
        dispatcher.invoke(composeRequest("vote", new VoteRequest(
            raftLog.currentTerm().toBytes(),
            memberId)),
            instance)

            .whenComplete((success, error) -> {
              VoteResponse vote = success.data();
              LOGGER.info("member: [{}] recived vote response: [{}].", this.memberId, vote);
              if (vote.granted()) {
                consensus.countDown();
              }
            });

      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    });

    try {
      consensus.await(3, TimeUnit.SECONDS);
      stateMachine.transition(State.LEADER, raftLog.currentTerm());
    } catch (InterruptedException e) {
      stateMachine.transition(State.FOLLOWER, raftLog.currentTerm());
    }
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
      LOGGER.info("member: [{}] has become: [{}].", this.memberId, stateMachine.currentState());
      heartbeatScheduler.stop();
      raftLog.nextTerm();
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

      // spread the gossip about me as follower.
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
    List<ServiceInstance> list = new ArrayList<>();
    this.microservices.services().forEach(instance -> {
      if (LeaderElectionService.SERVICE_NAME.equals(instance.serviceName()) && !instance.isLocal()) {
        list.add(instance);
      }
    });

    return list;
  }

  public void on(State state, Consumer func) {
    stateMachine.on(state, func);
  }



}
