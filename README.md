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
go run ./cmd/bridge
```

### Mobile App

Requires Android Studio or Xcode (on Mac for iOS).

```bash
cd mobile
./gradlew composeApp:installDebug  # Android
```

For iPhone setup, Xcode packaging, and TestFlight release flow, see `mobile/README.md`.

## Development

```bash
make check  # runs all format, lint, test, and build gates
```

## License

MIT
