package io.scalecube.services.leader.election.api;

import java.util.Optional;

public interface RaftLog {

  Long index();
  
  LogicalTimestamp currentTerm();

  long commitedIndex();
  
  long getLastLogTerm();
 
  LogicalTimestamp nextTerm();
  

  void append(LogEntry entry);

  void append(LogEntry[] entries);
  
  void append(byte[] data);

  void currentTerm(LogicalTimestamp term);

  LogEntry getEntry(Long index);
  
  void setMemberLog(String memberId, MemberLog memberLog) ;
  
  MemberLog getMemberLog(String memberId) ;

  Optional<LogEntry[]> replicateEntries(String memberId);

  void decrementIndex(String memberId);

}

