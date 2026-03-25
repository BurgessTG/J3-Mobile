package downstream

import (
	"encoding/json"
	"net/http"

	"github.com/BurgessTG/J3-Mobile/server/internal/bridge"
)

// healthHandler returns the server health status including upstream connectivity.
func healthHandler(b *bridge.Bridge) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")

		resp := map[string]interface{}{
			"status":            "healthy",
			"upstreamConnected": b.Upstream.IsConnected(),
		}

		json.NewEncoder(w).Encode(resp)
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

		w.Write(raw)
	}
}

// sessionsHandler returns the list of active mobile sessions.
func sessionsHandler(b *bridge.Bridge) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(b.Sessions.List())
	}
}
