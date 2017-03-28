package io.scalecube.services.leader.election.api;

import java.util.Arrays;

public class HeartbeatRequest {

  @Override
  public String toString() {
    return "HeartbeatRequest [term=" + Arrays.toString(term) + ", memberId=" + memberId + "]";
  }

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

