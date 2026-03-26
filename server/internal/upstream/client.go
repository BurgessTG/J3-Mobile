package upstream

import (
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"sync"
	"sync/atomic"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"

	"github.com/BurgessTG/J3-Mobile/server/internal/protocol"
)

// PushHandler is called for each push message received from upstream.
type PushHandler func(push *protocol.WsPush)

// Client maintains a WebSocket connection to the T3 Code upstream server.
type Client struct {
	url         string
	authToken   string
	conn        *websocket.Conn
	connMu      sync.RWMutex
	tracker     *RequestTracker
	reconnState *ReconnectState
	pushHandler PushHandler
	logger      zerolog.Logger
	connected   atomic.Bool
	done        chan struct{}
	ctx         context.Context
	cancel      context.CancelFunc
	requestSeq  atomic.Int64
	lastErrorMu sync.RWMutex
	lastError   string
}

const (
	readWait  = 45 * time.Second
	writeWait = 10 * time.Second
	pingWait  = 20 * time.Second
)

// NewClient creates an upstream client targeting the given WebSocket URL.
func NewClient(upstreamURL string, authToken string, logger zerolog.Logger) *Client {
	ctx, cancel := context.WithCancel(context.Background())
	return &Client{
		url:         upstreamURL,
		authToken:   authToken,
		tracker:     NewRequestTracker(),
		reconnState: NewReconnectState(),
		logger:      logger.With().Str("component", "upstream").Logger(),
		done:        make(chan struct{}),
		ctx:         ctx,
		cancel:      cancel,
	}
}

// Connect dials the T3 Code WebSocket server. It blocks until the connection
// is established or the context is cancelled.
func (c *Client) Connect(ctx context.Context) error {
	if err := c.dial(ctx); err != nil {
		c.setLastError(err)
		return err
	}

	c.connected.Store(true)
	c.reconnState.Reset()
	c.clearLastError()

	conn := c.connection()
	go c.readPump(conn)
	go c.pingLoop(conn)
	go c.reconnectLoop()

	c.logger.Info().Str("url", sanitizeURL(c.url)).Msg("connected to upstream")
	return nil
}

// dial performs the actual WebSocket handshake.
func (c *Client) dial(ctx context.Context) error {
	u, err := url.Parse(c.url)
	if err != nil {
		return fmt.Errorf("invalid upstream URL: %w", err)
	}

	q := u.Query()
	if c.authToken != "" {
		q.Set("token", c.authToken)
	}
	u.RawQuery = q.Encode()

	dialer := websocket.DefaultDialer
	conn, _, err := dialer.DialContext(ctx, u.String(), nil)
	if err != nil {
		return fmt.Errorf("upstream dial failed: %w", err)
	}
	configureConn(conn)

	c.connMu.Lock()
	c.conn = conn
	c.connMu.Unlock()

	return nil
}

// SetPushHandler sets the callback for incoming push messages.
func (c *Client) SetPushHandler(handler PushHandler) {
	c.pushHandler = handler
}

// SendRequest sends an RPC request upstream and waits for the response.
// method is the _tag value (e.g. "orchestration.getSnapshot").
// params is marshaled into the body alongside _tag.
func (c *Client) SendRequest(ctx context.Context, method string, params json.RawMessage) (json.RawMessage, error) {
	id := fmt.Sprintf("m-%d-%s", c.requestSeq.Add(1), uuid.New().String()[:8])

	// Build the body: merge _tag with any extra params.
	body := map[string]json.RawMessage{
		"_tag": mustMarshal(method),
	}
	if len(params) > 0 {
		// Merge params keys into body.
		var extra map[string]json.RawMessage
		if err := json.Unmarshal(params, &extra); err == nil {
			for k, v := range extra {
				body[k] = v
			}
		}
	}

	bodyJSON, err := json.Marshal(body)
	if err != nil {
		return nil, fmt.Errorf("marshal body: %w", err)
	}

	envelope := protocol.WsRequest{
		ID:   id,
		Body: bodyJSON,
	}

	msg, err := json.Marshal(envelope)
	if err != nil {
		return nil, fmt.Errorf("marshal request: %w", err)
	}

	// Start tracking before sending so we never miss a fast reply.
	ch := c.tracker.Track(id)

	if c.connection() == nil {
		c.tracker.Resolve(id, &rpcResponse{Error: &protocol.WsError{Message: "not connected"}})
		return nil, fmt.Errorf("upstream not connected")
	}

	if err := c.writeMessage(msg); err != nil {
		c.tracker.Resolve(id, &rpcResponse{Error: &protocol.WsError{Message: err.Error()}})
		return nil, fmt.Errorf("write upstream: %w", err)
	}

	// Wait for response or context cancellation.
	select {
	case resp := <-ch:
		if resp.Error != nil {
			return nil, fmt.Errorf("upstream error: %s", resp.Error.Message)
		}
		return resp.Result, nil
	case <-ctx.Done():
		return nil, ctx.Err()
	}
}

// ForwardRaw sends a raw WebSocket message upstream (for pass-through from mobile).
func (c *Client) ForwardRaw(ctx context.Context, msg []byte) error {
	if c.connection() == nil {
		return fmt.Errorf("upstream not connected")
	}

	return c.writeMessage(msg)
}

// ForwardRequest forwards a raw request from a mobile client upstream,
// tracking it by request ID, and returns the serialized response.
func (c *Client) ForwardRequest(ctx context.Context, msg []byte) ([]byte, error) {
	var req protocol.WsRequest
	if err := json.Unmarshal(msg, &req); err != nil {
		return nil, fmt.Errorf("invalid request envelope: %w", err)
	}

	ch := c.tracker.Track(req.ID)

	if c.connection() == nil {
		response := c.errorResponse(req.ID, "upstream not connected")
		c.tracker.Resolve(req.ID, &rpcResponse{Error: &protocol.WsError{Message: "not connected"}})
		return response, fmt.Errorf("upstream not connected")
	}

	if err := c.writeMessage(msg); err != nil {
		response := c.errorResponse(req.ID, err.Error())
		c.tracker.Resolve(req.ID, &rpcResponse{Error: &protocol.WsError{Message: err.Error()}})
		return response, fmt.Errorf("write upstream: %w", err)
	}

	select {
	case resp := <-ch:
		wsResp := protocol.WsResponse{
			ID:     req.ID,
			Result: resp.Result,
			Error:  resp.Error,
		}
		return json.Marshal(wsResp)
	case <-ctx.Done():
		return c.errorResponse(req.ID, ctx.Err().Error()), ctx.Err()
	}
}

// IsConnected returns true if the upstream connection is alive.
func (c *Client) IsConnected() bool {
	return c.connected.Load()
}

// Close cleanly shuts down the connection.
func (c *Client) Close() error {
	c.cancel()

	c.connMu.Lock()
	conn := c.conn
	c.conn = nil
	c.connMu.Unlock()

	c.connected.Store(false)
	c.clearLastError()
	c.tracker.RejectAll(fmt.Errorf("client closed"))

	if conn != nil {
		return conn.Close()
	}
	return nil
}

func (c *Client) LastError() string {
	c.lastErrorMu.RLock()
	defer c.lastErrorMu.RUnlock()
	return c.lastError
}

// readPump reads messages from upstream and dispatches them as responses or pushes.
func (c *Client) readPump(conn *websocket.Conn) {
	defer func() {
		c.connected.Store(false)
		c.tracker.RejectAll(fmt.Errorf("upstream connection lost"))

		// Signal reconnect loop.
		select {
		case c.done <- struct{}{}:
		default:
		}
	}()

	for {
		if conn == nil {
			return
		}

		_, msg, err := conn.ReadMessage()
		if err != nil {
			if c.ctx.Err() != nil {
				// Shutting down; don't log as error.
				return
			}
			c.setLastError(err)
			c.logger.Warn().Err(err).Msg("upstream read error")
			return
		}

		// Peek at the message to classify it.
		var peek struct {
			ID   string `json:"id,omitempty"`
			Type string `json:"type,omitempty"`
		}
		if err := json.Unmarshal(msg, &peek); err != nil {
			c.logger.Warn().Err(err).Msg("failed to parse upstream message")
			continue
		}

		if peek.ID != "" {
			// This is a response to a tracked request.
			var resp protocol.WsResponse
			if err := json.Unmarshal(msg, &resp); err != nil {
				c.logger.Warn().Err(err).Msg("failed to parse upstream response")
				continue
			}
			c.clearLastError()
			c.tracker.Resolve(resp.ID, &rpcResponse{
				Result: resp.Result,
				Error:  resp.Error,
			})
		} else if peek.Type == "push" {
			// This is a push notification.
			var push protocol.WsPush
			if err := json.Unmarshal(msg, &push); err != nil {
				c.logger.Warn().Err(err).Msg("failed to parse upstream push")
				continue
			}
			c.clearLastError()
			c.reconnState.SetLastSequence(push.Sequence)

			if c.pushHandler != nil {
				c.pushHandler(&push)
			}
		} else {
			c.logger.Debug().RawJSON("msg", msg).Msg("unclassified upstream message")
		}
	}
}

// reconnectLoop runs in the background, attempting to reconnect when the
// connection drops. It exits when the client context is cancelled.
func (c *Client) reconnectLoop() {
	for {
		select {
		case <-c.ctx.Done():
			return
		case <-c.done:
			// Connection dropped — attempt reconnect.
		}

		c.logger.Info().Msg("upstream connection lost, starting reconnect")

		for {
			if c.ctx.Err() != nil {
				return
			}

			delay := c.reconnState.NextDelay()
			c.logger.Info().
				Dur("delay", delay).
				Int("lastSeq", c.reconnState.LastSequence()).
				Msg("reconnecting to upstream")

			select {
			case <-time.After(delay):
			case <-c.ctx.Done():
				return
			}

			if err := c.dial(c.ctx); err != nil {
				c.setLastError(err)
				c.logger.Warn().Err(err).Msg("reconnect attempt failed")
				continue
			}

			c.connected.Store(true)
			c.reconnState.Reset()
			c.clearLastError()
			c.logger.Info().Msg("reconnected to upstream")

			// Fire a replay request to catch up on missed events.
			c.requestReplay()

			conn := c.connection()
			go c.readPump(conn)
			go c.pingLoop(conn)
			break
		}
	}
}

// requestReplay asks upstream for events missed during the disconnect.
func (c *Client) requestReplay() {
	lastSeq := c.reconnState.LastSequence()
	if lastSeq == 0 {
		return
	}

	params, _ := json.Marshal(map[string]int{
		"fromSequence": lastSeq,
	})

	ctx, cancel := context.WithTimeout(c.ctx, 10*time.Second)
	defer cancel()

	_, err := c.SendRequest(ctx, protocol.MethodOrchestrationReplayEvents, params)
	if err != nil {
		c.logger.Warn().Err(err).Int("fromSequence", lastSeq).Msg("replay request failed")
	}
}

// mustMarshal marshals v to JSON, panicking on error (only for compile-time-safe values).
func mustMarshal(v any) json.RawMessage {
	b, err := json.Marshal(v)
	if err != nil {
		panic(fmt.Sprintf("mustMarshal: %v", err))
	}
	return b
}

func (c *Client) errorResponse(id, message string) []byte {
	resp, err := json.Marshal(protocol.WsResponse{
		ID: id,
		Error: &protocol.WsError{
			Message: message,
		},
	})
	if err != nil {
		return nil
	}
	return resp
}

func (c *Client) connection() *websocket.Conn {
	c.connMu.RLock()
	defer c.connMu.RUnlock()
	return c.conn
}

func (c *Client) writeMessage(msg []byte) error {
	c.connMu.Lock()
	defer c.connMu.Unlock()

	if c.conn == nil {
		return fmt.Errorf("upstream not connected")
	}

	if err := c.conn.SetWriteDeadline(time.Now().Add(writeWait)); err != nil {
		return err
	}

	return c.conn.WriteMessage(websocket.TextMessage, msg)
}

func (c *Client) pingLoop(conn *websocket.Conn) {
	ticker := time.NewTicker(pingWait)
	defer ticker.Stop()

	for {
		select {
		case <-c.ctx.Done():
			return
		case <-ticker.C:
			if conn == nil {
				return
			}
			if err := conn.WriteControl(websocket.PingMessage, nil, time.Now().Add(writeWait)); err != nil {
				c.setLastError(err)
				c.logger.Warn().Err(err).Msg("upstream ping failed")
				_ = conn.Close()
				return
			}
		}
	}
}

func configureConn(conn *websocket.Conn) {
	_ = conn.SetReadDeadline(time.Now().Add(readWait))
	conn.SetPongHandler(func(string) error {
		return conn.SetReadDeadline(time.Now().Add(readWait))
	})
}

func sanitizeURL(raw string) string {
	parsed, err := url.Parse(raw)
	if err != nil {
		return raw
	}
	parsed.User = nil
	query := parsed.Query()
	for _, key := range []string{"token", "access_token", "auth", "authorization"} {
		if query.Has(key) {
			query.Set(key, "REDACTED")
		}
	}
	parsed.RawQuery = query.Encode()
	return parsed.String()
}

func (c *Client) setLastError(err error) {
	if err == nil {
		return
	}
	c.lastErrorMu.Lock()
	c.lastError = err.Error()
	c.lastErrorMu.Unlock()
}

func (c *Client) clearLastError() {
	c.lastErrorMu.Lock()
	c.lastError = ""
	c.lastErrorMu.Unlock()
}
