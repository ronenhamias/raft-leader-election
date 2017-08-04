package io.scalecube.services.leader.election.api;

public class AppendEntriesResponse {


  @Override
  public String toString() {
    return "AppendEntriesResponse [term=" + term + ", memberId=" + memberId + "]";
  }

  private final String memberId;

  // currentTerm, for leader to update itself
  private final long term;

  // true if follower contained entry matching prevLogIndex and prevLogTerm
  private final boolean success;

  private final long lastLogIndex;

  public AppendEntriesResponse(String memberId, long term, long lastLogIndex, boolean success) {
    this.memberId = memberId;
    this.term = term;
    this.success = success;
    this.lastLogIndex = lastLogIndex;
  }

  public LogicalTimestamp term() {
    return LogicalTimestamp.fromLong(term);
  }

  public long lastLogIndex() {
    return this.lastLogIndex;
  }

  public boolean success() {
    return success;
  }

  public String memberId() {
    return memberId;
  }
}
