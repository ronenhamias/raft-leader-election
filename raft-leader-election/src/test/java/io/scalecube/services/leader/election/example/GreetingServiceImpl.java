package io.scalecube.services.leader.election.example;

import io.scalecube.services.Microservices;
import io.scalecube.services.annotations.AfterConstruct;
import io.scalecube.services.leader.election.Config;
import io.scalecube.services.leader.election.JobScheduler;
import io.scalecube.services.leader.election.RaftLeaderElection;

import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class GreetingServiceImpl extends RaftLeaderElection implements GreetingService {

  private Microservices ms;
  JobScheduler scheduler;
  
  @AfterConstruct
  public void start(Microservices ms) {
    super.start(ms);
    this.ms = ms;
  }

  public GreetingServiceImpl(Config config) {
    super(GreetingService.class, config);
  }

  @Override
  public Mono<String> sayHello(String name) {
    return Mono.just("hello: " + name);
  }

  @Override
  public void onBecomeLeader() {
    System.out.println(ms.cluster().member().id() + " (" + this.currentTerm().toLong() + ") >>>>>>>    +++ Become A Leader +++");
    scheduler = new JobScheduler(leaderIsWorking());
    scheduler.start(1000);
  }

  private Consumer leaderIsWorking() {
    return doingSomeWork -> {
      System.out.println(ms.id() + "I am working...");
    };
  }

  @Override
  public void onBecomeCandidate() {
    System.out.println(ms.cluster().member().id() + " (" + this.currentTerm().toLong() + ") ?? Become A Candidate");
    scheduler.stop();
  }

  @Override
  public void onBecomeFollower() {
    System.out.println(ms.cluster().member().id() + " (" + this.currentTerm().toLong() + ") << Become A Follower");
    scheduler.stop();
  }
}
