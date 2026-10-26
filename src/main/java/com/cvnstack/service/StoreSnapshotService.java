package com.cvnstack.service;

import com.cvnstack.model.response.StoreSnapshotResponse;
import com.cvnstack.model.response.StoreSnapshotResponse.EntrySnapshot;
import com.cvnstack.model.response.StoreSnapshotResponse.RedisSnapshot;
import com.cvnstack.model.response.StoreSnapshotResponse.WaitlistSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Reads the live PostgreSQL tables and Redis keys for the visualizer.
 *
 * <p>AI-assisted.
 */
@Service
public class StoreSnapshotService {

  private final JdbcTemplate jdbc;
  private final StringRedisTemplate redis;

  /**
   * Creates the snapshot reader. Either store may be absent in mocked tests.
   *
   * @param jdbc PostgreSQL access, if a datasource is configured
   * @param redis Redis access, if a Redis connection is configured
   */
  public StoreSnapshotService(
      ObjectProvider<JdbcTemplate> jdbc, ObjectProvider<StringRedisTemplate> redis) {
    this.jdbc = jdbc.getIfAvailable();
    this.redis = redis.getIfAvailable();
  }

  /**
   * Returns every waitlist row, entry row, and CVNStack Redis key.
   *
   * @return store snapshot
   */
  public StoreSnapshotResponse snapshot() {
    if (jdbc == null) {
      return new StoreSnapshotResponse(List.of(), List.of(), List.of());
    }
    return new StoreSnapshotResponse(waitlists(), entries(), redisKeys());
  }

  /**
   * Clears the Redis database used by the running application.
   */
  public void flushRedis() {
    if (redis == null) {
      return;
    }
    try (RedisConnection connection =
        Objects.requireNonNull(redis.getConnectionFactory()).getConnection()) {
      connection.serverCommands().flushDb();
    }
  }

  /**
   * Deletes every waitlist and entry row, then empties Redis.
   */
  public void wipeStores() {
    if (jdbc != null) {
      jdbc.execute("TRUNCATE TABLE entries, waitlists");
    }
    flushRedis();
  }

  private List<WaitlistSnapshot> waitlists() {
    return jdbc.query("""
        SELECT id, name, max_length, processing_time_per_entry_minutes
        FROM waitlists ORDER BY name
        """, (row, index) -> new WaitlistSnapshot(
        row.getObject("id", UUID.class).toString(), row.getString("name"),
        row.getInt("max_length"), row.getInt("processing_time_per_entry_minutes")));
  }

  private List<EntrySnapshot> entries() {
    return jdbc.query("""
        SELECT id, waitlist_id, name, priority, status, inserted_at, called_at, updated_at
        FROM entries ORDER BY inserted_at
        """, (row, index) -> new EntrySnapshot(
        row.getObject("id", UUID.class).toString(),
        row.getObject("waitlist_id", UUID.class).toString(), row.getString("name"),
        row.getInt("priority"), row.getString("status"),
        String.valueOf(row.getTimestamp("inserted_at").toInstant()),
        row.getTimestamp("called_at") == null
            ? null : String.valueOf(row.getTimestamp("called_at").toInstant()),
        String.valueOf(row.getTimestamp("updated_at").toInstant())));
  }

  private List<RedisSnapshot> redisKeys() {
    if (redis == null) {
      return List.of();
    }
    Set<String> keys = redis.keys("cvnstack:*");
    if (keys == null || keys.isEmpty()) {
      return List.of();
    }
    List<RedisSnapshot> views = new ArrayList<>();
    for (String key : keys.stream().sorted().toList()) {
      DataType type = redis.type(key);
      String typeName = type == null ? "none" : type.code();
      views.add(new RedisSnapshot(key, typeName, printableValue(key, type)));
    }
    return views;
  }

  private String printableValue(String key, DataType type) {
    if (type == DataType.ZSET) {
      return String.valueOf(redis.opsForZSet().range(key, 0, -1));
    }
    if (type == DataType.HASH) {
      return String.valueOf(redis.opsForHash().entries(key));
    }
    if (type == DataType.SET) {
      return String.valueOf(redis.opsForSet().members(key));
    }
    if (type == DataType.STRING) {
      return String.valueOf(redis.opsForValue().get(key));
    }
    return "";
  }
}
