#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
binary_dir="$server_dir/bin"
binary_path="$binary_dir/j3-bridge"
plist_path="$HOME/Library/LaunchAgents/io.j3mobile.bridge.plist"
stdout_log="$HOME/Library/Logs/j3-bridge.stdout.log"
stderr_log="$HOME/Library/Logs/j3-bridge.stderr.log"

mkdir -p "$binary_dir" "$HOME/Library/LaunchAgents" "$HOME/Library/Logs"

(
  cd "$server_dir"
  go build -o "$binary_path" ./cmd/bridge
)

cat >"$plist_path" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>io.j3mobile.bridge</string>
  <key>ProgramArguments</key>
  <array>
    <string>$binary_path</string>
  </array>
  <key>WorkingDirectory</key>
  <string>$server_dir</string>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>StandardOutPath</key>
  <string>$stdout_log</string>
  <key>StandardErrorPath</key>
  <string>$stderr_log</string>
</dict>
</plist>
PLIST

launchctl bootout "gui/$(id -u)" io.j3mobile.bridge >/dev/null 2>&1 || true
launchctl bootstrap "gui/$(id -u)" "$plist_path"
launchctl kickstart -k "gui/$(id -u)/io.j3mobile.bridge"

echo "Installed launchd service: $plist_path"
echo "Bridge binary: $binary_path"
echo "Logs:"
echo "  $stdout_log"
echo "  $stderr_log"
