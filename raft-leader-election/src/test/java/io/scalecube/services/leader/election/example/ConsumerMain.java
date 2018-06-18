package io.scalecube.services.leader.election.example;

import io.scalecube.services.Microservices;
import io.scalecube.services.leader.election.api.LeaderElectionGossip;
import io.scalecube.services.leader.election.api.LeaderElectionService;
import io.scalecube.transport.Address;

public class ConsumerMain {

  public static void main(String[] args) {
    
    if (args.length < 2) {
      System.out.println("the application paramters must set with seed node address ");
    }
    
    Microservices node1 = Microservices.builder()
        .seeds(Address.create(args[0], Integer.parseInt(args[1])))
        .startAwait();

    LeaderElectionService proxy = node1.call().create().api(LeaderElectionService.class);
    
    node1.cluster().listenGossips()
      .filter(m->m.headers().containsKey(LeaderElectionGossip.TYPE))
      .subscribe(onNext->{
        LeaderElectionGossip gossip = onNext.data();
        System.out.println(gossip.term());
        System.out.println(gossip.memberId());
        System.out.println(gossip.leaderId());
        
        // asking all leader elections nodes who is the leader and print.
        node1.services().forEach(action->{
          proxy.leader().doAfterSuccessOrError((success,error)->{
            System.out.println(success);
          });
        });
      });
  }
}
