package downstream

import (
	"context"
	"net/http"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/websocket"

	"github.com/BurgessTG/J3-Mobile/server/internal/auth"
	"github.com/BurgessTG/J3-Mobile/server/internal/bridge"
)

var upgrader = websocket.Upgrader{
	CheckOrigin:     func(r *http.Request) bool { return true },
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

// wsHandler upgrades an HTTP connection to a WebSocket and manages the
// lifecycle of a downstream mobile session.
func wsHandler(b *bridge.Bridge) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			b.Logger.Error().Err(err).Msg("websocket upgrade failed")
			return
		}

		userID := auth.UserIDFromContext(r.Context())

		session := &bridge.Session{
			ID:        uuid.New().String(),
			UserID:    userID,
			Conn:      conn,
			SendChan:  make(chan []byte, 256),
			Done:      make(chan struct{}),
			CreatedAt: time.Now(),
		}

		b.Sessions.Add(session)
		b.Logger.Info().
			Str("sessionId", session.ID).
			Str("userId", userID).
			Msg("mobile client connected")

		// writePump: reads from SendChan and writes to the WebSocket.
		go writePump(session, b)

		// readPump: reads from the WebSocket, forwards upstream, writes response back.
		readPump(session, b)
	}
}

// writePump drains the session's SendChan and writes each message to the
// WebSocket connection. It exits when the Done channel is closed.
func writePump(session *bridge.Session, b *bridge.Bridge) {
	defer session.Conn.Close()

	for {
		select {
		case msg, ok := <-session.SendChan:
			if !ok {
				// Channel closed.
				session.Conn.WriteMessage(websocket.CloseMessage, nil)
				return
			}
			if err := session.Conn.WriteMessage(websocket.TextMessage, msg); err != nil {
				b.Logger.Warn().Err(err).
					Str("sessionId", session.ID).
					Msg("write to mobile client failed")
				return
			}
		case <-session.Done:
			return
		}
	}
}

// readPump reads messages from the mobile client, forwards each one
// upstream via the bridge, and writes the response back. On disconnect
// it cleans up the session.
func readPump(session *bridge.Session, b *bridge.Bridge) {
	defer func() {
		close(session.Done)
		b.Sessions.Remove(session.ID)
		session.Conn.Close()
		b.Logger.Info().
			Str("sessionId", session.ID).
			Msg("mobile client disconnected")
	}()

	for {
		_, msg, err := session.Conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseNormalClosure) {
				b.Logger.Warn().Err(err).
					Str("sessionId", session.ID).
					Msg("unexpected websocket close")
			}
			return
		}

		resp, err := b.ForwardRequest(context.Background(), msg)
		if err != nil {
			b.Logger.Error().Err(err).
				Str("sessionId", session.ID).
				Msg("forward request failed")
			continue
		}

		if err := session.Conn.WriteMessage(websocket.TextMessage, resp); err != nil {
			b.Logger.Warn().Err(err).
				Str("sessionId", session.ID).
				Msg("write response to mobile client failed")
			return
		}
	}
}
