#!/usr/bin/env bash
set -euo pipefail

listen_addr="${1:-127.0.0.1:8181}"

if ! command -v tailscale >/dev/null 2>&1; then
  echo "tailscale CLI not found in PATH" >&2
  exit 1
fi

tailscale serve --bg --https=443 "http://${listen_addr}"

echo "Tailscale Serve is forwarding HTTPS traffic to http://${listen_addr}"
echo "Use your tailnet hostname, for example:"
echo "  https://<machine>.<tailnet>.ts.net"
echo "  wss://<machine>.<tailnet>.ts.net/ws"
