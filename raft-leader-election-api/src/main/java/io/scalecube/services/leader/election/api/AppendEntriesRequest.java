package io.scalecube.services.leader.election.api;

import io.scalecube.services.ServiceHeaders;
import io.scalecube.transport.Message;

public class AppendEntriesRequest {

  public static class Builder{

    private String leaderId;
    
    private Long term;
    
    private Long prevLogIndex;
    
    private long prevLogTerm;

    private long commitIndex;
    
    private LogEntry[] entries;

    public Builder term(LogicalTimestamp currentTerm) {
      this.prevLogIndex = currentTerm.toLong();
      return this;
    }

    public Builder leaderId(String leaderId) {
      this.leaderId = leaderId;
      return this;
    }

    public Builder leaderTerm(long leaderTerm){
      this.term = leaderTerm;
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
      this.commitIndex =leaderCommit;
      return this;
    }
    
    public Builder entries(LogEntry[] entries) {
      this.entries = entries;
      return this;
    }
    
    public Message build() {
      
      return Message.builder()
          .header(ServiceHeaders.SERVICE_REQUEST, "io.scalecube.services.leader.election.api.LeaderElectionService")
          .header(ServiceHeaders.METHOD, "heartbeat")
          .data(new AppendEntriesRequest(
              this.leaderId,
              this.term,
              this.prevLogTerm,
              this.prevLogIndex,
              this.commitIndex,
              this.entries
              )).build();
    }  
  } 
  
  private String leaderId;
  
  private Long term;
  
  private Long prevLogIndex;
  
  private long prevLogTerm;

  private long commitIndex;
  
  private LogEntry[] entries;
  
  
  
  private AppendEntriesRequest(String leaderId,long term,long prevLogTerm, long prevLogIndex,long commitIndex, LogEntry[] entries) {
    this.leaderId = leaderId;
    this.term = term;
    this.prevLogIndex = prevLogIndex;
    this.prevLogTerm = prevLogTerm;
    this.commitIndex = commitIndex;
    this.entries = entries;  
  }

  private AppendEntriesRequest(String memberId,long leaderTerm,long lastLogTerm, long lastLogIndex,long leaderCommit) {
    this(memberId,leaderTerm,lastLogTerm,lastLogIndex,leaderCommit,null);
  }
  
  public String leaderId() {
    return leaderId;
  }

  public LogEntry[] entries() {
    return entries;
  }

  public static Builder builder() {
      return new Builder();
  }

  public LogicalTimestamp term() {
    return LogicalTimestamp.fromLong(this.term) ;
  }
  
  public Long prevLogIndex() {
    return this.prevLogIndex;
  }
  
  public long commitIndex (){
    return this.commitIndex;
  }
  
  public LogicalTimestamp prevLogTerm(){
    return LogicalTimestamp.fromLong(this.prevLogTerm);
  }

}
