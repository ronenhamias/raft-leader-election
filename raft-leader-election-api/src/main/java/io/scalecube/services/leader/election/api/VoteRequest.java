package io.scalecube.services.leader.election.api;

public class VoteRequest {

  @Override
  public String toString() {
    return "VoteRequest [term=" + term + ", candidateId=" + candidateId + ", lastLogIndex=" + lastLogIndex
        + ", lastLogTerm=" + lastLogTerm + "]";
  }

  private final long term;

  private final String candidateId;

  private long lastLogIndex;

  private long lastLogTerm;

  public VoteRequest(String candidateId, long term, long lastLogTerm, long lastLogIndex) {
    this.term = term;
    this.candidateId = candidateId;
    this.lastLogIndex = lastLogIndex;
    this.lastLogTerm = lastLogTerm;
  }

  public LogicalTimestamp term() {
    return LogicalTimestamp.fromLong(term);
  }


  public long lastLogIndex() {
    return lastLogIndex;
  }

  public LogicalTimestamp lastLogTerm() {
    return LogicalTimestamp.fromLong(lastLogTerm);
  }

  public String candidateId() {
    return candidateId;
  }

}
