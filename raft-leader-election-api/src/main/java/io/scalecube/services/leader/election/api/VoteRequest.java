package io.scalecube.services.leader.election.api;

public class VoteRequest {

  private byte[] term;
  
  private String candidateId;

  public VoteRequest(byte[] bytes, String candidateId) {
    this.term = term;
    this.candidateId = candidateId;
  }

  public byte[] term() { 
    return term;
  }
}
