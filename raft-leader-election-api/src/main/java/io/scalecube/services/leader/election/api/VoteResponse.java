package io.scalecube.services.leader.election.api;

public class VoteResponse {

  private boolean granted;

  public VoteResponse(boolean granted) {
    this.granted = granted;
  }

  public boolean granted() {
    return this.granted;
  }

}
