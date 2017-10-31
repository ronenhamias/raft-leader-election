package io.scalecube.services.leader.election.api;

public class VoteResponse {

  @Override
  public String toString() {
    return "VoteResponse [granted=" + granted + ", memberId=" + memberId + "]";
  }

  private boolean granted;
  
  private String memberId;

  public VoteResponse(boolean granted, String memberId) {
    this.granted = granted;
    this.memberId = memberId;
  }

  public boolean granted() {
    return this.granted;
  }

  public String memberId() {
    return memberId;
  }

}
