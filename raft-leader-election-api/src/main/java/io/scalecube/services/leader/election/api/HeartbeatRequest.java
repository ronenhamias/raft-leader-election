package io.scalecube.services.leader.election.api;

import io.scalecube.services.ServiceHeaders;
import io.scalecube.transport.Message;

public class HeartbeatRequest {

  public static class Builder{

    private String memberId;
    
    private Long leaderTerm;
    
    private Long prevLogIndex;
    
    private long prevLogTerm;

    private long leaderCommit;
    
    private LogEntry[] entries;

    public Builder term(LogicalTimestamp currentTerm) {
      this.prevLogIndex = currentTerm.toLong();
      return this;
    }

    public Builder memberId(String memberId) {
      this.memberId = memberId;
      return this;
    }

    public Builder prevLogIndex(long prevLogIndex) {
      this.prevLogIndex =prevLogIndex;
      return this;
    }

    public Builder prevLogTerm(long prevLogTerm) {
      this.prevLogTerm =prevLogTerm;
      return this;
    }

    public Builder leaderCommit(long leaderCommit) {
      this.leaderCommit =leaderCommit;
      return this;
    }
    
    public Builder entries(LogEntry[] entries) {
      this.entries = entries;
      return this;
    }
    
    public Message build() {

      return Message.builder()
          .header(ServiceHeaders.SERVICE_REQUEST, LeaderElectionService.SERVICE_NAME)
          .header(ServiceHeaders.METHOD, "heartbeat")
          .data(new HeartbeatRequest(
              this.memberId,
              this.leaderTerm,
              this.prevLogTerm,
              this.prevLogIndex,
              this.leaderCommit,
              this.entries
              )).build();
    }  
  } 
  
  private String memberId;
  
  private Long leaderTerm;
  
  private Long prevLogIndex;
  
  private long prevLogTerm;

  private long leaderCommit;
  
  private LogEntry[] entries;
  
  private HeartbeatRequest(String memberId,long leaderTerm,long lastLogTerm, long lastLogIndex,long leaderCommit, LogEntry[] entries) {
    this.memberId = memberId;
    this.leaderTerm = leaderTerm;
    this.prevLogIndex = lastLogIndex;
    this.prevLogTerm = lastLogTerm;
    this.leaderCommit = leaderCommit;
    this.entries = entries;
    
  }

  private HeartbeatRequest(String memberId,long leaderTerm,long lastLogTerm, long lastLogIndex,long leaderCommit) {
    this(memberId,leaderTerm,lastLogTerm,lastLogIndex,leaderCommit,null);
  }
  
  public String memberId() {
    return memberId;
  }

  public LogEntry[] entries() {
    return entries;
  }

  public static Builder builder() {
      return new Builder();
  }

  public Long prevLogIndex() {
    return this.prevLogIndex;
  }
  
  public LogicalTimestamp prevLogTerm(){
    return LogicalTimestamp.fromLong(this.prevLogTerm);
  }

}
