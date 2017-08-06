package io.scalecube.services.leader.election;

import io.scalecube.services.leader.election.ChronicleRaftLog.Builder;

public class Config {

  private int heartbeatInterval = 300;
  private int timeout = 1000;
  private final Builder builder;
  private long electionTimeout = 3000;

  public Config(Builder builder) {
    this.builder = builder;
  }

  public int timeout() {
    return timeout;
  }
  
  public Config timeout(int timeout) {
    this.timeout = timeout;
    return this;
  }

  public int heartbeatInterval() {
    return heartbeatInterval;
  }

  public Config heartbeatInterval(int heartbeatInterval ) {
    this.heartbeatInterval= heartbeatInterval;
    return this;
  }
  
  public long electionTimeout() {
    return electionTimeout ;
  }
  public Config electionTimeout(long electionTimeout) {
    this.electionTimeout = electionTimeout;
    return this;
  }
  
 

  public Builder builder() {
    return this.builder;
  }

 
}
