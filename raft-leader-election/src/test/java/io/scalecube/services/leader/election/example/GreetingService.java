package io.scalecube.services.leader.election.example;

import io.scalecube.services.annotations.Service;
import io.scalecube.services.annotations.ServiceMethod;
import io.scalecube.services.leader.election.api.LeaderElectionService;

import reactor.core.publisher.Mono;

@Service("greetings")
public interface GreetingService extends LeaderElectionService{
  
  @ServiceMethod
  Mono<String> sayHello(String name);
  
} 
