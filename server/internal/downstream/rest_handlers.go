package downstream

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/BurgessTG/J3-Mobile/server/internal/auth"
	"github.com/BurgessTG/J3-Mobile/server/internal/bridge"
)

type healthResponse struct {
	Status            string `json:"status"`
	Ready             bool   `json:"ready"`
	DeploymentMode    string `json:"deploymentMode"`
	ListenAddr        string `json:"listenAddr"`
	PublicBaseURL     string `json:"publicBaseUrl,omitempty"`
	UpstreamConnected bool   `json:"upstreamConnected"`
	SnapshotAvailable bool   `json:"snapshotAvailable"`
	AuthMode          string `json:"authMode"`
	LastUpstreamError string `json:"lastUpstreamError,omitempty"`
	StartedAt         string `json:"startedAt"`
}

// healthHandler returns the server health status including upstream connectivity.
func healthHandler(b *bridge.Bridge, cfg RouterConfig) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")

		resp := healthResponse{
			Status:            "healthy",
			Ready:             b.Ready(),
			DeploymentMode:    "personal",
			ListenAddr:        cfg.ListenAddr,
			PublicBaseURL:     cfg.PublicBaseURL,
			UpstreamConnected: b.Upstream.IsConnected(),
			SnapshotAvailable: b.SnapshotAvailable(),
			AuthMode:          cfg.Authenticator.HealthMode(),
			LastUpstreamError: b.Upstream.LastError(),
			StartedAt:         b.StartedAt.UTC().Format(time.RFC3339),
		}

		_ = json.NewEncoder(w).Encode(resp)
	}
}

// snapshotHandler returns the cached OrchestrationReadModel as JSON.
func snapshotHandler(b *bridge.Bridge) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")

		raw := b.Snapshot.GetRaw()
		if raw == nil {
			http.Error(w, `{"error":"snapshot not available"}`, http.StatusServiceUnavailable)
			return
		}

		_, _ = w.Write(raw)
	}
}

// sessionsHandler returns the list of active mobile sessions for the same credential.
func sessionsHandler(b *bridge.Bridge) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		credentialID := auth.CredentialIDFromContext(r.Context())
		_ = json.NewEncoder(w).Encode(b.Sessions.ListByCredential(credentialID))
	}
}
