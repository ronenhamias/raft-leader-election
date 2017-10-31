package io.scalecube.services.leader.election.api;


public interface RaftLog {

  Long index();
  
  LogicalTimestamp currentTerm();

  long commitedIndex();
  
  void append(LogEntry entry);

  void append(LogEntry[] entries);
  
  void append(byte[] data);

  void currentTerm(LogicalTimestamp term);

  LogEntry entry(long index);

  LogEntry[] entries(long fromIndex);

  long lastLogTerm();

  LogicalTimestamp nextTerm();
 
}

