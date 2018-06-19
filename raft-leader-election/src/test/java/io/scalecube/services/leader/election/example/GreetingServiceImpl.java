package io.scalecube.services.leader.election.example;

import io.scalecube.services.Microservices;
import io.scalecube.services.annotations.AfterConstruct;
import io.scalecube.services.leader.election.Config;
import io.scalecube.services.leader.election.RaftLeaderElection;

import reactor.core.publisher.Mono;

public class GreetingServiceImpl extends RaftLeaderElection implements GreetingService {

  private Microservices ms;

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
    System.out.println(ms.cluster().member().id() + " >>>>>>>    +++ Become A Leader +++");
  }

  @Override
  public void onBecomeCandidate() {
    System.out.println(ms.cluster().member().id() + " ?? Become A Candidate");
  }

  @Override
  public void onBecomeFollower() {
    System.out.println(ms.cluster().member().id() + " << Become A Follower");
  }

}
