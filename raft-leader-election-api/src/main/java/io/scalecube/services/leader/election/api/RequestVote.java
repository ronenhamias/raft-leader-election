package io.scalecube.services.leader.election.api;

public class RequestVote {

  String candidateId;
  
  byte[] term;
  
  public String candidateId(){
    return candidateId;
  }
  
  public byte[] term(){
    return term;
  }
  
}
