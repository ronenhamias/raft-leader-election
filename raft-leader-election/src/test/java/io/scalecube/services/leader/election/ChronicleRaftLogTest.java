package io.scalecube.services.leader.election;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import io.scalecube.services.leader.election.api.LogEntry;
import io.scalecube.services.leader.election.api.RaftLog;

public class ChronicleRaftLogTest {

	@Test
	public void testLogIndex() throws IOException {
		RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build(); 
		long x = log.index();
		log.append(new LogEntry(1));
		
		assertTrue(log.index() == x+1);
	}
	
	@Test
	public void testNextTerm() throws IOException {
		RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build(); 
		long x = log.currentTerm().toLong();
		log.nextTerm();
		
		assertTrue(log.currentTerm().toLong() == x+1);
	}
	
	@Test
	public void testCommitedIndex() throws IOException {
		RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build(); 
		long x = log.commitedIndex();
		log.nextTerm();
		
		assertTrue(log.currentTerm().toLong() == x+1);
	}
	
	@Test
	public void testGetLogItem() throws IOException {
		RaftLog log = ChronicleRaftLog.builder().entries(1).averageValueSize(1500).build(); 
		LogEntry item1 = new LogEntry(1);
		log.append(item1);
		LogEntry item2 = log.getEntry(log.index());
		
		assertTrue(item1.term() == item2.term());
	}
	
	
	
}
