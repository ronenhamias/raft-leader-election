package io.scalecube.services.leader.election.api;

public class MemberLog {

  public final long logIndex;
  
  public MemberLog() {
    this.logIndex = 0;
  }
  
  public MemberLog(long logIndex) {
    this.logIndex = logIndex;
  }

  public long logIndex() {
    return logIndex;
  }

}
