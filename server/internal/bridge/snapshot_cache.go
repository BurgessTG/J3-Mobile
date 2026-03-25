package bridge

import (
	"encoding/json"
	"sync"

	"github.com/BurgessTG/J3-Mobile/server/internal/protocol"
)

// SnapshotCache holds the latest OrchestrationReadModel and its pre-serialized
// JSON form so downstream clients can be served without repeated marshaling.
type SnapshotCache struct {
	snapshot *protocol.OrchestrationReadModel
	raw      json.RawMessage
	mu       sync.RWMutex
}

// NewSnapshotCache creates an empty snapshot cache.
func NewSnapshotCache() *SnapshotCache {
	return &SnapshotCache{}
}

// Set stores a new snapshot and pre-serializes it to JSON.
func (sc *SnapshotCache) Set(snapshot *protocol.OrchestrationReadModel) error {
	raw, err := json.Marshal(snapshot)
	if err != nil {
		return err
	}

	sc.mu.Lock()
	sc.snapshot = snapshot
	sc.raw = raw
	sc.mu.Unlock()

	return nil
}

// Get returns the current snapshot, or nil if none has been loaded.
func (sc *SnapshotCache) Get() *protocol.OrchestrationReadModel {
	sc.mu.RLock()
	defer sc.mu.RUnlock()
	return sc.snapshot
}

// GetRaw returns the pre-serialized JSON of the current snapshot.
// Returns nil if no snapshot has been loaded.
func (sc *SnapshotCache) GetRaw() json.RawMessage {
	sc.mu.RLock()
	defer sc.mu.RUnlock()
	return sc.raw
}

// UpdateSequence atomically increments the snapshot's SnapshotSequence
// and re-serializes the cached JSON.
func (sc *SnapshotCache) UpdateSequence(seq int) {
	sc.mu.Lock()
	defer sc.mu.Unlock()

	if sc.snapshot == nil {
		return
	}

	sc.snapshot.SnapshotSequence = seq

	// Best-effort re-serialization; if it fails, keep stale raw.
	if raw, err := json.Marshal(sc.snapshot); err == nil {
		sc.raw = raw
	}
}
