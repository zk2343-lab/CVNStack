#!/usr/bin/env bash
# AI-assisted.
# Calls the waitlist service on a local port. Does not start the restaurant server.
set -euo pipefail

PORT="${1:-${PORT:-8080}}"
HOST="${HOST:-127.0.0.1}"
if [[ -n "${BASE_URL:-}" ]]; then
  BASE="${BASE_URL%/}"
else
  BASE="http://${HOST}:${PORT}"
fi

INSTANCE_DIR="${INSTANCE_DIR:-${HOME}/.cvnstack}"
mkdir -p "${INSTANCE_DIR}"
MACHINE_A="${INSTANCE_DIR}/mocked-client-instance"
MACHINE_B="${INSTANCE_DIR}/mocked-client-instance-b"

ensure_id() {
  local file="$1"
  if [[ ! -s "${file}" ]]; then
    python3 -c 'import uuid; print(uuid.uuid4())' > "${file}"
  fi
  tr -d '[:space:]' < "${file}"
}

ID_A="$(ensure_id "${MACHINE_A}")"
ID_B="$(ensure_id "${MACHINE_B}")"
if [[ "${ID_A}" == "${ID_B}" ]]; then
  echo "The two instance files hold the same id." >&2
  exit 1
fi

echo "Upstream ${BASE}"
echo "Machine A ${ID_A}"
echo "Machine B ${ID_B}"

call() {
  local instance="$1"
  local method="$2"
  local path="$3"
  local data="${4:-}"
  local body_file
  body_file="$(mktemp)"
  local code
  if [[ -n "${data}" ]]; then
    code="$(curl -sS -m 20 -o "${body_file}" -w '%{http_code}' -X "${method}" \
      -H "X-Client-Instance: ${instance}" \
      -H 'Accept: application/json' \
      -H 'Content-Type: application/json' \
      "${BASE}${path}" \
      -d "${data}")"
  else
    code="$(curl -sS -m 20 -o "${body_file}" -w '%{http_code}' -X "${method}" \
      -H "X-Client-Instance: ${instance}" \
      -H 'Accept: application/json' \
      "${BASE}${path}")"
  fi
  echo "${method} ${path} -> ${code}"
  cat "${body_file}"
  echo
  LAST_CODE="${code}"
  LAST_BODY="$(cat "${body_file}")"
  rm -f "${body_file}"
}

json_field() {
  python3 -c 'import json,sys; value=json.load(sys.stdin).get(sys.argv[1]); print("" if value is None else value)' "$1" <<<"${LAST_BODY}"
}

expect() {
  local wanted="$1"
  if [[ "${LAST_CODE}" != "${wanted}" ]]; then
    echo "Expected HTTP ${wanted} but got ${LAST_CODE}" >&2
    exit 1
  fi
}

call "${ID_A}" GET /health
expect 200

NAME="Dining room $(date +%s)"
CREATE="$(python3 -c 'import json,sys; print(json.dumps({"name":sys.argv[1],"maxLength":10,"processingTimePerEntryMinutes":5}))' "${NAME}")"
call "${ID_A}" POST /waitlists "${CREATE}"
expect 200
WAITLIST="$(json_field id)"

WALK="$(python3 -c 'import json; print(json.dumps({"name":"Walk-in","priority":1}))')"
call "${ID_A}" POST "/waitlists/${WAITLIST}/entries" "${WALK}"
expect 200
WALK_ID="$(json_field entryId)"

RUSH="$(python3 -c 'import json; print(json.dumps({"name":"Rush table","priority":5}))')"
call "${ID_A}" POST "/waitlists/${WAITLIST}/entries" "${RUSH}"
expect 200
RUSH_ID="$(json_field entryId)"
if [[ "$(json_field position)" != "1" ]]; then
  echo "Priority 5 should be position 1." >&2
  exit 1
fi

WINDOW="$(python3 -c 'import json; print(json.dumps({"name":"Window","priority":1}))')"
call "${ID_B}" POST "/waitlists/${WAITLIST}/entries" "${WINDOW}"
expect 200
WINDOW_ID="$(json_field entryId)"

call "${ID_A}" GET "/waitlists/${WAITLIST}/entries/${RUSH_ID}"
expect 200
echo "Rush position $(json_field position), estimate $(json_field estimatedWaitTimeMinutes) min"

call "${ID_A}" POST "/waitlists/${WAITLIST}/serve-next"
expect 200
if [[ "$(json_field entryId)" != "${RUSH_ID}" ]]; then
  echo "Serve-next should call the priority 5 party." >&2
  exit 1
fi

call "${ID_A}" PATCH "/waitlists/${WAITLIST}/entries/${RUSH_ID}" '{"status":"SERVED"}'
expect 200
call "${ID_A}" PATCH "/waitlists/${WAITLIST}/entries/${WALK_ID}" '{"status":"CANCELLED"}'
expect 200
call "${ID_B}" PATCH "/waitlists/${WAITLIST}/entries/${WINDOW_ID}" '{"status":"CANCELLED"}'
expect 200

call "${ID_A}" GET "/waitlists/${WAITLIST}"
expect 200
BOARD_FILE="$(mktemp)"
printf '%s' "${LAST_BODY}" > "${BOARD_FILE}"
python3 - "${ID_A}" "${ID_B}" "${BOARD_FILE}" <<'PY'
import json
import sys

wanted = {sys.argv[1], sys.argv[2]}
with open(sys.argv[3], encoding="utf-8") as handle:
    body = json.load(handle)
found = set()

def walk(node):
    if isinstance(node, dict):
        instance = node.get("clientInstance")
        if instance:
            found.add(instance)
        for value in node.values():
            walk(value)
    elif isinstance(node, list):
        for item in node:
            walk(item)

walk(body)
if not found:
    print("NOTE: this service did not return clientInstance.")
    print("The client still sent two different X-Client-Instance values.")
    print("PASS restaurant flow. Instance echo waits for the deployed header.")
elif found == wanted:
    print("Service returned both instance ids.")
    print("PASS")
else:
    print("Unexpected clientInstance values:", ", ".join(sorted(found)))
    sys.exit(1)
PY
rm -f "${BOARD_FILE}"
