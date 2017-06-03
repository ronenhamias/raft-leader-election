
package io.scalecube.services.leader.election;

import java.io.Serializable;

public class LogMetadata implements Serializable {

  private final long index;

  private long term;

  private long commitedIndex;

  public LogMetadata(long newIndex, long term, long commitedIndex) {
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
}
