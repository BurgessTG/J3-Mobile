package protocol

import "encoding/json"

// WebSocket method names (client -> server).
const (
	MethodOrchestrationGetSnapshot       = "orchestration.getSnapshot"
	MethodOrchestrationDispatchCommand   = "orchestration.dispatchCommand"
	MethodOrchestrationGetTurnDiff       = "orchestration.getTurnDiff"
	MethodOrchestrationGetFullThreadDiff = "orchestration.getFullThreadDiff"
	MethodOrchestrationReplayEvents      = "orchestration.replayEvents"
	MethodProjectsSearchEntries          = "projects.searchEntries"
	MethodProjectsWriteFile              = "projects.writeFile"
	MethodGitPull                        = "git.pull"
	MethodGitStatus                      = "git.status"
	MethodGitRunStackedAction            = "git.runStackedAction"
	MethodGitListBranches                = "git.listBranches"
	MethodGitCreateWorktree              = "git.createWorktree"
	MethodGitRemoveWorktree              = "git.removeWorktree"
	MethodGitCreateBranch                = "git.createBranch"
	MethodGitCheckout                    = "git.checkout"
	MethodGitInit                        = "git.init"
	MethodGitResolvePullRequest          = "git.resolvePullRequest"
	MethodGitPreparePullRequestThread    = "git.preparePullRequestThread"
	MethodTerminalOpen                   = "terminal.open"
	MethodTerminalWrite                  = "terminal.write"
	MethodTerminalResize                 = "terminal.resize"
	MethodTerminalClear                  = "terminal.clear"
	MethodTerminalRestart                = "terminal.restart"
	MethodTerminalClose                  = "terminal.close"
	MethodShellOpenInEditor              = "shell.openInEditor"
	MethodServerGetConfig                = "server.getConfig"
	MethodServerUpsertKeybinding         = "server.upsertKeybinding"
)

// WebSocket push channel names (server -> client).
const (
	ChannelServerWelcome            = "server.welcome"
	ChannelServerConfigUpdated      = "server.configUpdated"
	ChannelOrchestrationDomainEvent = "orchestration.domainEvent"
	ChannelTerminalEvent            = "terminal.event"
	ChannelGitActionProgress        = "git.actionProgress"
)

// WsRequest is a client-to-server request envelope.
// Body is kept as raw JSON because the bridge passes it through.
type WsRequest struct {
	ID   string          `json:"id"`
	Body json.RawMessage `json:"body"`
}

// WsRequestBody is the minimal shape inside Body used for routing.
type WsRequestBody struct {
	Tag string `json:"_tag"`
}

// WsResponse is a server-to-client response envelope.
type WsResponse struct {
	ID     string          `json:"id"`
	Result json.RawMessage `json:"result,omitempty"`
	Error  *WsError        `json:"error,omitempty"`
}

// WsError carries an error message inside a response.
type WsError struct {
	Message string `json:"message"`
}

// WsPush is a server-to-client push envelope.
type WsPush struct {
	Type     string          `json:"type"`
	Sequence int             `json:"sequence"`
	Channel  string          `json:"channel"`
	Data     json.RawMessage `json:"data"`
}
