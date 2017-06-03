package io.scalecube.services.leader.election;

import io.scalecube.services.leader.election.api.RaftLog;

public class Config {


  private int heartbeatInterval = 300;
  private int timeout = 1000;
  private final RaftLog raftLog;

  public Config(RaftLog raftLog) {
    this.raftLog = raftLog;
  }

  public int timeout() {
    return timeout;
  }

  public int heartbeatInterval() {
    return heartbeatInterval;
  }

  public RaftLog raftLog() {
    return this.raftLog;
  }
}
