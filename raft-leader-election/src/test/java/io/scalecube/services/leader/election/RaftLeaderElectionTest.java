package io.scalecube.services.leader.election;

import io.scalecube.services.Microservices;
import io.scalecube.services.leader.election.api.Leader;
import io.scalecube.services.leader.election.api.LeaderElectionService;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RaftLeaderElectionTest {

  @Test
  public void test() throws InterruptedException {
  
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
    
    // wait for leader to be elected.
    Thread.sleep(7000);
    
    
    LeaderElectionService proxy = seed.proxy().api(LeaderElectionService.class).create();
    
    CountDownLatch latch = new CountDownLatch(3);
    
    List<Leader> leaders = new ArrayList();
    for(int i =0 ; i < 6 ; i ++ ){
      proxy.leader().whenComplete((success,error)->{
        latch.countDown();
        leaders.add(success);
      });
    }
    
    latch.await(3, TimeUnit.SECONDS);
    
    System.out.println(leaders);
    
    System.out.println("DONE");
    
  }

}
