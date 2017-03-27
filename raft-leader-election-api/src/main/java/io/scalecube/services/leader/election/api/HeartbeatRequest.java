package io.scalecube.services.leader.election.api;


public class HeartbeatRequest {

  private final byte[] term;
  private final String memberId;

  public HeartbeatRequest(byte[] term, String memberId) {
    this.term = term;
    this.memberId = memberId;
  }

  public byte[] term() {
    return term;
  }

  public String memberId() {
    return memberId;
  }
  
}

