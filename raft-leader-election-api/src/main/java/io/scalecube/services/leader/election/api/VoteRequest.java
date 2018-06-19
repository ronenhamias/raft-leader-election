package io.scalecube.services.leader.election.api;

public class VoteRequest {

  private byte[] term;
  
  private String candidateId;

  public VoteRequest() {};
  
  public VoteRequest(byte[] term, String candidateId) {
    this.term = term;
    this.candidateId = candidateId;
  }

  public byte[] term() { 
    return term;
  }
}
