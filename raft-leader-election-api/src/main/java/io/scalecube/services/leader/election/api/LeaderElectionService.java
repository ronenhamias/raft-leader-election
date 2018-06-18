package io.scalecube.services.leader.election.api;

import io.scalecube.services.annotations.Service;
import io.scalecube.services.annotations.ServiceMethod;

import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service(LeaderElectionService.SERVICE_NAME)
public interface LeaderElectionService {

  public static final String SERVICE_NAME = "scalecube-leader-election";
 
  @ServiceMethod("leader")
  public Mono<Leader> leader();

  @ServiceMethod("heartbeat")
  Mono<HeartbeatResponse> onHeartbeat(HeartbeatRequest request);
  
  @ServiceMethod("vote")
  Mono<VoteResponse> onRequestVote(VoteRequest request);
  
}
