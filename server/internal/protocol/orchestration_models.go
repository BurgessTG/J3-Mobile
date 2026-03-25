package protocol

import "encoding/json"

// OrchestrationReadModel is the top-level snapshot returned by getSnapshot.
type OrchestrationReadModel struct {
	SnapshotSequence int                    `json:"snapshotSequence"`
	Projects         []OrchestrationProject `json:"projects"`
	Threads          []OrchestrationThread  `json:"threads"`
	UpdatedAt        string                 `json:"updatedAt"`
}

// OrchestrationProject represents a workspace project.
type OrchestrationProject struct {
	ID                    ProjectId       `json:"id"`
	Title                 string          `json:"title"`
	WorkspaceRoot         string          `json:"workspaceRoot"`
	DefaultModelSelection *ModelSelection `json:"defaultModelSelection,omitempty"`
	Scripts               []ProjectScript `json:"scripts"`
	CreatedAt             string          `json:"createdAt"`
	UpdatedAt             string          `json:"updatedAt"`
	DeletedAt             string          `json:"deletedAt,omitempty"`
}

// ProjectScript is a user-defined script attached to a project.
type ProjectScript struct {
	ID                  string `json:"id"`
	Name                string `json:"name"`
	Command             string `json:"command"`
	Icon                string `json:"icon"`
	RunOnWorktreeCreate bool   `json:"runOnWorktreeCreate"`
}

// OrchestrationThread represents a conversation thread.
type OrchestrationThread struct {
	ID              ThreadId                         `json:"id"`
	ProjectId       ProjectId                        `json:"projectId"`
	Title           string                           `json:"title"`
	ModelSelection  ModelSelection                   `json:"modelSelection"`
	RuntimeMode     string                           `json:"runtimeMode"`
	InteractionMode string                           `json:"interactionMode"`
	Branch          *string                          `json:"branch,omitempty"`
	WorktreePath    *string                          `json:"worktreePath,omitempty"`
	LatestTurn      *OrchestrationLatestTurn         `json:"latestTurn,omitempty"`
	Messages        []OrchestrationMessage           `json:"messages"`
	ProposedPlans   []OrchestrationProposedPlan      `json:"proposedPlans"`
	Activities      []OrchestrationThreadActivity    `json:"activities"`
	Checkpoints     []OrchestrationCheckpointSummary `json:"checkpoints"`
	Session         *OrchestrationSession            `json:"session,omitempty"`
	CreatedAt       string                           `json:"createdAt"`
	UpdatedAt       string                           `json:"updatedAt"`
	DeletedAt       string                           `json:"deletedAt,omitempty"`
}

// OrchestrationMessage is a single message in a thread.
type OrchestrationMessage struct {
	ID          MessageId        `json:"id"`
	Role        string           `json:"role"`
	Text        string           `json:"text"`
	Attachments []ChatAttachment `json:"attachments"`
	TurnId      *TurnId          `json:"turnId,omitempty"`
	Streaming   bool             `json:"streaming"`
	CreatedAt   string           `json:"createdAt"`
	UpdatedAt   string           `json:"updatedAt"`
}

// ChatAttachment is a file attachment on a message.
type ChatAttachment struct {
	Type      string `json:"type"`
	ID        string `json:"id"`
	Name      string `json:"name"`
	MimeType  string `json:"mimeType"`
	SizeBytes int    `json:"sizeBytes"`
}

// OrchestrationLatestTurn tracks the most recent turn in a thread.
type OrchestrationLatestTurn struct {
	TurnId             TurnId     `json:"turnId"`
	State              string     `json:"state"`
	RequestedAt        string     `json:"requestedAt"`
	StartedAt          *string    `json:"startedAt,omitempty"`
	CompletedAt        *string    `json:"completedAt,omitempty"`
	AssistantMessageId *MessageId `json:"assistantMessageId,omitempty"`
}

// OrchestrationSession describes the runtime session for a thread.
type OrchestrationSession struct {
	ThreadId     ThreadId `json:"threadId"`
	Status       string   `json:"status"`
	ProviderName *string  `json:"providerName,omitempty"`
	RuntimeMode  string   `json:"runtimeMode"`
	ActiveTurnId *TurnId  `json:"activeTurnId,omitempty"`
	LastError    *string  `json:"lastError,omitempty"`
	UpdatedAt    string   `json:"updatedAt"`
}

// OrchestrationProposedPlan is a plan the assistant has proposed.
type OrchestrationProposedPlan struct {
	ID                     string    `json:"id"`
	TurnId                 *TurnId   `json:"turnId,omitempty"`
	PlanMarkdown           string    `json:"planMarkdown"`
	ImplementedAt          *string   `json:"implementedAt,omitempty"`
	ImplementationThreadId *ThreadId `json:"implementationThreadId,omitempty"`
	CreatedAt              string    `json:"createdAt"`
	UpdatedAt              string    `json:"updatedAt"`
}

// OrchestrationCheckpointSummary describes a checkpoint in a thread.
type OrchestrationCheckpointSummary struct {
	TurnId              TurnId                        `json:"turnId"`
	CheckpointTurnCount int                           `json:"checkpointTurnCount"`
	CheckpointRef       CheckpointRef                 `json:"checkpointRef"`
	Status              string                        `json:"status"`
	Files               []OrchestrationCheckpointFile `json:"files"`
	AssistantMessageId  *MessageId                    `json:"assistantMessageId,omitempty"`
	CompletedAt         string                        `json:"completedAt"`
}

// OrchestrationCheckpointFile describes a file changed in a checkpoint.
type OrchestrationCheckpointFile struct {
	Path      string `json:"path"`
	Kind      string `json:"kind"`
	Additions int    `json:"additions"`
	Deletions int    `json:"deletions"`
}

// OrchestrationThreadActivity is an activity event in a thread.
type OrchestrationThreadActivity struct {
	ID        EventId         `json:"id"`
	Tone      string          `json:"tone"`
	Kind      string          `json:"kind"`
	Summary   string          `json:"summary"`
	Payload   json.RawMessage `json:"payload"`
	TurnId    *TurnId         `json:"turnId,omitempty"`
	Sequence  *int            `json:"sequence,omitempty"`
	CreatedAt string          `json:"createdAt"`
}
