package io.scalecube.services.leader.election.api;

import io.scalecube.services.annotations.Service;
import io.scalecube.services.annotations.ServiceMethod;

import java.util.concurrent.CompletableFuture;

@Service(LeaderElectionService.SERVICE_NAME)
public interface LeaderElectionService {

  public static final String SERVICE_NAME = "scalecube-leader-election";
 
  @ServiceMethod()
  public CompletableFuture<Leader> leader();

  @ServiceMethod("heartbeat")
  CompletableFuture<HeartbeatResponse> onHeartbeat(HeartbeatRequest request);
  
  @ServiceMethod("vote")
  CompletableFuture<VoteResponse> onRequestVote(VoteRequest request);
}
