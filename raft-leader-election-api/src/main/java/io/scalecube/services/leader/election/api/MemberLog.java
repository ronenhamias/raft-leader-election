package io.scalecube.services.leader.election.api;

import java.io.Serializable;

public class MemberLog {

  private final long index;

  private final long term;

  private final long commitedIndex;

  public MemberLog(long newIndex, long term, long commitedIndex) {
    this.index = newIndex;
    this.term = term;
    this.commitedIndex = commitedIndex;
  }

  public long term() {
    return term;
  }

  public long commitedIndex() {
    return commitedIndex;
  }

  public long index() {
    return index;
  }
  
  public static MemberLog withIndex(MemberLog meta, long newIndex) {
    return new MemberLog(newIndex, meta.term, meta.commitedIndex);
  }

  public static MemberLog empty() {
    return MemberLog.create(0,0,0);
  }

  public static MemberLog create(long newIndex,long  term,long  commitedIndex) {
    return new MemberLog(newIndex, term, commitedIndex);
  }

  public MemberLog withIndex(long newIndex) {
    return this.withIndex(this, newIndex);
  }
}
