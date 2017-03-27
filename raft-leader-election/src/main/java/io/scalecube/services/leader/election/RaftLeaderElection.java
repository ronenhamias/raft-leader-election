package io.scalecube.services.leader.election;

import io.scalecube.services.Microservices;
import io.scalecube.services.ServiceCall;
import io.scalecube.services.ServiceHeaders;
import io.scalecube.services.ServiceInstance;
import io.scalecube.services.leader.election.api.HeartbeatRequest;
import io.scalecube.services.leader.election.api.HeartbeatResponse;
import io.scalecube.services.leader.election.api.Leader;
import io.scalecube.services.leader.election.api.LeaderElectionService;
import io.scalecube.services.leader.election.api.VoteRequest;
import io.scalecube.services.leader.election.api.VoteResponse;
import io.scalecube.services.leader.election.clock.LogicalClock;
import io.scalecube.services.leader.election.clock.LogicalTimestamp;
import io.scalecube.services.leader.election.state.State;
import io.scalecube.services.leader.election.state.StateMachine;
import io.scalecube.transport.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class RaftLeaderElection implements LeaderElectionService {

  private final StateMachine stateMachine;

  private Microservices microservices;

  private final LogicalClock clock = new LogicalClock();

  private final AtomicReference<LogicalTimestamp> currentTerm = new AtomicReference<LogicalTimestamp>();

  private final JobScheduler timeoutScheduler;

  private final JobScheduler heartbeatScheduler;

  private ServiceCall dispatcher;

  private String candidateId;

  private final int timeout;

  private final Config config;

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
  public CompletableFuture<Leader> leader() {

    return null;
  }

  
  public void start(Microservices seed) {
    this.microservices = seed;
    this.candidateId = microservices.cluster().member().id();
    this.dispatcher = microservices.dispatcher().create();
    this.stateMachine.transition(State.FOLLOWER, currentTerm.get()); 
  }
  
  private Consumer onHeartbeatNotRecived() {
    return toCandidate -> {
      this.timeoutScheduler.stop();
      this.currentTerm.set(clock.tick(currentTerm.get()));
      this.stateMachine.transition(State.CANDIDATE, currentTerm.get());
    };
  }

  @Override
  public CompletableFuture<HeartbeatResponse> onHeartbeat(HeartbeatRequest request) {
    
    this.timeoutScheduler.reset(this.timeout);
    
    LogicalTimestamp term = LogicalTimestamp.fromBytes(request.term());
    //System.out.println("Recived Heartbeat from: " +  request.memberId() + " term: " + term + " current-term: " +  currentTerm.get());
    
    if(currentTerm.get().isBefore(term)) {
      currentTerm.set(term);
    }
    stateMachine.transition(State.FOLLOWER, term);
    return CompletableFuture.completedFuture(new HeartbeatResponse(currentTerm.get().toBytes()));
  }

  @Override
  public CompletableFuture<VoteResponse> onRequestVote(VoteRequest request) {
    LogicalTimestamp term = LogicalTimestamp.fromBytes(request.term());
    
    boolean voteGranted = currentTerm.get().isBefore(term);
    
    System.out.println("request vote" + currentTerm.get() + " vote granted " + voteGranted);
    
    if(currentTerm.get().isBefore(term)){
      currentTerm.set(term);
    }
    
    return CompletableFuture.completedFuture(
        new VoteResponse(voteGranted));
  }

  

  /**
   * find all leader election services that are remote and send current term to all of them.
   * 
   * @return consumer.
   */
  private Consumer sendHeartbeat() {

    return heartbeat -> {
      //System.out.println("Sending Heartbeat from: " +  this.candidateId);
      List<ServiceInstance> services = findPeersServiceInstances();
      services.forEach(instance -> {
        if (LeaderElectionService.SERVICE_NAME.equals(instance.serviceName()) && !instance.isLocal()) {

          try {
            dispatcher.invoke(composeRequest("heartbeat",
                new HeartbeatRequest(currentTerm.get().toBytes(),this.candidateId)), instance)
                .whenComplete((success, error) -> {
                  HeartbeatResponse response = success.data();
                  if (currentTerm.get().isBefore(LogicalTimestamp.fromBytes(response.term()))) {
                    currentTerm.set(LogicalTimestamp.fromBytes(response.term()));
                    System.out.println(currentTerm.get());
                  }
                });
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      });
    };
  }

  private void sendElectionCampaign() {
    List<ServiceInstance> services = findPeersServiceInstances();
    CountDownLatch voteLatch = new CountDownLatch((services.size() / 2) + 1);

    services.forEach(instance -> {
      try {
        System.out.println(candidateId +  " send vote current term" + currentTerm.get() + " to node " + instance.memberId());
        dispatcher.invoke(composeRequest("vote", new VoteRequest(
            currentTerm.get().toBytes(),
            candidateId)), instance).whenComplete((success, error) -> {
              VoteResponse vote = success.data();
              if (vote.granted()) {
                voteLatch.countDown();
              }
            });
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    });

    try {
      voteLatch.await(3, TimeUnit.SECONDS);
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
      System.out.println(this.microservices.cluster().member().id() + " became leader");
      timeoutScheduler.stop();
      heartbeatScheduler.start(config.heartbeatInterval());
    };
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
      System.out.println(this.microservices.cluster().member().id() + " became Candidate");
      heartbeatScheduler.stop();
      currentTerm.set(clock.tick());
      sendElectionCampaign();
    };
  }

  /**
   * node became follower when it initiates
   * 
   * @return
   */
  private Consumer becomeFollower() {
    return follower -> {
      System.out.println(this.microservices.cluster().member().id() + " became Follower");
      heartbeatScheduler.stop();
      timeoutScheduler.start(this.timeout);
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
    this.microservices.services().forEach( instance-> {
      if(LeaderElectionService.SERVICE_NAME.equals(instance.serviceName()) && !instance.isLocal()){
        list.add(instance);
      }      
    });
   
    return list;
  }

 
}
