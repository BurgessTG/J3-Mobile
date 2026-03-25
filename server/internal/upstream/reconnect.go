package upstream

import (
	"sync"
	"time"
)

var reconnectDelays = []time.Duration{
	500 * time.Millisecond,
	1 * time.Second,
	2 * time.Second,
	4 * time.Second,
	8 * time.Second,
}

// ReconnectState tracks the reconnection attempt counter and the last
// observed push sequence number so the client can resume where it left off.
type ReconnectState struct {
	attempt      int
	lastSequence int
	mu           sync.Mutex
}

// NewReconnectState creates a fresh reconnect state starting at attempt 0.
func NewReconnectState() *ReconnectState {
	return &ReconnectState{}
}

// NextDelay returns the delay for the current attempt and increments the
// attempt counter. The delay is capped at the last entry in reconnectDelays.
func (rs *ReconnectState) NextDelay() time.Duration {
	rs.mu.Lock()
	defer rs.mu.Unlock()

	idx := rs.attempt
	if idx >= len(reconnectDelays) {
		idx = len(reconnectDelays) - 1
	}
	delay := reconnectDelays[idx]
	rs.attempt++
	return delay
}

// Reset resets the attempt counter to 0 (called after a successful connect).
func (rs *ReconnectState) Reset() {
	rs.mu.Lock()
	rs.attempt = 0
	rs.mu.Unlock()
}

// SetLastSequence records the most recent push sequence number.
func (rs *ReconnectState) SetLastSequence(seq int) {
	rs.mu.Lock()
	rs.lastSequence = seq
	rs.mu.Unlock()
}

// LastSequence returns the most recent push sequence number.
func (rs *ReconnectState) LastSequence() int {
	rs.mu.Lock()
	defer rs.mu.Unlock()
	return rs.lastSequence
}
