package io.scalecube.services.leader.election;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.scalecube.services.leader.election.api.LogEntry;
import io.scalecube.services.leader.election.api.LogicalClock;
import io.scalecube.services.leader.election.api.LogicalTimestamp;
import io.scalecube.services.leader.election.api.MemberLog;
import io.scalecube.services.leader.election.api.RaftLog;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ChronicleRaftLog implements RaftLog {

  private static final String SNAPSHOOT = "snapshoot";

  private LogicalClock clock = new LogicalClock();

  public final AtomicLong index = new AtomicLong();

  public final AtomicLong commitedIndex = new AtomicLong();

  private final AtomicReference<LogicalTimestamp> currentTerm = new AtomicReference<LogicalTimestamp>(clock.time());

  private ChronicleMap<Long, LogEntry> log;

  private ChronicleMap<String, Object> store;

  public static class Builder {

    public final ChronicleMapBuilder<Long, LogEntry> log;

    private ChronicleMapBuilder<String, Object> store;

    private File directory;

    private Builder() {
      log = ChronicleMap.of(Long.class, LogEntry.class).minSegments(512);
      store = ChronicleMap.of(String.class, Object.class).minSegments(512);
      store.entries(1).averageKeySize(14).averageValueSize(100);
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

    public Builder persistedTo(File directory) {
      checkNotNull(directory);
      checkArgument(directory.isDirectory());
      this.directory = directory;
      return this;
    }

    public ChronicleRaftLog build() throws IOException {
      if (directory != null) {
        return createPersistentStore();
      } else {
        return createOffHeap();
      }
    }

    /**
     * Creates a new hash container from this builder, storing it's data in off-heap memory, not mapped to any file. On
     * ChronicleHash.close() called on the returned container, or after the container object is collected during GC, or
     * on JVM shutdown the off-heap memory used by the returned container is freed.
     * 
     * @return ChronicleRaftLog
     * @throws IOException
     */
    private ChronicleRaftLog createOffHeap() throws IOException {
      return new ChronicleRaftLog(log.create(), store.create());
    }

    private ChronicleRaftLog createPersistentStore() throws IOException {
      Path dir = directory.toPath();
      if (!directory.exists()) {
        dir.toFile().mkdirs();
      }
      File logFile = Paths.get(dir.toString(), "/.log").toFile();
      File storeFile = Paths.get(dir.toString(), "/.index").toFile();

      return new ChronicleRaftLog(log.createPersistedTo(logFile), store.createPersistedTo(storeFile));
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  private ChronicleRaftLog(ChronicleMap<Long, LogEntry> log, ChronicleMap<String, Object> store)
      throws IOException {
    this.log = log;
    this.store = store;
    if (this.store.containsKey(SNAPSHOOT)) {
      readSnapshoot();
    }
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
    log.put(index.incrementAndGet(), entry);
    this.snapshoot();
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

  private void snapshoot() {
    store.put(SNAPSHOOT, new LogMetadata(index.get(), commitedIndex.get(), currentTerm.get().toLong()));
  }

  private void readSnapshoot() {
    LogMetadata metadata = (LogMetadata) this.store.get(SNAPSHOOT);
    this.currentTerm.set(LogicalTimestamp.fromLong(metadata.term()));
    this.clock = new LogicalClock(LogicalTimestamp.fromLong(metadata.term()));
    this.commitedIndex.set(metadata.commitedIndex());
    this.index.set(metadata.index());
  }

  @Override
  public LogEntry getEntry(Long index) {
    return log.get(index);
  }

  @Override
  public void setMemberLog(String memberId, MemberLog memberLog) {
    this.store.put(memberId, memberLog);
  }

  @Override
  public MemberLog getMemberLog(String memberId) {
    return (MemberLog) this.store.get(memberId);
  }
}
