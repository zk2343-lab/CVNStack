package com.cvnstack.controller;

import com.cvnstack.model.response.StoreSnapshotResponse;
import com.cvnstack.service.StoreSnapshotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes live PostgreSQL and Redis contents to the request-flow visualizer.
 *
 * <p>AI-assisted.
 */
@RestController
public class StoreSnapshotController {

  private final StoreSnapshotService snapshot;

  /**
   * Creates the snapshot controller.
   *
   * @param snapshot store reader
   */
  public StoreSnapshotController(StoreSnapshotService snapshot) {
    this.snapshot = snapshot;
  }

  /**
   * Returns the current waitlist tables and Redis keys.
   *
   * @return 200 with the snapshot
   */
  @GetMapping("/debug/stores")
  public ResponseEntity<StoreSnapshotResponse> stores() {
    return ResponseEntity.ok(snapshot.snapshot());
  }

  /**
   * Empties Redis so a later GET can demonstrate cache rebuild.
   *
   * @return 204
   */
  @PostMapping("/debug/flush-redis")
  public ResponseEntity<Void> flushRedis() {
    snapshot.flushRedis();
    return ResponseEntity.noContent().build();
  }

  /**
   * Dev reset: removes every PostgreSQL waitlist and entry, then flushes Redis.
   *
   * @return 204
   */
  @PostMapping("/debug/wipe-stores")
  public ResponseEntity<Void> wipeStores() {
    snapshot.wipeStores();
    return ResponseEntity.noContent().build();
  }
}
