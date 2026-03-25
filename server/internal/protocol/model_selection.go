package protocol

// ProviderKind identifies an LLM provider backend.
type ProviderKind string

const (
	ProviderCodex       ProviderKind = "codex"
	ProviderClaudeAgent ProviderKind = "claudeAgent"
)

// ModelSelection specifies which provider and model to use for a turn.
type ModelSelection struct {
	Provider ProviderKind  `json:"provider"`
	Model    string        `json:"model"`
	Options  *ModelOptions `json:"options,omitempty"`
}

// ModelOptions holds provider-specific tuning knobs.
type ModelOptions struct {
	// Codex options
	ReasoningEffort string `json:"reasoningEffort,omitempty"` // xhigh, high, medium, low
	FastMode        *bool  `json:"fastMode,omitempty"`

	// Claude options
	Thinking *bool  `json:"thinking,omitempty"`
	Effort   string `json:"effort,omitempty"` // low, medium, high, max, ultrathink
}
