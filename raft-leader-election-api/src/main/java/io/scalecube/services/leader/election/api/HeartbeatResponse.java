package io.scalecube.services.leader.election.api;

import java.util.Arrays;

public class HeartbeatResponse {

  @Override
  public String toString() {
    return "HeartbeatResponse [term=" + Arrays.toString(term) + ", memberId=" + memberId + "]";
  }

  private final byte[] term;

  private final String memberId;

  public HeartbeatResponse(String memberId, byte[] term) {
    this.term = term;
    this.memberId = memberId;
  }

  public LogicalTimestamp term() {
    return LogicalTimestamp.fromBytes(term);
  }

  public String memberId() {
    return memberId;
  }
}
