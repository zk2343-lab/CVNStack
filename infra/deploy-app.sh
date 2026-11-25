#!/bin/bash
# AI-assisted.
# Copy this repository onto the CVNStack VM and start the Compose stack.

set -euo pipefail

export PATH="${HOME}/google-cloud-sdk/bin:/opt/homebrew/bin:${PATH}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
INFRA="$(cd "$(dirname "$0")" && pwd)"
cd "${INFRA}"

NAME="$(pulumi stack output instanceName)"
ZONE="$(pulumi stack output instanceZone)"
PROJECT="$(pulumi config get gcp:project)"

ARCHIVE="$(mktemp -d)/cvnstack.tgz"
COPYFILE_DISABLE=1 tar -C "${ROOT}" -czf "${ARCHIVE}" \
  --exclude .git \
  --exclude target \
  --exclude node_modules \
  --exclude .cursor \
  --exclude reports \
  .

gcloud compute scp --quiet --tunnel-through-iap \
  --project "${PROJECT}" --zone "${ZONE}" \
  "${ARCHIVE}" "${NAME}:/tmp/cvnstack.tgz"

gcloud compute ssh --quiet --tunnel-through-iap \
  --project "${PROJECT}" --zone "${ZONE}" "${NAME}" --command "$(cat <<'EOF'
set -euo pipefail
sudo mkdir -p /opt/cvnstack
sudo tar -C /opt/cvnstack -xzf /tmp/cvnstack.tgz
for _ in $(seq 1 60); do
  if sudo docker info >/dev/null 2>&1 && sudo systemctl cat cvnstack.service >/dev/null 2>&1; then
    break
  fi
  sleep 5
done
sudo docker info >/dev/null
sudo systemctl cat cvnstack.service >/dev/null
sudo systemctl restart cvnstack.service
sudo docker compose -f /opt/cvnstack/docker-compose.yml ps
EOF
)"
