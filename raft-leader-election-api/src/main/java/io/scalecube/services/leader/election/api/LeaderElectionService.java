package io.scalecube.services.leader.election.api;


import io.scalecube.services.annotations.ServiceMethod;

import reactor.core.publisher.Mono;

public interface LeaderElectionService {

  @ServiceMethod("leader")
  public Mono<Leader> leader();

  @ServiceMethod("heartbeat")
  Mono<HeartbeatResponse> onHeartbeat(HeartbeatRequest request);
  
  @ServiceMethod("vote")
  Mono<VoteResponse> onRequestVote(VoteRequest request);
  
}
