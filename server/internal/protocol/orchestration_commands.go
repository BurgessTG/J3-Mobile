package protocol

// Command type constants.
const (
	CmdProjectCreate            = "project.create"
	CmdProjectMetaUpdate        = "project.meta.update"
	CmdProjectDelete            = "project.delete"
	CmdThreadCreate             = "thread.create"
	CmdThreadDelete             = "thread.delete"
	CmdThreadMetaUpdate         = "thread.meta.update"
	CmdThreadRuntimeModeSet     = "thread.runtime-mode.set"
	CmdThreadInteractionModeSet = "thread.interaction-mode.set"
	CmdThreadTurnStart          = "thread.turn.start"
	CmdThreadTurnInterrupt      = "thread.turn.interrupt"
	CmdThreadApprovalRespond    = "thread.approval.respond"
	CmdThreadUserInputRespond   = "thread.user-input.respond"
	CmdThreadCheckpointRevert   = "thread.checkpoint.revert"
	CmdThreadSessionStop        = "thread.session.stop"
)

// CommandBase holds fields common to all commands.
type CommandBase struct {
	Type      string    `json:"type"`
	CommandId CommandId `json:"commandId"`
	CreatedAt string    `json:"createdAt"`
}

// ProjectCreateCommand creates a new project.
type ProjectCreateCommand struct {
	CommandBase
	ProjectId             ProjectId       `json:"projectId"`
	Title                 string          `json:"title"`
	WorkspaceRoot         string          `json:"workspaceRoot"`
	DefaultModelSelection *ModelSelection `json:"defaultModelSelection,omitempty"`
}

// ProjectMetaUpdateCommand updates project metadata.
type ProjectMetaUpdateCommand struct {
	CommandBase
	ProjectId             ProjectId       `json:"projectId"`
	Title                 *string         `json:"title,omitempty"`
	WorkspaceRoot         *string         `json:"workspaceRoot,omitempty"`
	DefaultModelSelection *ModelSelection `json:"defaultModelSelection,omitempty"`
	Scripts               []ProjectScript `json:"scripts,omitempty"`
}

// ProjectDeleteCommand deletes a project.
type ProjectDeleteCommand struct {
	CommandBase
	ProjectId ProjectId `json:"projectId"`
}

// ThreadCreateCommand creates a new thread.
type ThreadCreateCommand struct {
	CommandBase
	ThreadId        ThreadId       `json:"threadId"`
	ProjectId       ProjectId      `json:"projectId"`
	Title           string         `json:"title"`
	ModelSelection  ModelSelection `json:"modelSelection"`
	RuntimeMode     string         `json:"runtimeMode"`
	InteractionMode string         `json:"interactionMode"`
	Branch          *string        `json:"branch,omitempty"`
	WorktreePath    *string        `json:"worktreePath,omitempty"`
}

// ThreadDeleteCommand deletes a thread.
type ThreadDeleteCommand struct {
	CommandBase
	ThreadId ThreadId `json:"threadId"`
}

// ThreadMetaUpdateCommand updates thread metadata.
type ThreadMetaUpdateCommand struct {
	CommandBase
	ThreadId       ThreadId        `json:"threadId"`
	Title          *string         `json:"title,omitempty"`
	ModelSelection *ModelSelection `json:"modelSelection,omitempty"`
	Branch         *string         `json:"branch,omitempty"`
	WorktreePath   *string         `json:"worktreePath,omitempty"`
}

// ThreadRuntimeModeSetCommand changes the runtime mode for a thread.
type ThreadRuntimeModeSetCommand struct {
	CommandBase
	ThreadId    ThreadId `json:"threadId"`
	RuntimeMode string   `json:"runtimeMode"`
}

// ThreadInteractionModeSetCommand changes the interaction mode for a thread.
type ThreadInteractionModeSetCommand struct {
	CommandBase
	ThreadId        ThreadId `json:"threadId"`
	InteractionMode string   `json:"interactionMode"`
}

// TurnStartMessage is the user message that initiates a turn.
type TurnStartMessage struct {
	MessageId   MessageId              `json:"messageId"`
	Role        string                 `json:"role"`
	Text        string                 `json:"text"`
	Attachments []UploadChatAttachment `json:"attachments"`
}

// UploadChatAttachment is an attachment being sent with a turn-start message.
type UploadChatAttachment struct {
	Type      string `json:"type"`
	ID        string `json:"id"`
	Name      string `json:"name"`
	MimeType  string `json:"mimeType"`
	SizeBytes int    `json:"sizeBytes"`
	DataUrl   string `json:"dataUrl"`
}

// ThreadTurnStartCommand starts a new turn in a thread.
type ThreadTurnStartCommand struct {
	CommandBase
	ThreadId              ThreadId         `json:"threadId"`
	Message               TurnStartMessage `json:"message"`
	ModelSelection        *ModelSelection  `json:"modelSelection,omitempty"`
	RuntimeMode           string           `json:"runtimeMode"`
	InteractionMode       string           `json:"interactionMode"`
	AssistantDeliveryMode string           `json:"assistantDeliveryMode"`
}

// ThreadTurnInterruptCommand interrupts the current turn.
type ThreadTurnInterruptCommand struct {
	CommandBase
	ThreadId ThreadId `json:"threadId"`
}

// ThreadApprovalRespondCommand responds to an approval request.
type ThreadApprovalRespondCommand struct {
	CommandBase
	ThreadId  ThreadId          `json:"threadId"`
	RequestId ApprovalRequestId `json:"requestId"`
	Decision  string            `json:"decision"`
}

// ThreadUserInputRespondCommand responds to a user-input request.
type ThreadUserInputRespondCommand struct {
	CommandBase
	ThreadId  ThreadId          `json:"threadId"`
	RequestId ApprovalRequestId `json:"requestId"`
	Answers   map[string]string `json:"answers"`
}

// ThreadCheckpointRevertCommand reverts to a checkpoint.
type ThreadCheckpointRevertCommand struct {
	CommandBase
	ThreadId  ThreadId `json:"threadId"`
	TurnCount int      `json:"turnCount"`
}

// ThreadSessionStopCommand stops the runtime session for a thread.
type ThreadSessionStopCommand struct {
	CommandBase
	ThreadId ThreadId `json:"threadId"`
}
