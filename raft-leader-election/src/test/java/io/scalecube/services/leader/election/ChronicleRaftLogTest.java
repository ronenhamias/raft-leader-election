package io.scalecube.services.leader.election;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Optional;

import org.junit.Test;

import io.scalecube.services.leader.election.api.LogEntry;
import io.scalecube.services.leader.election.api.MemberLog;
import io.scalecube.services.leader.election.api.RaftLog;

public class ChronicleRaftLogTest {

  @Test
  public void testLogIndex() throws IOException {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    long x = log.index();
    log.append(new LogEntry(1,null));

    assertTrue(log.index() == x + 1);
  }

  @Test
  public void testNextTerm() throws IOException {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    long x = log.currentTerm().toLong();
    log.nextTerm();

    assertTrue(log.currentTerm().toLong() == x + 1);
  }

  @Test
  public void testCommitedIndex() throws IOException {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    long x = log.commitedIndex();
    log.nextTerm();

    assertTrue(log.currentTerm().toLong() == x + 1);
  }

  @Test
  public void testGetLogItem() throws IOException {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    LogEntry item1 = new LogEntry(1,null);
    log.append(item1);
    LogEntry item2 = log.getEntry(log.index());

    assertTrue(item1.term() == item2.term());
  }

  @Test
  public void testReplicateLog() throws IOException {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    LogEntry item1 = new LogEntry(1,null);
    log.setMemberLog("some-member-Id", new MemberLog(0L));
    
    log.append(item1);
    log.append(item1);
    log.append(item1);
    
    Optional<LogEntry[]> entries = log.replicateEntries("some-member-Id");

    assertTrue(entries.isPresent());
    assertTrue(entries.get().length == 3);
  }

  @Test
  public void testReplicateLogNoEntries() throws IOException {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    LogEntry item1 = new LogEntry(1,null);
    log.setMemberLog("some-member-Id", new MemberLog(0L));

    Optional<LogEntry[]> entries = log.replicateEntries("some-member-Id");

    assertTrue(entries.isPresent());
    assertTrue(entries.get().length == 0);
  }
  
  @Test
  public void testReplicateLogNoneMember() throws IOException {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    LogEntry item1 = new LogEntry(1,null);
    log.append(item1);
    log.append(item1);
    log.append(item1);
    
    Optional<LogEntry[]> entries = log.replicateEntries("some-member-Id");

    assertTrue(!entries.isPresent());
  }
  

}
