package io.scalecube.services.leader.election;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.scalecube.services.leader.election.api.LogEntry;
import io.scalecube.services.leader.election.api.MemberLog;
import io.scalecube.services.leader.election.api.RaftLog;

import com.sun.jna.platform.win32.Guid.GUID;
import com.thoughtworks.xstream.core.ReferenceByIdMarshaller.IDGenerator;

import org.junit.Test;

import java.util.Optional;

public class ChronicleRaftLogTest {

  @Test
  public void testLogIndex() throws Exception {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    long x = log.index();
    log.append(new LogEntry(1,null));

    assertTrue(log.index() == x + 1);
  }

  @Test
  public void testNextTerm() throws Exception {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    long x = log.currentTerm().toLong();
    log.nextTerm();

    assertTrue(log.currentTerm().toLong() == x + 1);
  }

  @Test
  public void testCommitedIndex() throws Exception {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    long x = log.commitedIndex();
    log.nextTerm();

    assertTrue(log.currentTerm().toLong() == x + 1);
  }

  @Test
  public void testGetLogItem() throws Exception {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    LogEntry item1 = new LogEntry(1,null);
    log.append(item1);
    LogEntry item2 = log.entry(log.index());

    assertTrue(item1.term() == item2.term());
  }

  @Test
  public void testReplicateLog() throws Exception {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    LogEntry item1 = new LogEntry(1,null);
    
    log.append(item1);
    log.append(item1);
    log.append(item1);
    
    LogEntry[] entries = log.entries(0);

    assertTrue(entries !=null);
    assertTrue(entries.length == 3);
  }

  @Test
  public void testReplicateLogNoEntries() throws Exception {
    RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build();
    LogEntry item1 = new LogEntry(1,null);
  }
  
  @Test
  public void testReplicateLogNoneMember() throws Exception {
    RaftLog log = ChronicleRaftLog.builder().averageValueSize(20000).entries(1000)
        .build();
    String memberid = GUID.newGuid().toString();
    
    
    LogEntry item1 = new LogEntry(1,"some data".getBytes());
    log.append(item1);
    log.append(item1);
    log.append(item1);
    
    Optional<LogEntry[]> entries ;//= log.entries(memberid);
    //assertTrue(entries.isPresent());
    //assertEquals(entries.get().length,3);
    
    
    //entries = log.replicateEntries(memberid);
    //assertTrue(entries.isPresent());
    //assertEquals(entries.get().length,1);
    
    
    //entries = log.replicateEntries(memberid);
   // assertTrue(entries.isPresent());
    //assertEquals(entries.get().length,0);
    
    
    
  }
  

}
