package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Persists waitlists and entries with PostgreSQL constraints and atomic state changes.
 *
 * <p>AI-assisted.
 */
@Repository
public class PostgresWaitlistStorage implements WaitlistStorage {

  private static final RowMapper<Waitlist> WAITLIST_MAPPER = (row, index) -> new Waitlist(
      row.getObject("id", UUID.class).toString(), row.getString("name"),
      row.getInt("max_length"), row.getInt("processing_time_per_entry_minutes"));
  private static final RowMapper<Entry> ENTRY_MAPPER = (row, index) -> new Entry(
      row.getObject("id", UUID.class).toString(),
      row.getObject("waitlist_id", UUID.class).toString(), row.getString("name"),
      row.getInt("priority"), EntryStatus.valueOf(row.getString("status")),
      row.getTimestamp("inserted_at").toInstant(), nullableInstant(row, "called_at"),
      row.getTimestamp("updated_at").toInstant());

  private final JdbcTemplate jdbc;
  private final TransactionTemplate capacityTransaction;

  /**
   * Creates the PostgreSQL adapter using Spring's datasource and transaction manager.
   *
   * @param jdbc JDBC operations for the configured datasource
   * @param transactionManager transaction manager for the same datasource
   */
  public PostgresWaitlistStorage(
      JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
    this.jdbc = jdbc;
    capacityTransaction = new TransactionTemplate(transactionManager);
    // Each count must see committed inserts after a competing row-lock holder commits.
    capacityTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    capacityTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Override
  public Waitlist saveWaitlist(Waitlist waitlist) {
    try {
      return jdbc.queryForObject("""
          INSERT INTO waitlists (id, name, max_length, processing_time_per_entry_minutes)
          VALUES (?, ?, ?, ?)
          ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name,
              max_length = EXCLUDED.max_length,
              processing_time_per_entry_minutes = EXCLUDED.processing_time_per_entry_minutes
          RETURNING *
          """, WAITLIST_MAPPER, parseUuid(waitlist.id(), "waitlist id"), waitlist.name(),
          waitlist.maxLength(), waitlist.processingTimePerEntryMinutes());
    } catch (DataIntegrityViolationException exception) {
      if (isConstraintViolation(exception, "23505", "uq_waitlists_name")) {
        throw ApiException.conflict("Waitlist name already exists");
      }
      throw exception;
    }
  }

  @Override
  public Optional<Waitlist> findWaitlistById(String id) {
    return jdbc.query("SELECT * FROM waitlists WHERE id = ?", WAITLIST_MAPPER,
        parseUuid(id, "waitlist id")).stream().findFirst();
  }

  @Override
  public Optional<Waitlist> findWaitlistByName(String name) {
    return jdbc.query("SELECT * FROM waitlists WHERE name = ?", WAITLIST_MAPPER,
        name).stream().findFirst();
  }

  @Override
  public Entry saveEntry(Entry entry) {
    try {
      return jdbc.queryForObject("""
          INSERT INTO entries (id, waitlist_id, name, priority, status, inserted_at, called_at)
          VALUES (?, ?, ?, ?, ?, ?, ?)
          RETURNING *
          """, ENTRY_MAPPER, parseUuid(entry.id(), "entry id"),
          parseUuid(entry.waitlistId(), "waitlist id"),
          entry.name(), entry.priority(), entry.status().name(), Timestamp.from(entry.insertedAt()),
          entry.calledAt() == null ? null : Timestamp.from(entry.calledAt()));
    } catch (DataIntegrityViolationException exception) {
      if (isConstraintViolation(exception, "23503", "fk_entries_waitlist")) {
        throw ApiException.notFound("Waitlist does not exist");
      }
      throw exception;
    }
  }

  @Override
  public Optional<Entry> updateEntryIfUnchanged(Entry updatedEntry, Entry expectedCurrent) {
    return jdbc.query("""
        UPDATE entries SET status = ?, priority = ?, called_at = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE waitlist_id = ? AND id = ? AND status = ? AND priority = ?
        RETURNING *
        """, ENTRY_MAPPER, updatedEntry.status().name(), updatedEntry.priority(),
        updatedEntry.calledAt() == null ? null : Timestamp.from(updatedEntry.calledAt()),
        parseUuid(expectedCurrent.waitlistId(), "waitlist id"),
        parseUuid(expectedCurrent.id(), "entry id"), expectedCurrent.status().name(),
        expectedCurrent.priority()).stream().findFirst();
  }

  @Override
  public Entry insertEntryIfCapacityAvailable(Entry entry) {
    return Objects.requireNonNull(capacityTransaction.execute(transaction -> {
      UUID waitlistId = parseUuid(entry.waitlistId(), "waitlist id");
      Waitlist waitlist = jdbc.query("SELECT * FROM waitlists WHERE id = ? FOR UPDATE",
          WAITLIST_MAPPER, waitlistId).stream().findFirst()
          .orElseThrow(() -> ApiException.notFound("Waitlist does not exist"));
      Long active = jdbc.queryForObject("""
          SELECT COUNT(*) FROM entries
          WHERE waitlist_id = ? AND status IN ('WAITING', 'CALLED')
          """, Long.class, waitlistId);
      if (Objects.requireNonNull(active) >= waitlist.maxLength()) {
        throw ApiException.capacityExceeded("Waitlist is full");
      }
      return saveEntry(entry);
    }));
  }

  @Override
  public Optional<Entry> callEntryIfWaiting(
      String waitlistId, String entryId, Instant calledAt) {
    return jdbc.query("""
        UPDATE entries SET status = 'CALLED', called_at = ?, updated_at = CURRENT_TIMESTAMP
        WHERE waitlist_id = ? AND id = ? AND status = 'WAITING'
        RETURNING *
        """, ENTRY_MAPPER, Timestamp.from(calledAt), parseUuid(waitlistId, "waitlist id"),
        parseUuid(entryId, "entry id")).stream().findFirst();
  }

  @Override
  public Optional<Entry> findEntry(String waitlistId, String entryId) {
    return jdbc.query("SELECT * FROM entries WHERE waitlist_id = ? AND id = ?",
        ENTRY_MAPPER, parseUuid(waitlistId, "waitlist id"), parseUuid(entryId, "entry id"))
        .stream().findFirst();
  }

  @Override
  public List<Entry> findEntriesByWaitlistId(String waitlistId) {
    return jdbc.query("SELECT * FROM entries WHERE waitlist_id = ?", ENTRY_MAPPER,
        parseUuid(waitlistId, "waitlist id"));
  }

  @Override
  public boolean deleteEntry(String waitlistId, String entryId) {
    return jdbc.update("DELETE FROM entries WHERE waitlist_id = ? AND id = ?",
        parseUuid(waitlistId, "waitlist id"), parseUuid(entryId, "entry id")) == 1;
  }

  @Override
  public long countWaitingEntries(String waitlistId) {
    return Objects.requireNonNull(jdbc.queryForObject(
        "SELECT COUNT(*) FROM entries WHERE waitlist_id = ? AND status = 'WAITING'",
        Long.class, parseUuid(waitlistId, "waitlist id")));
  }

  private static Instant nullableInstant(ResultSet row, String column) throws SQLException {
    Timestamp value = row.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private static UUID parseUuid(final String value, final String fieldName) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw ApiException.badRequest("Invalid " + fieldName);
    }
  }

  private static boolean isConstraintViolation(
      Throwable exception, String sqlState, String constraint) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof PSQLException postgres
          && sqlState.equals(postgres.getSQLState())
          && postgres.getServerErrorMessage() != null
          && constraint.equals(postgres.getServerErrorMessage().getConstraint())) {
        return true;
      }
    }
    return false;
  }
}
