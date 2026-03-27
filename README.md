# J3 Mobile

Mobile orchestrator for [T3 Code](https://github.com/pingdotgg/t3code). Control your coding agents from your phone.

## Architecture

```
Phone (KMP App)  <--WS/REST-->  Go Bridge Server  <--WS-->  T3 Code Server
                                (J3 Bridge)                   (existing)
```

**J3 Bridge** is a Go WebSocket proxy that connects to a running T3 Code server and exposes a mobile-optimized API. The mobile app connects to the bridge, not directly to T3 Code.

## Components

- **`server/`** — Go bridge server (chi + gorilla/websocket)
- **`mobile/`** — Kotlin Multiplatform + Compose Multiplatform app (Android + iOS)

## Quick Start

### Bridge Server

```bash
cd server
cp .env.example .env  # configure T3 Code URL and auth
go run ./cmd/pair-token create --name "Jacob iPhone"
go run ./cmd/bridge
```

The bridge is personal-only in this phase: one bridge fronts one personal T3 Code instance for one owner. It listens on loopback by default and is intended to be exposed remotely through Tailscale, not by binding to all interfaces.

Bridge auth now defaults to opaque pairing tokens over `Authorization: Bearer <token>`. Legacy JWT auth is migration-only and is accepted only when `ALLOW_LEGACY_JWT=true`.

### Mobile App

Requires Android Studio or Xcode (on Mac for iOS).

```bash
cd mobile
./gradlew composeApp:compileDebugKotlinAndroid  # Android compile check
```

For iPhone setup, Xcode packaging, and TestFlight release flow, see `mobile/README.md`.

The `mobile/iosApp` folder contains the SwiftUI host shell that embeds `MainViewController()` from the shared KMP module.

### Remote Access With Tailscale

Local simulator:

```bash
http://127.0.0.1:8181
```

Remote iPhone:

```bash
cd server
./scripts/setup-tailscale-serve.sh
```

That exposes the local bridge through Tailscale HTTPS so the phone can use `https://<machine>.<tailnet>.ts.net` and `wss://<machine>.<tailnet>.ts.net/ws`.

## Development

```bash
make check  # runs all format, lint, test, and build gates
```

## License

MIT
