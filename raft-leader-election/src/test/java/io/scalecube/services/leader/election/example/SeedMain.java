package io.scalecube.services.leader.election.example;

import io.scalecube.services.Microservices;

public class SeedMain {

  public static void main(String[] args) throws InterruptedException {
    Microservices seed = Microservices.builder().startAwait();

    System.out.println(seed.cluster().address());
    
    Thread.currentThread().join();
  }

}
