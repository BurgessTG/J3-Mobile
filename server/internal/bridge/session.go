package bridge

import (
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

// Session represents a single connected mobile client.
type Session struct {
	ID        string
	UserID    string
	Conn      *websocket.Conn
	SendChan  chan []byte
	Done      chan struct{}
	CreatedAt time.Time
}

// SessionInfo is a JSON-safe summary of a session for listing endpoints.
type SessionInfo struct {
	ID        string    `json:"id"`
	UserID    string    `json:"userId"`
	CreatedAt time.Time `json:"createdAt"`
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
// Slow or blocked clients are skipped (non-blocking send).
func (sm *SessionManager) Broadcast(msg []byte) {
	sm.mu.RLock()
	defer sm.mu.RUnlock()

	for _, s := range sm.sessions {
		select {
		case s.SendChan <- msg:
		default:
			// Skip slow client to avoid blocking the broadcast.
		}
	}
}

// List returns a snapshot of all active session summaries.
func (sm *SessionManager) List() []SessionInfo {
	sm.mu.RLock()
	defer sm.mu.RUnlock()

	infos := make([]SessionInfo, 0, len(sm.sessions))
	for _, s := range sm.sessions {
		infos = append(infos, SessionInfo{
			ID:        s.ID,
			UserID:    s.UserID,
			CreatedAt: s.CreatedAt,
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
