package io.scalecube.services.leader.election;

import io.scalecube.services.Microservices;

import org.junit.Test;

public class RaftLeaderElectionTest {

  @Test
  public void test() {
  
    Microservices seed = Microservices.builder().build();

    RaftLeaderElection leaderElection1 = new RaftLeaderElection(new Config());
    RaftLeaderElection leaderElection2 = new RaftLeaderElection(new Config());
    RaftLeaderElection leaderElection3 = new RaftLeaderElection(new Config());
    
    Microservices node1 = Microservices.builder().seeds(seed.cluster().address()).services(leaderElection1).build();
    Microservices node2 = Microservices.builder().seeds(seed.cluster().address()).services(leaderElection2).build();
    Microservices node3 = Microservices.builder().seeds(seed.cluster().address()).services(leaderElection3).build();
    
    leaderElection1.start(node1);
    leaderElection2.start(node2);
    leaderElection3.start(node3);
    
    System.out.println("DONE");
    
  }

}
