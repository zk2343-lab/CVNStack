package com.cvnstack.service;

import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Stores waiting order, entry payloads, and serve-next locks in Redis.
 *
 * <p>AI-assisted.
 */
@Service
public class RedisCacheStorage implements CacheStorage {

  // Expire abandoned serve-next locks rather than leaving a waitlist locked.
  private static final Duration SERVE_LOCK_TTL = Duration.ofSeconds(30);

  // The caller supplies priority-descending, insertion-ascending ids from WaitingOrder.
  // Redis score = zero-based list index, so ascending scores preserve that exact order.
  private static final RedisScript<Long> REPLACE_ORDER = new DefaultRedisScript<>(
      """
      redis.call('DEL', KEYS[1])
      for index, entryId in ipairs(ARGV) do
        redis.call('ZADD', KEYS[1], index - 1, entryId)
      end
      redis.call('SET', KEYS[2], #ARGV)
      return 1
      """, Long.class);

  private static final RedisScript<List> READ_ORDER = new DefaultRedisScript<>(
      """
      local count = redis.call('GET', KEYS[2])
      if not count or tonumber(count) ~= redis.call('ZCARD', KEYS[1]) then
        return {}
      end
      local ids = redis.call('ZRANGE', KEYS[1], 0, -1)
      table.insert(ids, 1, 'cached')
      return ids
      """, List.class);

  private static final RedisScript<Long> PUT_ENTRY = new DefaultRedisScript<>(
      """
      redis.call('HSET', KEYS[1],
        'id', ARGV[1], 'waitlistId', ARGV[2], 'name', ARGV[3],
        'priority', ARGV[4], 'status', ARGV[5], 'insertedAt', ARGV[6],
        'calledAt', ARGV[7], 'updatedAt', ARGV[8])
      redis.call('SADD', KEYS[2], KEYS[1])
      return 1
      """, Long.class);

  private static final RedisScript<Long> DELETE_ENTRY = new DefaultRedisScript<>(
      """
      redis.call('DEL', KEYS[1])
      redis.call('SREM', KEYS[2], KEYS[1])
      return 1
      """, Long.class);

  private static final RedisScript<Long> INVALIDATE_WAITLIST = new DefaultRedisScript<>(
      """
      for _, entryKey in ipairs(redis.call('SMEMBERS', KEYS[3])) do
        redis.call('DEL', entryKey)
      end
      redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
      return 1
      """, Long.class);

  private static final RedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
      """
      if redis.call('GET', KEYS[1]) == ARGV[1] then
        return redis.call('DEL', KEYS[1])
      end
      return 0
      """, Long.class);

  private final StringRedisTemplate redis;
  // Each request thread keeps the tokens for the locks it acquired.
  private final ThreadLocal<Map<String, String>> lockTokens = new ThreadLocal<>();

  /**
   * Creates the Redis adapter using the configured connection.
   *
   * @param redis Redis string operations
   */
  public RedisCacheStorage(StringRedisTemplate redis) {
    this.redis = Objects.requireNonNull(redis, "redis");
  }

  @Override
  public void replaceWaitingOrder(String waitlistId, List<String> entryIdsInOrder) {
    List<String> ids = List.copyOf(entryIdsInOrder);
    if (new HashSet<>(ids).size() != ids.size()) {
      throw new IllegalArgumentException("Waiting entry ids must be unique");
    }

    String keyPrefix = prefix(waitlistId);
    List<String> keys = List.of(keyPrefix + "waiting", keyPrefix + "waiting-ready");
    redis.execute(REPLACE_ORDER, keys, ids.toArray());
  }

  @Override
  public Optional<List<String>> findWaitingOrder(String waitlistId) {
    String keyPrefix = prefix(waitlistId);
    List<String> keys = List.of(keyPrefix + "waiting", keyPrefix + "waiting-ready");
    List<?> values = redis.execute(READ_ORDER, keys);
    if (values == null || values.isEmpty()) {
      return Optional.empty();
    }
    // The first script result marks a cache hit, including a cached empty queue.
    List<String> ids = new ArrayList<>(values.size() - 1);
    for (Object id : values.subList(1, values.size())) {
      ids.add((String) id);
    }
    return Optional.of(ids);
  }

  @Override
  public void putEntry(Entry entry) {
    String waitlistId = entry.waitlistId();
    String key = entryKey(waitlistId, entry.id());
    String indexKey = prefix(waitlistId) + "cached-entries";
    List<String> keys = List.of(key, indexKey);

    redis.execute(PUT_ENTRY, keys,
        entry.id(), waitlistId, entry.name(), Integer.toString(entry.priority()),
        entry.status().name(), entry.insertedAt().toString(),
        entry.calledAt() == null ? "" : entry.calledAt().toString(),
        entry.updatedAt() == null ? "" : entry.updatedAt().toString());
  }

  @Override
  public Optional<Entry> findEntry(String waitlistId, String entryId) {
    String key = entryKey(waitlistId, entryId);
    Map<Object, Object> fields = redis.opsForHash().entries(key);
    if (fields.isEmpty()) {
      return Optional.empty();
    }
    String calledAt = (String) fields.get("calledAt");
    String updatedAt = (String) fields.get("updatedAt");
    return Optional.of(new Entry(
        (String) fields.get("id"),
        (String) fields.get("waitlistId"),
        (String) fields.get("name"),
        Integer.parseInt((String) fields.get("priority")),
        EntryStatus.valueOf((String) fields.get("status")),
        Instant.parse((String) fields.get("insertedAt")),
        calledAt == null || calledAt.isEmpty() ? null : Instant.parse(calledAt),
        updatedAt == null || updatedAt.isEmpty() ? null : Instant.parse(updatedAt)));
  }

  @Override
  public void invalidateWaitlist(String waitlistId) {
    String keyPrefix = prefix(waitlistId);
    List<String> keys = List.of(
        keyPrefix + "waiting", keyPrefix + "waiting-ready", keyPrefix + "cached-entries");
    redis.execute(INVALIDATE_WAITLIST, keys);
  }

  @Override
  public void deleteEntry(String waitlistId, String entryId) {
    String key = entryKey(waitlistId, entryId);
    String indexKey = prefix(waitlistId) + "cached-entries";
    List<String> keys = List.of(key, indexKey);
    redis.execute(DELETE_ENTRY, keys);
  }

  /**
   * Acquires a non-reentrant lock for this thread, if it is not already held.
   *
   * @param waitlistId waitlist to lock
   * @return true when this thread acquired the lock
   */
  @Override
  public boolean tryAcquireServeLock(String waitlistId) {
    String key = prefix(waitlistId) + "serve-lock";
    String token = UUID.randomUUID().toString();
    Boolean acquired = redis.opsForValue().setIfAbsent(key, token, SERVE_LOCK_TTL);
    if (!Boolean.TRUE.equals(acquired)) {
      return false;
    }

    Map<String, String> tokens = lockTokens.get();
    if (tokens == null) {
      tokens = new HashMap<>();
      lockTokens.set(tokens);
    }
    tokens.put(waitlistId, token);
    return true;
  }

  /**
   * Releases only the lock acquired by this thread and adapter instance.
   *
   * <p>Call from the same thread as acquisition, normally in a finally block.
   * An expired owner cannot delete a newer owner's lock.
   *
   * @param waitlistId waitlist to unlock
   */
  @Override
  public void releaseServeLock(String waitlistId) {
    Map<String, String> tokens = lockTokens.get();
    if (tokens == null) {
      return;
    }

    String token = tokens.remove(waitlistId);
    if (tokens.isEmpty()) {
      lockTokens.remove();
    }
    if (token == null) {
      return;
    }

    String key = prefix(waitlistId) + "serve-lock";
    redis.execute(RELEASE_LOCK, List.of(key), token);
  }

  private static String prefix(String waitlistId) {
    return "cvnstack:waitlist:" + encode(waitlistId) + ":";
  }

  private static String entryKey(String waitlistId, String entryId) {
    return prefix(waitlistId) + "entry:" + encode(entryId);
  }

  private static String encode(String id) {
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(id.getBytes(StandardCharsets.UTF_8));
  }
}
