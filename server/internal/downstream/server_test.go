package downstream

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"

	"github.com/BurgessTG/J3-Mobile/server/internal/auth"
	"github.com/BurgessTG/J3-Mobile/server/internal/bridge"
	"github.com/BurgessTG/J3-Mobile/server/internal/pairtoken"
	"github.com/BurgessTG/J3-Mobile/server/internal/protocol"
	"github.com/BurgessTG/J3-Mobile/server/internal/upstream"
)

const testSecret = "test-secret-key"

func newTestBridge() *bridge.Bridge {
	logger := zerolog.Nop()
	client := upstream.NewClient("ws://localhost:0/test", "", logger)
	return bridge.New(client, logger)
}

func newConnectedTestBridge(t *testing.T) *bridge.Bridge {
	t.Helper()

	logger := zerolog.Nop()
	upgrader := websocket.Upgrader{}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			t.Fatalf("upgrade failed: %v", err)
		}
		defer conn.Close()

		for {
			if _, _, err := conn.ReadMessage(); err != nil {
				return
			}
		}
	}))
	t.Cleanup(server.Close)

	client := upstream.NewClient("ws"+strings.TrimPrefix(server.URL, "http"), "", logger)
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	t.Cleanup(cancel)
	if err := client.Connect(ctx); err != nil {
		t.Fatalf("Connect failed: %v", err)
	}
	t.Cleanup(func() { _ = client.Close() })

	return bridge.New(client, logger)
}

func newRouterConfigWithPairingToken(t *testing.T) (RouterConfig, string, pairtoken.Credential) {
	t.Helper()

	store := pairtoken.NewStore(filepath.Join(t.TempDir(), "tokens.json"))
	credential, token, err := store.Create("Jacob iPhone")
	if err != nil {
		t.Fatalf("Create failed: %v", err)
	}

	return RouterConfig{
		Authenticator: &auth.Authenticator{
			PairingTokens: store,
			JWTSecret:     testSecret,
		},
		ListenAddr:    "127.0.0.1:8181",
		PublicBaseURL: "https://bridge.example.ts.net",
	}, token, credential
}

func TestNewRouter_HealthIsPublic(t *testing.T) {
	b := newTestBridge()
	cfg, _, _ := newRouterConfigWithPairingToken(t)
	router := NewRouter(b, cfg)

	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}
}

func TestNewRouter_SnapshotRequiresAuth(t *testing.T) {
	b := newTestBridge()
	cfg, _, _ := newRouterConfigWithPairingToken(t)
	router := NewRouter(b, cfg)

	req := httptest.NewRequest(http.MethodGet, "/snapshot", nil)
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected status 401, got %d", rec.Code)
	}
}

func TestNewRouter_SnapshotAcceptsPairingToken(t *testing.T) {
	b := newTestBridge()
	cfg, token, _ := newRouterConfigWithPairingToken(t)
	router := NewRouter(b, cfg)

	req := httptest.NewRequest(http.MethodGet, "/snapshot", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected status 503 when snapshot is missing, got %d", rec.Code)
	}
}

func TestNewRouter_LegacyJWTRequiresExplicitFlag(t *testing.T) {
	b := newTestBridge()
	cfg, _, _ := newRouterConfigWithPairingToken(t)
	router := NewRouter(b, cfg)

	token, err := auth.SignToken(testSecret, "user-1")
	if err != nil {
		t.Fatalf("SignToken failed: %v", err)
	}

	req := httptest.NewRequest(http.MethodGet, "/snapshot", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected status 401 when legacy JWT is disabled, got %d", rec.Code)
	}
}

func TestNewRouter_LegacyJWTCanBeEnabled(t *testing.T) {
	b := newTestBridge()
	cfg, _, _ := newRouterConfigWithPairingToken(t)
	cfg.Authenticator.AllowLegacyJWT = true
	router := NewRouter(b, cfg)

	token, err := auth.SignToken(testSecret, "user-2")
	if err != nil {
		t.Fatalf("SignToken failed: %v", err)
	}

	req := httptest.NewRequest(http.MethodGet, "/snapshot", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected status 503 when snapshot is missing, got %d", rec.Code)
	}
}

func TestNewRouter_WebSocketRequiresBearerToken(t *testing.T) {
	b := newTestBridge()
	cfg, _, _ := newRouterConfigWithPairingToken(t)
	router := NewRouter(b, cfg)
	server := httptest.NewServer(router)
	t.Cleanup(server.Close)

	wsURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws"

	dialer := websocket.Dialer{}
	_, _, err := dialer.Dial(wsURL, nil)
	if err == nil {
		t.Fatal("expected websocket handshake to fail without auth")
	}
}

func TestNewRouter_WebSocketAcceptsPairingTokenWithoutOrigin(t *testing.T) {
	b := newTestBridge()
	cfg, token, _ := newRouterConfigWithPairingToken(t)
	router := NewRouter(b, cfg)
	server := httptest.NewServer(router)
	t.Cleanup(server.Close)

	wsURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws"
	header := http.Header{}
	header.Set("Authorization", "Bearer "+token)

	dialer := websocket.Dialer{}
	conn, _, err := dialer.Dial(wsURL, header)
	if err != nil {
		t.Fatalf("websocket dial failed: %v", err)
	}
	defer conn.Close()
}

func TestNewRouter_WebSocketRejectsUnexpectedOrigin(t *testing.T) {
	b := newTestBridge()
	cfg, token, _ := newRouterConfigWithPairingToken(t)
	router := NewRouter(b, cfg)
	server := httptest.NewServer(router)
	t.Cleanup(server.Close)

	wsURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws"
	header := http.Header{}
	header.Set("Authorization", "Bearer "+token)
	header.Set("Origin", "https://evil.example")

	dialer := websocket.Dialer{}
	_, _, err := dialer.Dial(wsURL, header)
	if err == nil {
		t.Fatal("expected websocket handshake to fail for unexpected origin")
	}
}

func TestNewRouter_WebSocketAcceptsConfiguredOrigin(t *testing.T) {
	b := newTestBridge()
	cfg, token, _ := newRouterConfigWithPairingToken(t)
	router := NewRouter(b, cfg)
	server := httptest.NewServer(router)
	t.Cleanup(server.Close)

	wsURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws"
	header := http.Header{}
	header.Set("Authorization", "Bearer "+token)
	header.Set("Origin", "https://bridge.example.ts.net")

	dialer := websocket.Dialer{}
	conn, _, err := dialer.Dial(wsURL, header)
	if err != nil {
		t.Fatalf("websocket dial failed: %v", err)
	}
	defer conn.Close()
}

func TestHealthHandlerIncludesReadinessMetadata(t *testing.T) {
	b := newConnectedTestBridge(t)
	cfg, _, _ := newRouterConfigWithPairingToken(t)

	snapshot := &protocol.OrchestrationReadModel{
		SnapshotSequence: 1,
		Projects:         []protocol.OrchestrationProject{},
		Threads:          []protocol.OrchestrationThread{},
		UpdatedAt:        "2026-03-25T00:00:00Z",
	}
	if err := b.Snapshot.Set(snapshot); err != nil {
		t.Fatalf("failed to set snapshot: %v", err)
	}

	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	rec := httptest.NewRecorder()

	healthHandler(b, cfg).ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}

	var body map[string]any
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatalf("failed to decode body: %v", err)
	}

	if body["ready"] != true {
		t.Fatalf("expected ready true, got %v", body["ready"])
	}
	if body["listenAddr"] != "127.0.0.1:8181" {
		t.Fatalf("expected listenAddr to round-trip, got %v", body["listenAddr"])
	}
	if body["authMode"] != "pairing-token" {
		t.Fatalf("expected pairing-token auth mode, got %v", body["authMode"])
	}
}

func TestSessionsHandlerReturnsOnlyMatchingCredential(t *testing.T) {
	b := newTestBridge()
	cfg, token, credential := newRouterConfigWithPairingToken(t)
	router := NewRouter(b, cfg)

	now := time.Date(2026, 3, 25, 15, 0, 0, 0, time.UTC)

	sameCredential := &bridge.Session{
		ID:           "session-1",
		CredentialID: credential.ID,
		DeviceName:   "Jacob iPhone",
		RemoteAddr:   "100.64.0.1:1234",
		CreatedAt:    now,
	}
	sameCredential.Touch(now)
	b.Sessions.Add(sameCredential)

	otherCredential := &bridge.Session{
		ID:           "session-2",
		CredentialID: "cred_other",
		DeviceName:   "Other Device",
		RemoteAddr:   "100.64.0.2:4321",
		CreatedAt:    now,
	}
	otherCredential.Touch(now)
	b.Sessions.Add(otherCredential)

	req := httptest.NewRequest(http.MethodGet, "/sessions", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}

	var sessions []bridge.SessionInfo
	if err := json.Unmarshal(rec.Body.Bytes(), &sessions); err != nil {
		t.Fatalf("failed to decode body: %v", err)
	}

	if len(sessions) != 1 {
		t.Fatalf("expected 1 session, got %d", len(sessions))
	}
	if sessions[0].SessionID != "session-1" {
		t.Fatalf("expected matching session to be returned, got %s", sessions[0].SessionID)
	}
}
