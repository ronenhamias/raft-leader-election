package io.scalecube.services.leader.election;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Quorum<T> {

  ConcurrentMap<String, T> items;
  
  CountDownLatch latch;
  
  private Quorum(int size){
    latch = new CountDownLatch(size);
    items = new ConcurrentHashMap<>(size);
  }
  
  public void collect(String key ,T item){
    items.putIfAbsent(key, item);
    latch.countDown();
  }
  
  public ConcurrentMap<String, T> items(){
    return this.items;
  }
  
  public CompletableFuture<ConcurrentMap<String, T>> quorum(long timeout, TimeUnit unit){
    CompletableFuture<ConcurrentMap<String, T>> future = new CompletableFuture<>();
    CompletableFuture.runAsync(()->{
      try {
        latch.await(timeout, unit);
        if(latch.getCount() == 0) {
          future.complete(items);
        } else {
          future.completeExceptionally(new TimeoutException("cannot reach Quorum in: " + timeout + "-" + unit ));
        }
      } catch (InterruptedException e) {
        future.completeExceptionally(new TimeoutException("cannot reach Quorum in: " + timeout + "-" + unit ));
      }  
    });
    return future;
  }

  public static Quorum of(int consensus) {
    return new Quorum(consensus);
  }
}
