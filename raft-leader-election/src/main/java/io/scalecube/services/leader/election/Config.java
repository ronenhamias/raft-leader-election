package io.scalecube.services.leader.election;

public class Config {

  
  private int heartbeatInterval = 300;
  private int timeout = 1000;
  
  public Config() {
  }

  public int timeout() {
    return timeout;
  }

  public int heartbeatInterval() {
    return heartbeatInterval;
  }
}
