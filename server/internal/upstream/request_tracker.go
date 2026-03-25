package upstream

import (
	"encoding/json"
	"sync"

	"github.com/BurgessTG/J3-Mobile/server/internal/protocol"
)

type pendingRequest struct {
	ch chan *rpcResponse
}

type rpcResponse struct {
	Result json.RawMessage
	Error  *protocol.WsError
}

// RequestTracker maps in-flight RPC request IDs to channels awaiting responses.
type RequestTracker struct {
	pending map[string]*pendingRequest
	mu      sync.Mutex
}

// NewRequestTracker creates an empty tracker.
func NewRequestTracker() *RequestTracker {
	return &RequestTracker{
		pending: make(map[string]*pendingRequest),
	}
}

// Track registers a request ID and returns a channel that will receive the response.
func (rt *RequestTracker) Track(id string) <-chan *rpcResponse {
	ch := make(chan *rpcResponse, 1)
	rt.mu.Lock()
	rt.pending[id] = &pendingRequest{ch: ch}
	rt.mu.Unlock()
	return ch
}

// Resolve delivers a response for the given request ID and removes it from tracking.
// If the ID is not found (e.g. already resolved), the call is a no-op.
func (rt *RequestTracker) Resolve(id string, resp *rpcResponse) {
	rt.mu.Lock()
	pr, ok := rt.pending[id]
	if ok {
		delete(rt.pending, id)
	}
	rt.mu.Unlock()

	if ok {
		pr.ch <- resp
	}
}

// RejectAll sends an error response to every pending request and clears the map.
func (rt *RequestTracker) RejectAll(err error) {
	rt.mu.Lock()
	old := rt.pending
	rt.pending = make(map[string]*pendingRequest)
	rt.mu.Unlock()

	errResp := &rpcResponse{
		Error: &protocol.WsError{Message: err.Error()},
	}
	for _, pr := range old {
		pr.ch <- errResp
	}
}

// Count returns the number of in-flight requests.
func (rt *RequestTracker) Count() int {
	rt.mu.Lock()
	defer rt.mu.Unlock()
	return len(rt.pending)
}
