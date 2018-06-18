package io.scalecube.services.leader.election;

import io.scalecube.services.Microservices;
import io.scalecube.services.ServiceCall;
import io.scalecube.services.ServiceEndpoint;
import io.scalecube.services.ServiceReference;
import io.scalecube.services.api.ServiceMessage;
import io.scalecube.services.leader.election.api.HeartbeatRequest;
import io.scalecube.services.leader.election.api.HeartbeatResponse;
import io.scalecube.services.leader.election.api.Leader;
import io.scalecube.services.leader.election.api.LeaderElectionGossip;
import io.scalecube.services.leader.election.api.LeaderElectionService;
import io.scalecube.services.leader.election.api.VoteRequest;
import io.scalecube.services.leader.election.api.VoteResponse;
import io.scalecube.services.leader.election.clock.LogicalClock;
import io.scalecube.services.leader.election.clock.LogicalTimestamp;
import io.scalecube.services.leader.election.state.State;
import io.scalecube.services.leader.election.state.StateMachine;
import io.scalecube.transport.Address;
import io.scalecube.transport.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class RaftLeaderElection implements LeaderElectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LeaderElectionService.class);
  
  private final StateMachine stateMachine;

  private Microservices microservices;

  private final LogicalClock clock = new LogicalClock();

  private final AtomicReference<LogicalTimestamp> currentTerm = new AtomicReference<LogicalTimestamp>();

  private final JobScheduler timeoutScheduler;

  private final JobScheduler heartbeatScheduler;

  private ServiceCall dispatcher;

  private String memberId;

  private final int timeout;

  private final Config config;

  private final AtomicReference<String> currentLeader = new AtomicReference<String>("");
  
  public RaftLeaderElection(Config config) {
    this.config = config;
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

    this.currentTerm.set(clock.tick());
    this.timeoutScheduler = new JobScheduler(onHeartbeatNotRecived());
    this.heartbeatScheduler = new JobScheduler(sendHeartbeat());
  }
  
  @Override
  public Mono<Leader> leader() {
    return Mono.just(new Leader(this.memberId, this.currentLeader.get()));
  }
  
  public void start(Microservices seed) {
    this.microservices = seed;
    this.memberId = microservices.cluster().member().id();
    this.dispatcher = microservices.call().create();
    this.stateMachine.transition(State.FOLLOWER, currentTerm.get()); 
  }
  
  private Consumer onHeartbeatNotRecived() {
    return toCandidate -> {
      this.timeoutScheduler.stop();
      this.currentTerm.set(clock.tick(currentTerm.get()));
      this.stateMachine.transition(State.CANDIDATE, currentTerm.get());
      LOGGER.info("member: [{}] didnt recive heartbeat until timeout: [{}ms] became: [{}]", this.memberId, timeout, stateMachine.currentState());
    };
  }

  @Override
  public Mono<HeartbeatResponse> onHeartbeat(HeartbeatRequest request) {
    LOGGER.debug("member [{}] recived heartbeat request: [{}]", this.memberId, request);
    this.timeoutScheduler.reset(this.timeout);
    
    LogicalTimestamp term = LogicalTimestamp.fromBytes(request.term());
    
    if(currentTerm.get().isBefore(term)) {
      LOGGER.info("member: [{}] currentTerm: [{}] is before: [{}] setting new seen term.", this.memberId, currentTerm.get(),term);
      currentTerm.set(term);
    }
    
    if(!stateMachine.currentState().equals(State.FOLLOWER)){
      LOGGER.info("member [{}] currentState [{}] and recived heartbeat. becoming FOLLOWER.", this.memberId, stateMachine.currentState());
      stateMachine.transition(State.FOLLOWER, term);
    }
    
    this.currentLeader.set(request.memberId());
    
    return Mono.just(new HeartbeatResponse(this.memberId,currentTerm.get().toBytes()));
  }

  @Override
  public Mono<VoteResponse> onRequestVote(VoteRequest request) {
    
    LogicalTimestamp term = LogicalTimestamp.fromBytes(request.term());
    
    boolean voteGranted = currentTerm.get().isBefore(term);
    LOGGER.info("member [{}:{}] recived vote request: [{}] voteGranted: [{}].", this.memberId,stateMachine.currentState(), request, voteGranted);
    
    if(currentTerm.get().isBefore(term)){
      LOGGER.info("member: [{}] currentTerm: [{}] is before: [{}] setting new seen term.", this.memberId, currentTerm.get(),term);
      currentTerm.set(term);
    }
    
    return Mono.just(new VoteResponse(voteGranted,this.memberId));
  }

  

  /**
   * find all leader election services that are remote and send current term to all of them.
   * 
   * @return consumer.
   */
  private Consumer sendHeartbeat() {

    return heartbeat -> {
   
      List<ServiceReference> services = findPeersServiceInstances();
      
      services.forEach(instance -> {
        LOGGER.debug("member: [{}] sending heartbeat: [{}].", this.memberId, instance.endpointId());
          try {
            ServiceMessage request = composeRequest("heartbeat",new HeartbeatRequest(currentTerm.get().toBytes(),this.memberId));
            Address address = Address.create(instance.host(), instance.port());
            dispatcher.requestOne(request, HeartbeatRequest.class, address)
              .doAfterSuccessOrError((success, error) -> {
                  HeartbeatResponse response = success.data();
                  LogicalTimestamp term = LogicalTimestamp.fromBytes(response.term());
                  if(currentTerm.get().isBefore(term)){
                    LOGGER.info("member: [{}] currentTerm: [{}] is before: [{}] setting new seen term.", this.memberId, currentTerm.get(),term);
                    currentTerm.set(term);
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
    List<ServiceReference> services = findPeersServiceInstances();
    CountDownLatch consensus = new CountDownLatch((services.size() / 2));

    services.forEach(instance -> {
      try {
        LOGGER.info("member: [{}] sending vote request to: [{}].", this.memberId, instance.endpointId());
        ServiceMessage request = composeRequest("vote", new VoteRequest(currentTerm.get().toBytes(),
            instance.endpointId()));
        
        dispatcher.requestOne(request, VoteRequest.class, Address.create(instance.host(), instance.port()))
          .doAfterSuccessOrError((success, error) -> {
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
      stateMachine.transition(State.LEADER, currentTerm);
    } catch (InterruptedException e) {
      stateMachine.transition(State.FOLLOWER, currentTerm);
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
      heartbeatScheduler.start(config.heartbeatInterval());
      this.currentLeader.set(this.memberId);
      
      // spread the gossip about me as a new leader.
      this.microservices.cluster().spreadGossip(newLeaderElectionGossip(State.LEADER));
    };
  }

  private Message newLeaderElectionGossip(State state) {
    return Message.builder()
        .header(LeaderElectionGossip.TYPE, state.toString())
        .data(new LeaderElectionGossip(this.memberId,this.currentLeader.get(), this.currentTerm.get().toLong(), this.microservices.cluster().address()))
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
      currentTerm.set(clock.tick());
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
  
  private ServiceMessage composeRequest(String action, Object data) {
    return ServiceMessage.builder()
        .header("service-request", LeaderElectionService.SERVICE_NAME)
        .header("service-method", action)
        .data(data).build();
  }

  private List<ServiceReference> findPeersServiceInstances() {
    return microservices.serviceRegistry()
        .lookupService(filter->
        filter.namespace().equals(LeaderElectionService.SERVICE_NAME)
            );
  }

  public void on(State state, Consumer func) {
    stateMachine.on(state,func);
  }
}
