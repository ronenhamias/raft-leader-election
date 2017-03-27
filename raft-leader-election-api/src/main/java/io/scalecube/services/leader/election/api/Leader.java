package io.scalecube.services.leader.election.api;

public class Leader {

  

  @Override
  public String toString() {
    return "Leader [memberId=" + memberId + ", leaderId=" + leaderId + "]";
  }

  private final String memberId;
  private final String leaderId;

  public Leader(String memberId, String leaderId) {
    this.memberId = memberId;
    this.leaderId = leaderId;
  }

  public String memberId(){
    return memberId;
  }
  
  public String leaderId(){
    return leaderId;
  }
}
