package io.scalecube.services.leader.election;

import io.scalecube.services.leader.election.ChronicleRaftLog.Builder;

public class Config {


  private int heartbeatInterval = 1000;
  private int timeout = 3000;
  
  private final Builder builder;

  public Config(Builder builder) {
    this.builder = builder;
  }

  public int timeout() {
    return timeout;
  }

  public int heartbeatInterval() {
    return heartbeatInterval;
  }

  public Builder builder() {
    return this.builder;
  }
}
