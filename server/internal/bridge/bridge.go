package bridge

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/rs/zerolog"

	"github.com/BurgessTG/J3-Mobile/server/internal/protocol"
	"github.com/BurgessTG/J3-Mobile/server/internal/upstream"
)

// Bridge connects a single upstream T3 Code server to many downstream
// mobile clients. It caches the orchestration snapshot and fans out
// push events.
type Bridge struct {
	Upstream  *upstream.Client
	Sessions  *SessionManager
	Snapshot  *SnapshotCache
	Logger    zerolog.Logger
	StartedAt time.Time
}

// New creates a Bridge with a fresh SessionManager and SnapshotCache.
func New(upstreamClient *upstream.Client, logger zerolog.Logger) *Bridge {
	return &Bridge{
		Upstream:  upstreamClient,
		Sessions:  NewSessionManager(),
		Snapshot:  NewSnapshotCache(),
		Logger:    logger.With().Str("component", "bridge").Logger(),
		StartedAt: time.Now().UTC(),
	}
}

// Start wires up the push handler on the upstream client, connects to
// upstream, and loads the initial orchestration snapshot. It blocks until
// the initial setup is complete or the context is cancelled.
func (b *Bridge) Start(ctx context.Context) error {
	b.Upstream.SetPushHandler(b.HandlePush)

	if err := b.Upstream.Connect(ctx); err != nil {
		return fmt.Errorf("upstream connect: %w", err)
	}

	if err := b.LoadSnapshot(ctx); err != nil {
		b.Logger.Warn().Err(err).Msg("failed to load initial snapshot (continuing anyway)")
	}

	return nil
}

// HandlePush is invoked by the upstream client for every push message.
// It serializes the push to JSON, broadcasts it to all downstream mobile
// sessions, and updates the snapshot cache when domain events arrive.
func (b *Bridge) HandlePush(push *protocol.WsPush) {
	data, err := json.Marshal(push)
	if err != nil {
		b.Logger.Error().Err(err).Str("channel", push.Channel).Msg("failed to marshal push")
		return
	}

	b.Sessions.Broadcast(data)

	if push.Channel == protocol.ChannelOrchestrationDomainEvent {
		b.Snapshot.UpdateSequence(push.Sequence)
	}
}

// ForwardRequest takes a raw JSON request from a mobile client, forwards
// it upstream, and returns the raw JSON response.
func (b *Bridge) ForwardRequest(ctx context.Context, msg []byte) ([]byte, error) {
	return b.Upstream.ForwardRequest(ctx, msg)
}

// LoadSnapshot sends an "orchestration.getSnapshot" RPC to upstream,
// unmarshals the result, and stores it in the snapshot cache.
func (b *Bridge) LoadSnapshot(ctx context.Context) error {
	result, err := b.Upstream.SendRequest(ctx, protocol.MethodOrchestrationGetSnapshot, nil)
	if err != nil {
		return fmt.Errorf("getSnapshot RPC: %w", err)
	}

	var snapshot protocol.OrchestrationReadModel
	if err := json.Unmarshal(result, &snapshot); err != nil {
		return fmt.Errorf("unmarshal snapshot: %w", err)
	}

	if err := b.Snapshot.Set(&snapshot); err != nil {
		return fmt.Errorf("cache snapshot: %w", err)
	}

	b.Logger.Info().
		Int("sequence", snapshot.SnapshotSequence).
		Int("projects", len(snapshot.Projects)).
		Int("threads", len(snapshot.Threads)).
		Msg("snapshot loaded")

	return nil
}

func (b *Bridge) SnapshotAvailable() bool {
	return b.Snapshot.GetRaw() != nil
}

func (b *Bridge) Ready() bool {
	return b.Upstream.IsConnected() && b.SnapshotAvailable()
}
