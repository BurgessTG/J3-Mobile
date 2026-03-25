package protocol

import (
	"encoding/json"
	"testing"
)

func TestWsRequestMarshal(t *testing.T) {
	body := json.RawMessage(`{"_tag":"orchestration.getSnapshot","threadId":"t1"}`)
	req := WsRequest{
		ID:   "req-1",
		Body: body,
	}

	data, err := json.Marshal(req)
	if err != nil {
		t.Fatalf("marshal WsRequest: %v", err)
	}

	var parsed map[string]json.RawMessage
	if err := json.Unmarshal(data, &parsed); err != nil {
		t.Fatalf("unmarshal to map: %v", err)
	}

	// Verify "id" field
	var id string
	if err := json.Unmarshal(parsed["id"], &id); err != nil {
		t.Fatalf("unmarshal id: %v", err)
	}
	if id != "req-1" {
		t.Errorf("id = %q, want %q", id, "req-1")
	}

	// Verify "body" field contains _tag
	var bodyMap map[string]interface{}
	if err := json.Unmarshal(parsed["body"], &bodyMap); err != nil {
		t.Fatalf("unmarshal body: %v", err)
	}
	if tag, ok := bodyMap["_tag"].(string); !ok || tag != "orchestration.getSnapshot" {
		t.Errorf("body._tag = %v, want %q", bodyMap["_tag"], "orchestration.getSnapshot")
	}
}

func TestWsPushUnmarshal(t *testing.T) {
	input := `{
		"type": "push",
		"sequence": 42,
		"channel": "orchestration.domainEvent",
		"data": {"eventId": "evt-1", "type": "thread.message.sent"}
	}`

	var push WsPush
	if err := json.Unmarshal([]byte(input), &push); err != nil {
		t.Fatalf("unmarshal WsPush: %v", err)
	}

	if push.Type != "push" {
		t.Errorf("Type = %q, want %q", push.Type, "push")
	}
	if push.Sequence != 42 {
		t.Errorf("Sequence = %d, want %d", push.Sequence, 42)
	}
	if push.Channel != ChannelOrchestrationDomainEvent {
		t.Errorf("Channel = %q, want %q", push.Channel, ChannelOrchestrationDomainEvent)
	}
	if push.Data == nil {
		t.Fatal("Data is nil")
	}

	var dataMap map[string]string
	if err := json.Unmarshal(push.Data, &dataMap); err != nil {
		t.Fatalf("unmarshal Data: %v", err)
	}
	if dataMap["eventId"] != "evt-1" {
		t.Errorf("data.eventId = %q, want %q", dataMap["eventId"], "evt-1")
	}
}

func TestOrchestrationReadModelRoundTrip(t *testing.T) {
	thinking := true
	model := OrchestrationReadModel{
		SnapshotSequence: 7,
		Projects: []OrchestrationProject{
			{
				ID:            "proj-1",
				Title:         "My Project",
				WorkspaceRoot: "/home/user/code",
				DefaultModelSelection: &ModelSelection{
					Provider: ProviderClaudeAgent,
					Model:    "claude-opus-4-6",
					Options: &ModelOptions{
						Thinking: &thinking,
						Effort:   "high",
					},
				},
				Scripts:   []ProjectScript{},
				CreatedAt: "2026-01-01T00:00:00Z",
				UpdatedAt: "2026-01-02T00:00:00Z",
			},
		},
		Threads: []OrchestrationThread{
			{
				ID:        "thread-1",
				ProjectId: "proj-1",
				Title:     "Fix the bug",
				ModelSelection: ModelSelection{
					Provider: ProviderClaudeAgent,
					Model:    "claude-opus-4-6",
				},
				RuntimeMode:     "agent",
				InteractionMode: "autonomous",
				Messages:        []OrchestrationMessage{},
				ProposedPlans:   []OrchestrationProposedPlan{},
				Activities:      []OrchestrationThreadActivity{},
				Checkpoints:     []OrchestrationCheckpointSummary{},
				CreatedAt:       "2026-01-01T00:00:00Z",
				UpdatedAt:       "2026-01-02T00:00:00Z",
			},
		},
		UpdatedAt: "2026-01-02T00:00:00Z",
	}

	data, err := json.Marshal(model)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}

	var roundTripped OrchestrationReadModel
	if err := json.Unmarshal(data, &roundTripped); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}

	if roundTripped.SnapshotSequence != 7 {
		t.Errorf("SnapshotSequence = %d, want %d", roundTripped.SnapshotSequence, 7)
	}
	if len(roundTripped.Projects) != 1 {
		t.Fatalf("len(Projects) = %d, want 1", len(roundTripped.Projects))
	}
	if roundTripped.Projects[0].ID != "proj-1" {
		t.Errorf("Projects[0].ID = %q, want %q", roundTripped.Projects[0].ID, "proj-1")
	}
	if roundTripped.Projects[0].DefaultModelSelection == nil {
		t.Fatal("Projects[0].DefaultModelSelection is nil")
	}
	if roundTripped.Projects[0].DefaultModelSelection.Provider != ProviderClaudeAgent {
		t.Errorf("provider = %q, want %q", roundTripped.Projects[0].DefaultModelSelection.Provider, ProviderClaudeAgent)
	}
	if roundTripped.Projects[0].DefaultModelSelection.Options == nil {
		t.Fatal("Options is nil")
	}
	if roundTripped.Projects[0].DefaultModelSelection.Options.Thinking == nil || !*roundTripped.Projects[0].DefaultModelSelection.Options.Thinking {
		t.Error("Thinking should be true")
	}

	if len(roundTripped.Threads) != 1 {
		t.Fatalf("len(Threads) = %d, want 1", len(roundTripped.Threads))
	}
	if roundTripped.Threads[0].Title != "Fix the bug" {
		t.Errorf("Threads[0].Title = %q, want %q", roundTripped.Threads[0].Title, "Fix the bug")
	}
	if roundTripped.UpdatedAt != "2026-01-02T00:00:00Z" {
		t.Errorf("UpdatedAt = %q, want %q", roundTripped.UpdatedAt, "2026-01-02T00:00:00Z")
	}
}
