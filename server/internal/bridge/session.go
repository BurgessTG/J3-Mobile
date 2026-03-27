package bridge

import (
	"sync"
	"sync/atomic"
	"time"

	"github.com/gorilla/websocket"
)

// Session represents a single connected mobile client for one personal bridge.
type Session struct {
	ID           string
	CredentialID string
	DeviceName   string
	RemoteAddr   string
	Conn         *websocket.Conn
	SendChan     chan []byte
	Done         chan struct{}
	CreatedAt    time.Time
	lastSeenUnix atomic.Int64
}

func (s *Session) Touch(t time.Time) {
	s.lastSeenUnix.Store(t.UTC().UnixNano())
}

func (s *Session) LastSeenAt() time.Time {
	unixNano := s.lastSeenUnix.Load()
	if unixNano == 0 {
		return s.CreatedAt.UTC()
	}
	return time.Unix(0, unixNano).UTC()
}

// SessionInfo is a JSON-safe summary of a session for listing endpoints.
type SessionInfo struct {
	SessionID    string    `json:"sessionId"`
	DeviceName   string    `json:"deviceName"`
	CreatedAt    time.Time `json:"createdAt"`
	LastSeenAt   time.Time `json:"lastSeenAt"`
	RemoteAddr   string    `json:"remoteAddr"`
	State        string    `json:"state"`
	CredentialID string    `json:"-"`
}

// SessionManager tracks all active downstream mobile sessions.
type SessionManager struct {
	sessions map[string]*Session
	mu       sync.RWMutex
}

// NewSessionManager creates an empty session manager.
func NewSessionManager() *SessionManager {
	return &SessionManager{
		sessions: make(map[string]*Session),
	}
}

// Add registers a session.
func (sm *SessionManager) Add(session *Session) {
	sm.mu.Lock()
	sm.sessions[session.ID] = session
	sm.mu.Unlock()
}

// Remove deregisters a session by ID.
func (sm *SessionManager) Remove(id string) {
	sm.mu.Lock()
	delete(sm.sessions, id)
	sm.mu.Unlock()
}

// Broadcast sends a message to all connected sessions via their SendChan.
// Slow or blocked clients are skipped to avoid stalling the bridge.
func (sm *SessionManager) Broadcast(msg []byte) {
	sm.mu.RLock()
	defer sm.mu.RUnlock()

	for _, session := range sm.sessions {
		select {
		case session.SendChan <- msg:
		default:
		}
	}
}

// ListByCredential returns active sessions for the same credential only.
func (sm *SessionManager) ListByCredential(credentialID string) []SessionInfo {
	sm.mu.RLock()
	defer sm.mu.RUnlock()

	infos := make([]SessionInfo, 0, len(sm.sessions))
	for _, session := range sm.sessions {
		if session.CredentialID != credentialID {
			continue
		}
		infos = append(infos, SessionInfo{
			SessionID:    session.ID,
			DeviceName:   session.DeviceName,
			CreatedAt:    session.CreatedAt.UTC(),
			LastSeenAt:   session.LastSeenAt(),
			RemoteAddr:   session.RemoteAddr,
			State:        "connected",
			CredentialID: session.CredentialID,
		})
	}
	return infos
}

// Count returns the number of active sessions.
func (sm *SessionManager) Count() int {
	sm.mu.RLock()
	defer sm.mu.RUnlock()
	return len(sm.sessions)
}
