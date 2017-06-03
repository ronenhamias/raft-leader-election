package io.scalecube.services.leader.election.api;

import java.util.Arrays;

import io.scalecube.services.ServiceHeaders;
import io.scalecube.transport.Message;

public class HeartbeatRequest {

  @Override
  public String toString() {
    return "HeartbeatRequest [term=" + Arrays.toString(term) + ", memberId=" + memberId + "]";
  }

  public static class Builder{

    private byte[] currentTerm;
    
    private String memberId;

    private long logIndex;

    public Builder term(LogicalTimestamp currentTerm) {
      this.currentTerm = currentTerm.toBytes();
      return this;
    }

    public Builder memberId(String memberId) {
      this.memberId = memberId;
      return this;
    }

    public Builder nextIndex(long logIndex) {
      this.logIndex =logIndex;
      return this;
    }

    public Message build() {

      return Message.builder()
          .header(ServiceHeaders.SERVICE_REQUEST, LeaderElectionService.SERVICE_NAME)
          .header(ServiceHeaders.METHOD, "heartbeat")
          .data(new HeartbeatRequest(
              this.currentTerm,
              this.memberId,
              this.logIndex)).build();
    }
    
  } 
  
  private final byte[] term;
  private final String memberId;
  private final LogEntry[] entries;
  private final long nextIndex;
  
  private HeartbeatRequest(byte[] term, String memberId, long nextIndex, LogEntry[] entries) {
    this.term = term;
    this.memberId = memberId;
    this.entries = entries;
    this.nextIndex = nextIndex;
  }

  private HeartbeatRequest(byte[] term, String memberId,long nextIndex) {
    this(term,memberId,nextIndex,null);
  }
  
  public byte[] term() {
    return term;
  }

  public String memberId() {
    return memberId;
  }

  private LogEntry[] entries() {
    return entries;
  }

  public static Builder builder() {
      return new Builder();
  }

}
