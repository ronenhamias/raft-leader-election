package io.scalecube.services.leader.election;

import io.scalecube.services.Microservices;
import io.scalecube.services.leader.election.api.Leader;
import io.scalecube.services.leader.election.api.LeaderElectionService;
import io.scalecube.services.leader.election.state.State;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RaftLeaderElectionTest {

  @Test
  public void test() throws InterruptedException, Exception {

    Microservices seed = Microservices.builder().build();

    RaftLeaderElection leaderElection1 = new RaftLeaderElection(new Config(
        ChronicleRaftLog.builder()
            .entries(10)
            .averageValueSize(2)
            .persistedTo(new File("./target/node1/"))
            ));

    RaftLeaderElection leaderElection2 = new RaftLeaderElection(new Config(
        ChronicleRaftLog.builder()
            .entries(10)
            .averageValueSize(2)
            .persistedTo(new File("./target/node2/"))
            ));

    RaftLeaderElection leaderElection3 = new RaftLeaderElection(new Config(
        ChronicleRaftLog.builder()
            .entries(10)
            .averageValueSize(2)
            .persistedTo(new File("./target/node3/"))
            ));

    Microservices node1 = Microservices.builder().seeds(seed.cluster().address()).services(leaderElection1).build();
    Microservices node2 = Microservices.builder().seeds(seed.cluster().address()).services(leaderElection2).build();
    Microservices node3 = Microservices.builder().seeds(seed.cluster().address()).services(leaderElection3).build();

    leaderElection1.start();
    leaderElection2.start();
    leaderElection3.start();

    leaderElection1.on(State.LEADER, onLeader());
    leaderElection1.on(State.FOLLOWER, onFollower());

    leaderElection2.on(State.LEADER, onLeader());
    leaderElection2.on(State.FOLLOWER, onFollower());

    leaderElection3.on(State.LEADER, onLeader());
    leaderElection3.on(State.FOLLOWER, onFollower());

    // wait for leader to be elected.
    Thread.sleep(7000);


    LeaderElectionService proxy = seed.proxy().api(LeaderElectionService.class).create();

    CountDownLatch latch = new CountDownLatch(6);

    List<Leader> leaders = new ArrayList();
    for (int i = 0; i < 6; i++) {
      proxy.leader().whenComplete((success, error) -> {
        latch.countDown();
        leaders.add(success);
      });
    }

    latch.await(3, TimeUnit.SECONDS);
    String selected = leaders.get(0).leaderId();
    leaders.stream().forEach(leader -> {
      System.out.println(leader);
      Assert.assertEquals(selected, leader.leaderId());
    });


    System.out.println("DONE");

  }

  private Consumer onFollower() {
    return leader -> {
      System.out.println("on state onFollower ");
    };
  }

  private Consumer onLeader() {
    return leader -> {
      System.out.println("on state leader ");
    };
  }

}
