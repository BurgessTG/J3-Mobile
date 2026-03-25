package upstream

import (
	"encoding/json"
	"fmt"
	"testing"
	"time"

	"github.com/BurgessTG/J3-Mobile/server/internal/protocol"
)

func TestTrackAndResolve(t *testing.T) {
	rt := NewRequestTracker()

	ch := rt.Track("req-1")
	if rt.Count() != 1 {
		t.Fatalf("expected count 1, got %d", rt.Count())
	}

	result := json.RawMessage(`{"ok":true}`)
	go rt.Resolve("req-1", &rpcResponse{Result: result})

	select {
	case resp := <-ch:
		if resp.Error != nil {
			t.Fatalf("unexpected error: %s", resp.Error.Message)
		}
		if string(resp.Result) != `{"ok":true}` {
			t.Fatalf("unexpected result: %s", string(resp.Result))
		}
	case <-time.After(time.Second):
		t.Fatal("timed out waiting for response")
	}

	if rt.Count() != 0 {
		t.Fatalf("expected count 0 after resolve, got %d", rt.Count())
	}
}

func TestResolveWithError(t *testing.T) {
	rt := NewRequestTracker()

	ch := rt.Track("req-err")
	go rt.Resolve("req-err", &rpcResponse{
		Error: &protocol.WsError{Message: "something failed"},
	})

	select {
	case resp := <-ch:
		if resp.Error == nil {
			t.Fatal("expected error, got nil")
		}
		if resp.Error.Message != "something failed" {
			t.Fatalf("unexpected error message: %s", resp.Error.Message)
		}
	case <-time.After(time.Second):
		t.Fatal("timed out waiting for error response")
	}
}

func TestRejectAll(t *testing.T) {
	rt := NewRequestTracker()

	channels := make([]<-chan *rpcResponse, 5)
	for i := range 5 {
		channels[i] = rt.Track(fmt.Sprintf("req-%d", i))
	}
	if rt.Count() != 5 {
		t.Fatalf("expected count 5, got %d", rt.Count())
	}

	rt.RejectAll(fmt.Errorf("connection lost"))

	if rt.Count() != 0 {
		t.Fatalf("expected count 0 after reject all, got %d", rt.Count())
	}

	for i, ch := range channels {
		select {
		case resp := <-ch:
			if resp.Error == nil {
				t.Fatalf("channel %d: expected error, got nil", i)
			}
			if resp.Error.Message != "connection lost" {
				t.Fatalf("channel %d: unexpected error: %s", i, resp.Error.Message)
			}
		case <-time.After(time.Second):
			t.Fatalf("channel %d: timed out waiting for reject", i)
		}
	}
}

func TestDoubleResolveIsSafe(t *testing.T) {
	rt := NewRequestTracker()

	ch := rt.Track("req-double")
	rt.Resolve("req-double", &rpcResponse{Result: json.RawMessage(`"first"`)})

	// Second resolve should be a no-op (ID already removed), not panic.
	rt.Resolve("req-double", &rpcResponse{Result: json.RawMessage(`"second"`)})

	select {
	case resp := <-ch:
		if string(resp.Result) != `"first"` {
			t.Fatalf("expected first response, got %s", string(resp.Result))
		}
	case <-time.After(time.Second):
		t.Fatal("timed out waiting for response")
	}
}

func TestResolveUnknownIDIsSafe(t *testing.T) {
	rt := NewRequestTracker()

	// Resolving an ID that was never tracked should not panic.
	rt.Resolve("nonexistent", &rpcResponse{Result: json.RawMessage(`"nope"`)})
}

func TestCountTracksPending(t *testing.T) {
	rt := NewRequestTracker()

	if rt.Count() != 0 {
		t.Fatalf("expected 0, got %d", rt.Count())
	}

	rt.Track("a")
	rt.Track("b")
	rt.Track("c")
	if rt.Count() != 3 {
		t.Fatalf("expected 3, got %d", rt.Count())
	}

	rt.Resolve("b", &rpcResponse{})
	if rt.Count() != 2 {
		t.Fatalf("expected 2, got %d", rt.Count())
	}

	rt.RejectAll(fmt.Errorf("done"))
	if rt.Count() != 0 {
		t.Fatalf("expected 0 after reject all, got %d", rt.Count())
	}
}
