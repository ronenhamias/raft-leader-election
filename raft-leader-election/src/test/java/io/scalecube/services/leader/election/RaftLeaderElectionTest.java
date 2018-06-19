package io.scalecube.services.leader.election;

import io.scalecube.services.Microservices;
import io.scalecube.services.Reflect;
import io.scalecube.services.leader.election.example.GreetingServiceImpl;
import io.scalecube.services.leader.election.state.State;

import org.junit.Test;

import java.util.function.Consumer;

public class RaftLeaderElectionTest {

  @Test
  public void test() throws InterruptedException {

    Microservices seed = Microservices.builder().startAwait();

    GreetingServiceImpl leaderElection1 = new GreetingServiceImpl(new Config());
    GreetingServiceImpl leaderElection2 = new GreetingServiceImpl(new Config());
    GreetingServiceImpl leaderElection3 = new GreetingServiceImpl(new Config());

    Microservices node1 =
        Microservices.builder().seeds(seed.cluster().address()).services(leaderElection1).startAwait();
    Microservices node2 =
        Microservices.builder().seeds(seed.cluster().address()).services(leaderElection2).startAwait();
    Microservices node3 =
        Microservices.builder().seeds(seed.cluster().address()).services(leaderElection3).startAwait();

    leaderElection1.on(State.LEADER, onLeader());
    leaderElection1.on(State.FOLLOWER, onFollower());

    leaderElection2.on(State.LEADER, onLeader());
    leaderElection2.on(State.FOLLOWER, onFollower());

    leaderElection3.on(State.LEADER, onLeader());
    leaderElection3.on(State.FOLLOWER, onFollower());

    // wait for leader to be elected.
    Thread.sleep(7000);

    System.out.println("leaderElection1 leader:" + leaderElection1.leaderId());
    System.out.println("leaderElection2 leader:" + leaderElection2.leaderId());
    System.out.println("leaderElection3 leader:" + leaderElection3.leaderId());

    System.out.println("DONE");
    Thread.currentThread().join();
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
