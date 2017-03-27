package io.scalecube.services.leader.election.api;

public class HeartbeatResponse {

  private byte[] term;

  public HeartbeatResponse(byte[] term) {
    this.term = term;
  }

  public byte[] term() {
    return term;
  }
}
