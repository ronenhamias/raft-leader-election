package io.scalecube.services.leader.election.api;

import java.io.Serializable;

public class LogEntry implements Serializable {

  private final long term;
  private final byte[] data;
  


  public LogEntry(long term, byte[] data) {
    this.term = term;
    this.data = data;
  }

  public long term() {
    return term;
  }
}
