package downstream

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/rs/zerolog"

	"github.com/BurgessTG/J3-Mobile/server/internal/bridge"
	"github.com/BurgessTG/J3-Mobile/server/internal/protocol"
	"github.com/BurgessTG/J3-Mobile/server/internal/upstream"
)

// newTestBridge creates a Bridge suitable for handler tests.
// The upstream client is not connected, so IsConnected() returns false.
func newTestBridge() *bridge.Bridge {
	logger := zerolog.Nop()
	client := upstream.NewClient("ws://localhost:0/test", "", logger)
	return bridge.New(client, logger)
}

func TestHealthHandler(t *testing.T) {
	b := newTestBridge()

	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	rec := httptest.NewRecorder()

	healthHandler(b).ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}

	ct := rec.Header().Get("Content-Type")
	if ct != "application/json" {
		t.Fatalf("expected Content-Type application/json, got %q", ct)
	}

	var body map[string]interface{}
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatalf("failed to decode body: %v", err)
	}

	if body["status"] != "healthy" {
		t.Fatalf("expected status healthy, got %v", body["status"])
	}

	// Upstream is not connected in the test bridge.
	if body["upstreamConnected"] != false {
		t.Fatalf("expected upstreamConnected false, got %v", body["upstreamConnected"])
	}
}

func TestSnapshotHandlerNoSnapshot(t *testing.T) {
	b := newTestBridge()

	req := httptest.NewRequest(http.MethodGet, "/snapshot", nil)
	rec := httptest.NewRecorder()

	snapshotHandler(b).ServeHTTP(rec, req)

	if rec.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected status 503, got %d", rec.Code)
	}
}

func TestSnapshotHandlerWithSnapshot(t *testing.T) {
	b := newTestBridge()

	snapshot := &protocol.OrchestrationReadModel{
		SnapshotSequence: 42,
		Projects:         []protocol.OrchestrationProject{},
		Threads:          []protocol.OrchestrationThread{},
		UpdatedAt:        "2026-03-25T00:00:00Z",
	}
	if err := b.Snapshot.Set(snapshot); err != nil {
		t.Fatalf("failed to set snapshot: %v", err)
	}

	req := httptest.NewRequest(http.MethodGet, "/snapshot", nil)
	rec := httptest.NewRecorder()

	snapshotHandler(b).ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}

	ct := rec.Header().Get("Content-Type")
	if ct != "application/json" {
		t.Fatalf("expected Content-Type application/json, got %q", ct)
	}

	var result protocol.OrchestrationReadModel
	if err := json.Unmarshal(rec.Body.Bytes(), &result); err != nil {
		t.Fatalf("failed to decode body: %v", err)
	}

	if result.SnapshotSequence != 42 {
		t.Fatalf("expected sequence 42, got %d", result.SnapshotSequence)
	}

	if result.UpdatedAt != "2026-03-25T00:00:00Z" {
		t.Fatalf("expected updatedAt 2026-03-25T00:00:00Z, got %s", result.UpdatedAt)
	}
}

func TestSessionsHandlerEmpty(t *testing.T) {
	b := newTestBridge()

	req := httptest.NewRequest(http.MethodGet, "/sessions", nil)
	rec := httptest.NewRecorder()

	sessionsHandler(b).ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}

	ct := rec.Header().Get("Content-Type")
	if ct != "application/json" {
		t.Fatalf("expected Content-Type application/json, got %q", ct)
	}

	var sessions []bridge.SessionInfo
	if err := json.Unmarshal(rec.Body.Bytes(), &sessions); err != nil {
		t.Fatalf("failed to decode body: %v", err)
	}

	if len(sessions) != 0 {
		t.Fatalf("expected 0 sessions, got %d", len(sessions))
	}
}

func TestSessionsHandlerWithSessions(t *testing.T) {
	b := newTestBridge()

	b.Sessions.Add(&bridge.Session{
		ID:     "sess-1",
		UserID: "user-a",
	})
	b.Sessions.Add(&bridge.Session{
		ID:     "sess-2",
		UserID: "user-b",
	})

	req := httptest.NewRequest(http.MethodGet, "/sessions", nil)
	rec := httptest.NewRecorder()

	sessionsHandler(b).ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}

	var sessions []bridge.SessionInfo
	if err := json.Unmarshal(rec.Body.Bytes(), &sessions); err != nil {
		t.Fatalf("failed to decode body: %v", err)
	}

	if len(sessions) != 2 {
		t.Fatalf("expected 2 sessions, got %d", len(sessions))
	}
}
