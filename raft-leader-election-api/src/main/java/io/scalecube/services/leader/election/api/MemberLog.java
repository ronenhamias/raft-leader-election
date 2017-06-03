package io.scalecube.services.leader.election.api;

import java.io.Serializable;

public class MemberLog implements Serializable{

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
