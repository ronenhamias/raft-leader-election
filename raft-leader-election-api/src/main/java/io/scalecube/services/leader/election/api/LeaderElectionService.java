package io.scalecube.services.leader.election.api;

import io.scalecube.services.annotations.Service;
import io.scalecube.services.annotations.ServiceMethod;

import java.util.concurrent.CompletableFuture;

@Service(LeaderElectionService.SERVICE_NAME)
public interface LeaderElectionService {

  String SERVICE_NAME = "leader-election-service";

  @ServiceMethod("leader")
  public CompletableFuture<Leader> leader();

  @ServiceMethod("heartbeat")
  CompletableFuture<AppendEntriesResponse> appendEntries(AppendEntriesRequest request);

  @ServiceMethod("vote")
  CompletableFuture<VoteResponse> onRequestVote(VoteRequest request);

  @ServiceMethod("append")
  CompletableFuture<EntryResponse> append(EntryRequest request);
  
}
