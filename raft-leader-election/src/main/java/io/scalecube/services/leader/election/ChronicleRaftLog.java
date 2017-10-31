package io.scalecube.services.leader.election;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.scalecube.services.leader.election.api.LogEntry;
import io.scalecube.services.leader.election.api.LogicalClock;
import io.scalecube.services.leader.election.api.LogicalTimestamp;
import io.scalecube.services.leader.election.api.RaftLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

public class ChronicleRaftLog implements RaftLog {

  private LogicalClock clock = new LogicalClock();

  public final AtomicLong index = new AtomicLong();

  public final AtomicLong commitedIndex = new AtomicLong();

  private final AtomicReference<LogicalTimestamp> currentTerm = new AtomicReference<LogicalTimestamp>(clock.time());

  private ChronicleMap<Long, LogEntry> log;

  public static class Builder {

    private static final String THIS = "this";

    private final ChronicleMapBuilder<Long, LogEntry> log;

    private String memberId = THIS;

    private File directory = new File("./db");

    private Builder() {
      log = ChronicleMap.of(Long.class, LogEntry.class);
    }

    public Builder entries(int entries) {
      log.entries(entries);
      return this;
    }

    public Builder averageValueSize(double size) {
      checkArgument(size > 0);
      log.averageValueSize(size);
      return this;
    }

    public Builder memberId(String memberId) {
      checkNotNull(memberId);
      this.memberId = memberId;
      return this;
    }

    public Builder persistedTo(File directory) {
      checkNotNull(directory);
      checkArgument(directory.isDirectory());
      this.directory = directory;
      return this;
    }

    public ChronicleRaftLog build() throws Exception {
      if (directory != null) {
        return createPersistentStore(memberId);
      } else {
        throw new Exception("directory is not set");
      }

    }

    private ChronicleRaftLog createPersistentStore(String memberId) throws IOException {
      Path dir = directory.toPath();
      if (!directory.exists()) {
        dir.toFile().mkdirs();
      }
      File logFile = Paths.get(dir.toString(), "/" + memberId + ".log").toFile();

      return new ChronicleRaftLog(log.createPersistedTo(logFile));
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  private ChronicleRaftLog(ChronicleMap<Long, LogEntry> log) throws IOException {
    this.log = log;
  }

  @Override
  public Long index() {
    return index.get();
  }

  @Override
  public long commitedIndex() {
    return commitedIndex.get();
  }

  @Override
  public void append(LogEntry entry) {
    log.put(index.getAndIncrement(), entry);
  }

  @Override
  public void append(byte[] data) {
    log.put(index.incrementAndGet(), new LogEntry(currentTerm.get().toLong(), data));
  }

  @Override
  public void append(LogEntry[] entries) {
    for (int i = 0; i < entries.length; i++) {
      log.put(index.getAndIncrement(), entries[i]);
    }
  }

  @Override
  public LogicalTimestamp currentTerm() {
    return currentTerm.get();
  }

  @Override
  public LogicalTimestamp nextTerm() {
    currentTerm.set(clock.tick());
    return currentTerm.get();
  }

  @Override
  public void currentTerm(LogicalTimestamp term) {
    currentTerm.set(term);
  }

  @Override
  public LogEntry entry(long index) {
    return this.log.get(index);
  }
  
  @Override
  public LogEntry[] entries(long fromIndex) {
    if(fromIndex<this.index()){
      int batchSize = (int) (this.index() - fromIndex);
      LogEntry[] entries = new LogEntry[batchSize];
      if (batchSize > 0) {
        for (Integer x = 0, i = (int) fromIndex; i < this.index(); i++, x++) {
          entries[x] = this.log.get(i.longValue());
        }
      }
      return entries;
    } else {
      return new LogEntry[0];
    }
  }

  @Override
  public long lastLogTerm() {
    long term = 0;
    if(this.log.get(this.index())!=null){
      
    }
    return term;
  }

  
}
