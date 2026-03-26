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

const (
	downstreamReadWait  = 45 * time.Second
	downstreamWriteWait = 10 * time.Second
	downstreamPingWait  = 20 * time.Second
)

// wsHandler upgrades an HTTP connection to a WebSocket and manages the
// lifecycle of a downstream mobile session.
func wsHandler(b *bridge.Bridge, allowedOrigin string) http.HandlerFunc {
	upgrader := websocket.Upgrader{
		CheckOrigin: func(r *http.Request) bool {
			origin := r.Header.Get("Origin")
			if origin == "" {
				return true
			}
			return allowedOrigin != "" && origin == allowedOrigin
		},
		ReadBufferSize:  1024,
		WriteBufferSize: 1024,
	}

	return func(w http.ResponseWriter, r *http.Request) {
		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			b.Logger.Error().Err(err).Str("reason", "auth_failed").Msg("websocket upgrade failed")
			return
		}

		configureDownstreamConn(conn)

		session := &bridge.Session{
			ID:           uuid.New().String(),
			CredentialID: auth.CredentialIDFromContext(r.Context()),
			DeviceName:   auth.DeviceNameFromContext(r.Context()),
			RemoteAddr:   r.RemoteAddr,
			Conn:         conn,
			SendChan:     make(chan []byte, 256),
			Done:         make(chan struct{}),
			CreatedAt:    time.Now().UTC(),
		}
		session.Touch(session.CreatedAt)

		b.Sessions.Add(session)
		b.Logger.Info().
			Str("sessionId", session.ID).
			Str("credentialId", session.CredentialID).
			Str("deviceName", session.DeviceName).
			Msg("mobile client connected")

		go writePump(session, b)
		readPump(session, b)
	}
}

// writePump drains the session's SendChan and writes each message to the
// WebSocket connection. It exits when the Done channel is closed.
func writePump(session *bridge.Session, b *bridge.Bridge) {
	ticker := time.NewTicker(downstreamPingWait)
	defer ticker.Stop()
	defer session.Conn.Close()

	for {
		select {
		case msg, ok := <-session.SendChan:
			if !ok {
				_ = session.Conn.WriteControl(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, ""), time.Now().Add(downstreamWriteWait))
				return
			}

			if err := session.Conn.SetWriteDeadline(time.Now().Add(downstreamWriteWait)); err != nil {
				b.Logger.Warn().Err(err).Str("sessionId", session.ID).Str("reason", "ws_protocol_error").Msg("set write deadline failed")
				return
			}
			if err := session.Conn.WriteMessage(websocket.TextMessage, msg); err != nil {
				b.Logger.Warn().Err(err).
					Str("sessionId", session.ID).
					Str("reason", "ws_protocol_error").
					Msg("write to mobile client failed")
				return
			}
			session.Touch(time.Now().UTC())
		case <-ticker.C:
			if err := session.Conn.WriteControl(websocket.PingMessage, nil, time.Now().Add(downstreamWriteWait)); err != nil {
				b.Logger.Warn().Err(err).
					Str("sessionId", session.ID).
					Str("reason", "ws_protocol_error").
					Msg("ping mobile client failed")
				return
			}
		case <-session.Done:
			return
		}
	}
}

// readPump reads messages from the mobile client, forwards each one
// upstream via the bridge, and enqueues the response for writePump. On
// disconnect it cleans up the session.
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
					Str("reason", "ws_protocol_error").
					Msg("unexpected websocket close")
			}
			return
		}
		session.Touch(time.Now().UTC())

		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		resp, err := b.ForwardRequest(ctx, msg)
		cancel()
		if err != nil {
			b.Logger.Error().Err(err).
				Str("sessionId", session.ID).
				Str("reason", "bridge_unreachable").
				Msg("forward request failed")
			if len(resp) == 0 {
				continue
			}
		}

		select {
		case session.SendChan <- resp:
		default:
			b.Logger.Warn().
				Str("sessionId", session.ID).
				Str("reason", "ws_protocol_error").
				Msg("mobile client send buffer full")
			return
		}
	}
}

func configureDownstreamConn(conn *websocket.Conn) {
	_ = conn.SetReadDeadline(time.Now().Add(downstreamReadWait))
	conn.SetPongHandler(func(string) error {
		return conn.SetReadDeadline(time.Now().Add(downstreamReadWait))
	})
}
