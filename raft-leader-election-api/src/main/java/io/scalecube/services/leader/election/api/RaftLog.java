package io.scalecube.services.leader.election.api;

import java.util.Optional;

public interface RaftLog {

  void append(LogEntry entry);

  void append(LogEntry[] entries);
  
  void append(byte[] data);

  Long index();

  LogicalTimestamp currentTerm();

  LogicalTimestamp nextTerm();

  void currentTerm(LogicalTimestamp term);

  LogEntry getEntry(Long index);

  long commitedIndex();

  void setMemberLog(String memberId, MemberLog memberLog) ;
  
  MemberLog getMemberLog(String memberId) ;

  Optional<LogEntry[]> replicateEntries(String memberId);

}
