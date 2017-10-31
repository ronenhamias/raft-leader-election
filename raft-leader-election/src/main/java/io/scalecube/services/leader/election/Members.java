package io.scalecube.services.leader.election;

import io.scalecube.services.leader.election.api.MemberLog;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Members {

  private final ConcurrentMap<String, MemberLog> members;

  public static Members create() {
    return new Members();
  }

  private Members() {
    members = new ConcurrentHashMap<>();
  }

  public boolean contains(String memberId) {
    return members.containsKey(memberId);
  }

  public void set(String memberId, MemberLog memberLog) {
    members.put(memberId, memberLog);
  }

  public MemberLog get(String memberId) {
    return members.get(memberId);
  }

  public MemberLog updateIndex(String memberId, long lastLogIndex) {
    MemberLog item = members.get(memberId);
    if (item != null) {
      item = item.withIndex(lastLogIndex);
    } else {
      item = MemberLog.empty().withIndex(lastLogIndex);
    }
    members.put(memberId, item);
    return item;
  }


}
