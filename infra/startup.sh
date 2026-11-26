#!/bin/bash
# AI-assisted.
# Install Docker on the VM and start Compose when the application files are present.

set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

if ! command -v docker >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  # shellcheck disable=SC1091
  . /etc/os-release
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian ${VERSION_CODENAME} stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

systemctl enable --now docker
mkdir -p /opt/cvnstack

# Passthrough load balancer keeps the client port. Map 80 to the app on 8080.
iptables -t nat -C PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports 8080 2>/dev/null \
  || iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports 8080

cat > /etc/systemd/system/cvnstack.service <<'EOF'
[Unit]
Description=CVNStack Compose stack
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/cvnstack
ExecStart=/usr/bin/docker compose up -d --build
ExecStop=/usr/bin/docker compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable cvnstack.service
if [[ -f /opt/cvnstack/docker-compose.yml ]]; then
  systemctl start cvnstack.service
fi
