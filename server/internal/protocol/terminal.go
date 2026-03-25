package protocol

// TerminalOpenInput is the input for opening a new terminal session.
type TerminalOpenInput struct {
	ThreadId   ThreadId          `json:"threadId"`
	TerminalId string            `json:"terminalId"`
	Cwd        string            `json:"cwd"`
	Cols       *int              `json:"cols,omitempty"`
	Rows       *int              `json:"rows,omitempty"`
	Env        map[string]string `json:"env,omitempty"`
}

// TerminalWriteInput is the input for writing data to a terminal.
type TerminalWriteInput struct {
	ThreadId   ThreadId `json:"threadId"`
	TerminalId string   `json:"terminalId"`
	Data       string   `json:"data"`
}

// TerminalResizeInput is the input for resizing a terminal.
type TerminalResizeInput struct {
	ThreadId   ThreadId `json:"threadId"`
	TerminalId string   `json:"terminalId"`
	Cols       int      `json:"cols"`
	Rows       int      `json:"rows"`
}

// TerminalClearInput is the input for clearing a terminal.
type TerminalClearInput struct {
	ThreadId   ThreadId `json:"threadId"`
	TerminalId string   `json:"terminalId"`
}

// TerminalCloseInput is the input for closing a terminal.
type TerminalCloseInput struct {
	ThreadId      ThreadId `json:"threadId"`
	TerminalId    string   `json:"terminalId"`
	DeleteHistory *bool    `json:"deleteHistory,omitempty"`
}

// TerminalRestartInput is the input for restarting a terminal.
type TerminalRestartInput struct {
	ThreadId   ThreadId          `json:"threadId"`
	TerminalId string            `json:"terminalId"`
	Cwd        string            `json:"cwd"`
	Cols       int               `json:"cols"`
	Rows       int               `json:"rows"`
	Env        map[string]string `json:"env,omitempty"`
}

// TerminalSessionSnapshot is the state of a terminal session.
type TerminalSessionSnapshot struct {
	ThreadId   ThreadId `json:"threadId"`
	TerminalId string   `json:"terminalId"`
	Cwd        string   `json:"cwd"`
	Status     string   `json:"status"`
	Pid        *int     `json:"pid,omitempty"`
	History    string   `json:"history"`
	ExitCode   *int     `json:"exitCode,omitempty"`
	ExitSignal *int     `json:"exitSignal,omitempty"`
	UpdatedAt  string   `json:"updatedAt"`
}

// TerminalEvent is a push event for terminal state changes.
type TerminalEvent struct {
	ThreadId             ThreadId                 `json:"threadId"`
	TerminalId           string                   `json:"terminalId"`
	Type                 string                   `json:"type"`
	CreatedAt            string                   `json:"createdAt"`
	Data                 *string                  `json:"data,omitempty"`
	Snapshot             *TerminalSessionSnapshot `json:"snapshot,omitempty"`
	ExitCode             *int                     `json:"exitCode,omitempty"`
	ExitSignal           *int                     `json:"exitSignal,omitempty"`
	Message              *string                  `json:"message,omitempty"`
	HasRunningSubprocess *bool                    `json:"hasRunningSubprocess,omitempty"`
}
