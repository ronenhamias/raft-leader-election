package io.scalecube.services.leader.election;

import io.scalecube.services.Microservices;
import io.scalecube.services.leader.election.api.Leader;
import io.scalecube.services.leader.election.api.LeaderElectionService;
import io.scalecube.services.leader.election.state.State;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RaftLeaderElectionTest {

  @Test
  public void test() throws InterruptedException {
  
    Microservices seed = Microservices.builder().startAwait();

    RaftLeaderElection leaderElection1 = new RaftLeaderElection(new Config());
    RaftLeaderElection leaderElection2 = new RaftLeaderElection(new Config());
    RaftLeaderElection leaderElection3 = new RaftLeaderElection(new Config());
    
    Microservices node1 = Microservices.builder().seeds(seed.cluster().address()).services(leaderElection1).startAwait();
    Microservices node2 = Microservices.builder().seeds(seed.cluster().address()).services(leaderElection2).startAwait();
    Microservices node3 = Microservices.builder().seeds(seed.cluster().address()).services(leaderElection3).startAwait();
    
    leaderElection1.start(node1);
    leaderElection2.start(node2);
    leaderElection3.start(node3);
    
    leaderElection1.on(State.LEADER, onLeader());
    leaderElection1.on(State.FOLLOWER, onFollower());
    
    leaderElection2.on(State.LEADER, onLeader());
    leaderElection2.on(State.FOLLOWER, onFollower());
    
    leaderElection3.on(State.LEADER, onLeader());
    leaderElection3.on(State.FOLLOWER, onFollower());
   
    // wait for leader to be elected.
    Thread.sleep(7000);
    
    
    LeaderElectionService proxy = seed.call().create().api(LeaderElectionService.class);
    
    CountDownLatch latch = new CountDownLatch(6);
    
    List<Leader> leaders = new ArrayList();
    for(int i =0 ; i < 6 ; i ++ ){
      proxy.leader().doAfterSuccessOrError((success,error)->{
        latch.countDown();
        leaders.add(success);
      });
    }
    
    latch.await(3, TimeUnit.SECONDS);
    String selected =leaders.get(0).leaderId();
    leaders.stream().forEach(leader->{
      System.out.println(leader);
      Assert.assertEquals(selected, leader.leaderId());
    });
    
    
    System.out.println("DONE");
    
  }

  private Consumer onFollower() {
    return leader->{
      System.out.println("on state onFollower ");
    };
  }

  private Consumer onLeader() {
    return leader->{
      System.out.println("on state leader ");
    };
  }

}
