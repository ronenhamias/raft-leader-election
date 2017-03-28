package io.scalecube.services.leader.election.api;

public class VoteRequest {

  private final byte[] term;
  
  private final String candidateId;

  public VoteRequest(byte[] term, String candidateId) {
    this.term = term;
    this.candidateId = candidateId;
  }

  public byte[] term() { 
    return term;
  }
}
