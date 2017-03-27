package io.scalecube.services.leader.election;

import org.junit.Test;

public class HeartbeatSchedulerTest {

  @Test
  public void testHeartbeatScheduler() {
   JobScheduler scheduler = new JobScheduler(a->{
     System.out.println("executed");
   });
   
   
   scheduler.start(3000);
   scheduler.reset(3000);
   System.out.println("STARTED");
   
   scheduler.stop();
   System.out.println("STOPPED");
  }

  
}
