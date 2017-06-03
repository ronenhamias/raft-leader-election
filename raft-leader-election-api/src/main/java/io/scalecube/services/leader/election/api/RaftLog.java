package io.scalecube.services.leader.election.api;

public interface RaftLog {

  void append(LogEntry entry);

  Long index();

  LogicalTimestamp currentTerm();

  LogicalTimestamp nextTerm();

  void currentTerm(LogicalTimestamp term);

  LogEntry getEntry(Long index);

  long commitedIndex();

  void setMemberLog(String memberId, MemberLog memberLog) ;
  
  MemberLog getMemberLog(String memberId) ;
}
