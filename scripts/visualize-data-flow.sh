#!/usr/bin/env bash
# AI-assisted.
# Opens the request-flow visualizer and optionally runs live preset cases.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VISUALIZER="$ROOT/scripts/data-flow-visualizer.html"
REPORT="$ROOT/target/data-flow-report.html"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

PG_NAME="cvnstack-flow-pg"
REDIS_NAME="cvnstack-flow-redis"
APP_PID=""
MODE="${1:-static}"

usage() {
  cat <<'EOF'
Usage: scripts/visualize-data-flow.sh [static|live]

  static  Open the HTML visualizer with the five preset request flows.
  live    Start local PostgreSQL and Redis, run the app, execute the presets,
          write target/data-flow-report.html, and open both pages.

Presets:
  happy-path         create waitlist, add Alice/Bob, GET ranks, serve-next, PATCH SERVED
  cache-rebuild      flush Redis, GET rebuilds the missing rank and entry keys
  overlapping-serve  two concurrent serve-next calls
  delete-shifts-rank DELETE the first entry, remaining position becomes 1
  redis-failure      documented in the static visualizer (injected Redis faults)
EOF
}

open_page() {
  local file="$1"
  if command -v open >/dev/null 2>&1; then
    open "$file"
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$file"
  else
    echo "Open $file in a browser."
  fi
}

cleanup() {
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null || true
  fi
  docker rm -f "$PG_NAME" "$REDIS_NAME" >/dev/null 2>&1 || true
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

start_stores() {
  docker rm -f "$PG_NAME" "$REDIS_NAME" >/dev/null 2>&1 || true
  docker run -d --name "$PG_NAME" \
    -e POSTGRES_USER=cvnstack -e POSTGRES_PASSWORD=cvnstack -e POSTGRES_DB=cvnstack \
    -p 5432:5432 postgres:16-alpine >/dev/null
  docker run -d --name "$REDIS_NAME" -p 6379:6379 redis:7.4-alpine >/dev/null
  for _ in $(seq 1 30); do
    if docker exec "$PG_NAME" pg_isready -U cvnstack >/dev/null 2>&1 \
        && docker exec "$REDIS_NAME" redis-cli ping >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  echo "Local PostgreSQL or Redis did not become ready." >&2
  exit 1
}

start_app() {
  export DATABASE_URL="jdbc:postgresql://localhost:5432/cvnstack"
  export DATABASE_USERNAME="cvnstack"
  export DATABASE_PASSWORD="cvnstack"
  export REDIS_HOST="localhost"
  export REDIS_PORT="6379"
  (
    cd "$ROOT"
    mvn -q -DskipTests spring-boot:run
  ) >/tmp/cvnstack-flow-app.log 2>&1 &
  APP_PID=$!
  for _ in $(seq 1 60); do
    if curl -sf "http://localhost:8080/health" >/dev/null; then
      return
    fi
    if ! kill -0 "$APP_PID" 2>/dev/null; then
      echo "Application failed to start. See /tmp/cvnstack-flow-app.log" >&2
      exit 1
    fi
    sleep 1
  done
  echo "Timed out waiting for GET /health." >&2
  exit 1
}

pg_rows() {
  docker exec "$PG_NAME" psql -U cvnstack -d cvnstack -Atc \
    "SELECT name || ' ' || status || ' p' || priority FROM entries ORDER BY inserted_at;"
}

redis_order() {
  local keys key
  keys="$(docker exec "$REDIS_NAME" redis-cli --scan --pattern 'cvnstack:waitlist:*:waiting' || true)"
  if [[ -z "$keys" ]]; then
    echo "(no waiting keys)"
    return
  fi
  while IFS= read -r key; do
    [[ -z "$key" ]] && continue
    echo "$key => $(docker exec "$REDIS_NAME" redis-cli ZRANGE "$key" 0 -1)"
  done <<< "$keys"
}

json_field() {
  python3 -c "import json,sys; print(json.load(sys.stdin).get(sys.argv[1], ''))" "$1"
}

append_step() {
  local title="$1"
  local request="$2"
  local response="$3"
  local pg_state="$4"
  local redis_state="$5"
  cat >> "$REPORT" <<EOF
<section>
  <h2>$title</h2>
  <pre>Request: $request

Response: $response

PostgreSQL:
$pg_state

Redis:
$redis_state</pre>
</section>
EOF
}

run_live() {
  need_cmd docker
  need_cmd curl
  need_cmd mvn
  need_cmd python3
  trap cleanup EXIT
  mkdir -p "$ROOT/target"
  start_stores
  start_app

  cat > "$REPORT" <<'EOF'
<!DOCTYPE html>
<!-- AI-assisted. -->
<html lang="en"><head><meta charset="utf-8"><title>CVNStack live data-flow report</title>
<style>
  body { font: 15px/1.45 sans-serif; background: #0f1419; color: #e8eef4; margin: 24px; }
  h1, h2 { font-weight: 600; }
  section { background: #1a232d; border-radius: 12px; padding: 16px; margin: 16px 0; }
  pre { white-space: pre-wrap; }
</style></head><body>
<h1>Live preset report</h1>
<p>PostgreSQL committed first. Redis ranks and entry keys follow each mutation.</p>
EOF

  local created waitlist_id alice_id bob_id get_alice served
  created="$(curl -sS -H 'Content-Type: application/json' \
    -d '{"name":"Visualizer Support","maxLength":10,"processingTimePerEntryMinutes":5}' \
    http://localhost:8080/waitlists)"
  waitlist_id="$(printf '%s' "$created" | json_field id)"
  append_step "happy-path: create waitlist" "POST /waitlists" "$created" "$(pg_rows)" \
    "waitlists are not cached"

  local alice bob
  alice="$(curl -sS -H 'Content-Type: application/json' \
    -d '{"name":"Alice","priority":1}' \
    "http://localhost:8080/waitlists/${waitlist_id}/entries")"
  alice_id="$(printf '%s' "$alice" | json_field entryId)"
  bob="$(curl -sS -H 'Content-Type: application/json' \
    -d '{"name":"Bob","priority":5}' \
    "http://localhost:8080/waitlists/${waitlist_id}/entries")"
  bob_id="$(printf '%s' "$bob" | json_field entryId)"
  append_step "happy-path: add Alice then Bob" \
    "POST .../entries" "$alice"$'\n'"$bob" "$(pg_rows)" "$(redis_order "$waitlist_id")"

  get_alice="$(curl -sS "http://localhost:8080/waitlists/${waitlist_id}/entries/${alice_id}")"
  append_step "happy-path: GET Alice" "GET .../entries/{alice}" "$get_alice" \
    "$(pg_rows)" "$(redis_order "$waitlist_id")"

  local called
  called="$(curl -sS -X POST "http://localhost:8080/waitlists/${waitlist_id}/serve-next")"
  append_step "happy-path: serve-next" "POST .../serve-next" "$called" \
    "$(pg_rows)" "$(redis_order "$waitlist_id")"

  served="$(curl -sS -X PATCH -H 'Content-Type: application/json' \
    -d '{"status":"SERVED"}' \
    "http://localhost:8080/waitlists/${waitlist_id}/entries/${bob_id}")"
  append_step "happy-path: PATCH Bob SERVED" "PATCH Bob" "$served" \
    "$(pg_rows)" "$(redis_order "$waitlist_id")"

  docker exec "$REDIS_NAME" redis-cli FLUSHDB >/dev/null
  get_alice="$(curl -sS "http://localhost:8080/waitlists/${waitlist_id}/entries/${alice_id}")"
  append_step "cache-rebuild: GET Alice after Redis flush" \
    "FLUSHDB then GET Alice" "$get_alice" "$(pg_rows)" "$(redis_order "$waitlist_id")"

  local other other_id first_id second_id
  other="$(curl -sS -H 'Content-Type: application/json' \
    -d '{"name":"Overlap","maxLength":10,"processingTimePerEntryMinutes":5}' \
    http://localhost:8080/waitlists)"
  other_id="$(printf '%s' "$other" | json_field id)"
  first_id="$(curl -sS -H 'Content-Type: application/json' -d '{"name":"One","priority":5}' \
    "http://localhost:8080/waitlists/${other_id}/entries" | json_field entryId)"
  second_id="$(curl -sS -H 'Content-Type: application/json' -d '{"name":"Two","priority":1}' \
    "http://localhost:8080/waitlists/${other_id}/entries" | json_field entryId)"
  curl -sS -o /tmp/cvnstack-left.json -w '%{http_code}' \
    -X POST "http://localhost:8080/waitlists/${other_id}/serve-next" \
    > /tmp/cvnstack-left.code &
  local left_pid=$!
  curl -sS -o /tmp/cvnstack-right.json -w '%{http_code}' \
    -X POST "http://localhost:8080/waitlists/${other_id}/serve-next" \
    > /tmp/cvnstack-right.code &
  local right_pid=$!
  wait "$left_pid" "$right_pid"
  append_step "overlapping-serve: two serve-next calls" \
    "concurrent POST .../serve-next for $first_id and $second_id" \
    "HTTP $(cat /tmp/cvnstack-left.code) and $(cat /tmp/cvnstack-right.code)" \
    "$(pg_rows)" "$(redis_order "$other_id")"

  local leftover leftover_id remaining
  leftover="$(curl -sS -H 'Content-Type: application/json' \
    -d '{"name":"Delete Demo","maxLength":10,"processingTimePerEntryMinutes":5}' \
    http://localhost:8080/waitlists)"
  leftover_id="$(printf '%s' "$leftover" | json_field id)"
  local doomed kept
  doomed="$(curl -sS -H 'Content-Type: application/json' -d '{"name":"Doomed","priority":1}' \
    "http://localhost:8080/waitlists/${leftover_id}/entries" | json_field entryId)"
  kept="$(curl -sS -H 'Content-Type: application/json' -d '{"name":"Kept","priority":1}' \
    "http://localhost:8080/waitlists/${leftover_id}/entries" | json_field entryId)"
  curl -sS -X DELETE "http://localhost:8080/waitlists/${leftover_id}/entries/${doomed}" >/dev/null
  remaining="$(curl -sS "http://localhost:8080/waitlists/${leftover_id}/entries/${kept}")"
  append_step "delete-shifts-rank: DELETE first entry" \
    "DELETE $doomed then GET $kept" "$remaining" "$(pg_rows)" "$(redis_order "$leftover_id")"

  echo "</body></html>" >> "$REPORT"
  echo "Wrote $REPORT"
  open_page "$VISUALIZER"
  open_page "$REPORT"
}

case "$MODE" in
  -h|--help) usage ;;
  static)
    if curl -sf "http://localhost:8080/health" >/dev/null; then
      open_page "http://localhost:8080/visualizer.html"
    else
      open_page "$VISUALIZER"
    fi
    ;;
  live) run_live ;;
  *) usage; exit 1 ;;
esac
