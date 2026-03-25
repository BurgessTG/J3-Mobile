package protocol

import "encoding/json"

// EventBase holds fields common to all domain events.
type EventBase struct {
	Sequence         int           `json:"sequence"`
	EventId          EventId       `json:"eventId"`
	Type             string        `json:"type"`
	AggregateKind    string        `json:"aggregateKind"`
	AggregateId      string        `json:"aggregateId"`
	OccurredAt       string        `json:"occurredAt"`
	CommandId        *CommandId    `json:"commandId,omitempty"`
	CausationEventId *EventId      `json:"causationEventId,omitempty"`
	CorrelationId    *CommandId    `json:"correlationId,omitempty"`
	Metadata         EventMetadata `json:"metadata"`
}

// EventMetadata carries optional provider-level tracing information.
type EventMetadata struct {
	ProviderTurnId *string            `json:"providerTurnId,omitempty"`
	ProviderItemId *ProviderItemId    `json:"providerItemId,omitempty"`
	AdapterKey     *string            `json:"adapterKey,omitempty"`
	RequestId      *ApprovalRequestId `json:"requestId,omitempty"`
	IngestedAt     *string            `json:"ingestedAt,omitempty"`
}

// OrchestrationEvent is a domain event with its payload kept as raw JSON.
// The bridge passes events through without deserializing every payload.
type OrchestrationEvent struct {
	EventBase
	Payload json.RawMessage `json:"payload"`
}

// ThreadMessageSentPayload is the payload for thread message-sent events.
// The bridge inspects this to relay message content.
type ThreadMessageSentPayload struct {
	ThreadId  ThreadId  `json:"threadId"`
	MessageId MessageId `json:"messageId"`
	Role      string    `json:"role"`
	Text      string    `json:"text"`
	TurnId    TurnId    `json:"turnId"`
	Streaming bool      `json:"streaming"`
}

// ThreadSessionSetPayload is the payload for thread session-set events.
// The bridge inspects this to track session state.
type ThreadSessionSetPayload struct {
	ThreadId     ThreadId `json:"threadId"`
	Status       string   `json:"status"`
	ProviderName string   `json:"providerName"`
	RuntimeMode  string   `json:"runtimeMode"`
	ActiveTurnId TurnId   `json:"activeTurnId"`
	LastError    string   `json:"lastError"`
}
