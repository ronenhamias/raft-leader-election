package io.scalecube.services.leader.election.example;

import io.scalecube.services.Microservices;
import io.scalecube.services.leader.election.Config;
import io.scalecube.services.leader.election.RaftLeaderElection;
import io.scalecube.transport.Address;

public class LeaderElectionMain {

  public static void main(String[] args) {
    
    RaftLeaderElection leaderElection1 = new RaftLeaderElection(new Config());

    Microservices node1 = Microservices.builder()
         .seeds(Address.create(args[0], Integer.parseInt(args[1])))
         .services(leaderElection1).build();
    
    leaderElection1.start(node1);
  }

}
