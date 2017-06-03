package io.scalecube.services.leader.election.api;

public class HeartbeatResponse {

  @Override
  public String toString() {
    return "HeartbeatResponse [term=" + term + ", memberId=" + memberId + "]";
  }

  private final String memberId;

  // currentTerm, for leader to update itself
  private final long term;
  
  // true if follower contained entry matching  prevLogIndex and prevLogTerm
  private final boolean success; 
  
  public HeartbeatResponse(String memberId, long term, boolean success) {
    this.memberId = memberId;
    this.term = term;
    this.success = success;
  }

  public LogicalTimestamp term() {
    return LogicalTimestamp.fromLong(term);
  }

  public boolean success() {
    return success;
  }
  
  public String memberId() {
    return memberId;
  }
}
