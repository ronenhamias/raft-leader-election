package io.scalecube.services.leader.election;

import io.scalecube.services.leader.election.state.State;
import io.scalecube.services.leader.election.state.StateMachine;

import org.junit.Test;

public class StateMachineTest {

  @Test
  public void test() {

    
    StateMachine sm = StateMachine.builder()
        .init(State.INACTIVE)
        .addTransition(State.INACTIVE, State.CANDIDATE)
        .addTransition(State.CANDIDATE, State.FOLLOWER)
        .addTransition(State.CANDIDATE, State.LEADER)
        .addTransition(State.LEADER, State.INACTIVE)
        .addTransition(State.INACTIVE, State.FOLLOWER)
        .addTransition(State.FOLLOWER, State.CANDIDATE)
        .build();
    
    sm.on(State.CANDIDATE, (func)->{
      System.out.println(func + " 1");
    });
    
    sm.on(State.CANDIDATE, func->{
      System.out.println(func + " 2");
    });

    sm.beforeExit(State.CANDIDATE, func->{
      System.out.println(func + " leave 2");
    });
    
    sm.beforeExit(State.INACTIVE, func->{
      System.out.println(func + " leave 1");
    });
    
    sm.transition(State.CANDIDATE, "to CANDIDATE");
    sm.transition(State.LEADER, "to LEADER");
    sm.transition(State.INACTIVE, "to INACTIVE");
    sm.transition(State.FOLLOWER, "to CANDIDATE");
    sm.transition(State.CANDIDATE, "to CANDIDATE");
    
   
    System.out.println("DONE");
  }

 

}
