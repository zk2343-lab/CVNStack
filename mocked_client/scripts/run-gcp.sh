#!/usr/bin/env bash
# AI-assisted.
# Builds the restaurant client locally and points it at the hosted waitlist service.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=use-jdk21.sh
source "$(dirname "$0")/use-jdk21.sh"
cd "${ROOT}"
mvn -q -DskipTests package
exec java -jar target/mocked-client.jar serve --mode gcp "$@"
