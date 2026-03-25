package upstream

import (
	"sync"
	"testing"
	"time"
)

func TestNextDelayIncreasing(t *testing.T) {
	rs := NewReconnectState()

	expected := []time.Duration{
		500 * time.Millisecond,
		1 * time.Second,
		2 * time.Second,
		4 * time.Second,
		8 * time.Second,
	}

	for i, want := range expected {
		got := rs.NextDelay()
		if got != want {
			t.Fatalf("attempt %d: expected %v, got %v", i, want, got)
		}
	}
}

func TestNextDelayCapsAtLast(t *testing.T) {
	rs := NewReconnectState()

	// Exhaust all delays.
	for range len(reconnectDelays) {
		rs.NextDelay()
	}

	last := reconnectDelays[len(reconnectDelays)-1]

	// Further calls should all return the last delay.
	for range 5 {
		got := rs.NextDelay()
		if got != last {
			t.Fatalf("expected capped delay %v, got %v", last, got)
		}
	}
}

func TestResetResetsToFirstDelay(t *testing.T) {
	rs := NewReconnectState()

	// Advance a few attempts.
	rs.NextDelay()
	rs.NextDelay()
	rs.NextDelay()

	rs.Reset()

	got := rs.NextDelay()
	want := reconnectDelays[0]
	if got != want {
		t.Fatalf("after reset: expected %v, got %v", want, got)
	}
}

func TestLastSequenceThreadSafe(t *testing.T) {
	rs := NewReconnectState()

	const goroutines = 100
	const iterations = 1000

	var wg sync.WaitGroup
	wg.Add(goroutines * 2)

	// Half the goroutines write.
	for range goroutines {
		go func() {
			defer wg.Done()
			for i := range iterations {
				rs.SetLastSequence(i)
			}
		}()
	}

	// Half the goroutines read.
	for range goroutines {
		go func() {
			defer wg.Done()
			for range iterations {
				seq := rs.LastSequence()
				_ = seq // Just verify no race.
			}
		}()
	}

	wg.Wait()

	// After all writers finish with iterations-1, the last sequence should be
	// somewhere between 0 and iterations-1 (non-deterministic due to concurrency).
	seq := rs.LastSequence()
	if seq < 0 || seq >= iterations {
		t.Fatalf("unexpected final sequence: %d", seq)
	}
}

func TestSetAndGetLastSequence(t *testing.T) {
	rs := NewReconnectState()

	if got := rs.LastSequence(); got != 0 {
		t.Fatalf("expected initial sequence 0, got %d", got)
	}

	rs.SetLastSequence(42)
	if got := rs.LastSequence(); got != 42 {
		t.Fatalf("expected 42, got %d", got)
	}

	rs.SetLastSequence(100)
	if got := rs.LastSequence(); got != 100 {
		t.Fatalf("expected 100, got %d", got)
	}
}
