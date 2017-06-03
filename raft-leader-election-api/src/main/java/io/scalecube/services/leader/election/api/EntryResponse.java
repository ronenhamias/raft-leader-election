package io.scalecube.services.leader.election.api;

public class EntryResponse {

  private AppendStatus status;
  
  private String leaderId;

  public EntryResponse(AppendStatus status, String leaderId) {
    this.status = status;
    this.leaderId = leaderId;
  }

}
