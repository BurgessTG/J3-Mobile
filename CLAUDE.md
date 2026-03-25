# J3 Mobile

Mobile orchestrator for T3 Code. Go bridge server + KMP Compose Multiplatform app.

## Before Committing

All checks must pass:
- `make server-fmt && make server-lint && make server-test && make server-build`
- `make mobile-build` (if Kotlin changed, requires Android SDK)

## Conventional Commits

- feat: / fix: / refactor: / chore: / docs:
- Work on `dev` branch. Never commit directly to `main`.

## Go Server (`server/`)

- chi for HTTP routing, gorilla/websocket for WS, golang-jwt for auth
- Protocol types in `internal/protocol/` — use `json.RawMessage` for pass-through
- Test all packages: `go test ./... -race`
- Lint: `golangci-lint run`

## Mobile (`mobile/`)

- KMP + Compose Multiplatform, namespace: `io.j3mobile`
- Protocol types in `commonMain/protocol/` must match Go `internal/protocol/`
- On Linux: only commonMain + androidMain targets build
- iOS targets require macOS + Xcode

## Architecture

```
Phone (KMP App) <--WS/REST--> Go Bridge Server <--WS--> T3 Code Server
```

The bridge connects upstream to a running T3 Code WebSocket server and exposes a mobile-friendly API downstream.
