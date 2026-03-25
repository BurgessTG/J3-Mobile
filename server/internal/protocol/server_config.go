package protocol

import "encoding/json"

// ServerConfig is the server configuration snapshot.
type ServerConfig struct {
	Cwd                   string                 `json:"cwd"`
	KeybindingsConfigPath string                 `json:"keybindingsConfigPath"`
	Keybindings           json.RawMessage        `json:"keybindings"`
	Issues                []ServerConfigIssue    `json:"issues"`
	Providers             []ServerProviderStatus `json:"providers"`
	AvailableEditors      []string               `json:"availableEditors"`
}

// ServerProviderStatus describes the status of an LLM provider.
type ServerProviderStatus struct {
	Provider   ProviderKind `json:"provider"`
	Status     string       `json:"status"`
	Available  bool         `json:"available"`
	AuthStatus string       `json:"authStatus"`
	CheckedAt  string       `json:"checkedAt"`
	Message    *string      `json:"message,omitempty"`
}

// ServerConfigIssue describes a configuration problem.
type ServerConfigIssue struct {
	Kind    string `json:"kind"`
	Message string `json:"message"`
	Index   *int   `json:"index,omitempty"`
}
