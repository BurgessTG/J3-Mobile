package protocol

// GitStatusInput is the input for a git status request.
type GitStatusInput struct {
	Cwd string `json:"cwd"`
}

// GitStatusResult is the result of a git status request.
type GitStatusResult struct {
	Branch                *string         `json:"branch,omitempty"`
	HasWorkingTreeChanges bool            `json:"hasWorkingTreeChanges"`
	WorkingTree           GitWorkingTree  `json:"workingTree"`
	HasUpstream           bool            `json:"hasUpstream"`
	AheadCount            int             `json:"aheadCount"`
	BehindCount           int             `json:"behindCount"`
	PR                    *GitPullRequest `json:"pr,omitempty"`
}

// GitWorkingTree summarizes changes in the working tree.
type GitWorkingTree struct {
	Files      []GitFileChange `json:"files"`
	Insertions int             `json:"insertions"`
	Deletions  int             `json:"deletions"`
}

// GitFileChange describes a single changed file.
type GitFileChange struct {
	Path       string `json:"path"`
	Insertions int    `json:"insertions"`
	Deletions  int    `json:"deletions"`
}

// GitPullRequest describes a pull request associated with a branch.
type GitPullRequest struct {
	Number     int    `json:"number"`
	Title      string `json:"title"`
	URL        string `json:"url"`
	BaseBranch string `json:"baseBranch"`
	HeadBranch string `json:"headBranch"`
	State      string `json:"state"`
}

// GitPullInput is the input for a git pull request.
type GitPullInput struct {
	Cwd string `json:"cwd"`
}

// GitPullResult is the result of a git pull operation.
type GitPullResult struct {
	Success bool   `json:"success"`
	Message string `json:"message"`
}

// GitListBranchesInput is the input for listing branches.
type GitListBranchesInput struct {
	Cwd string `json:"cwd"`
}

// GitBranch describes a git branch.
type GitBranch struct {
	Name      string `json:"name"`
	IsCurrent bool   `json:"isCurrent"`
	IsRemote  bool   `json:"isRemote"`
	Commit    string `json:"commit"`
}

// GitListBranchesResult is the result of listing branches.
type GitListBranchesResult struct {
	Branches []GitBranch `json:"branches"`
}

// GitCreateBranchInput is the input for creating a branch.
type GitCreateBranchInput struct {
	Cwd        string  `json:"cwd"`
	BranchName string  `json:"branchName"`
	StartPoint *string `json:"startPoint,omitempty"`
}

// GitCheckoutInput is the input for checking out a branch.
type GitCheckoutInput struct {
	Cwd        string `json:"cwd"`
	BranchName string `json:"branchName"`
}

// GitInitInput is the input for initializing a git repository.
type GitInitInput struct {
	Cwd string `json:"cwd"`
}

// GitCreateWorktreeInput is the input for creating a worktree.
type GitCreateWorktreeInput struct {
	Cwd        string  `json:"cwd"`
	Path       string  `json:"path"`
	BranchName *string `json:"branchName,omitempty"`
}

// GitCreateWorktreeResult is the result of creating a worktree.
type GitCreateWorktreeResult struct {
	Path   string `json:"path"`
	Branch string `json:"branch"`
}

// GitRemoveWorktreeInput is the input for removing a worktree.
type GitRemoveWorktreeInput struct {
	Cwd  string `json:"cwd"`
	Path string `json:"path"`
}

// GitRunStackedActionInput is the input for running a stacked git action.
type GitRunStackedActionInput struct {
	ActionId       string         `json:"actionId"`
	Cwd            string         `json:"cwd"`
	Action         string         `json:"action"`
	CommitMessage  *string        `json:"commitMessage,omitempty"`
	FeatureBranch  *bool          `json:"featureBranch,omitempty"`
	FilePaths      []string       `json:"filePaths,omitempty"`
	ModelSelection ModelSelection `json:"modelSelection"`
}

// GitRunStackedActionResult is the result of a stacked git action.
type GitRunStackedActionResult struct {
	Action string                       `json:"action"`
	Branch *GitStackedActionBranch      `json:"branch,omitempty"`
	Commit *GitStackedActionCommit      `json:"commit,omitempty"`
	Push   *GitStackedActionPush        `json:"push,omitempty"`
	PR     *GitStackedActionPullRequest `json:"pr,omitempty"`
}

// GitStackedActionBranch describes the branch result of a stacked action.
type GitStackedActionBranch struct {
	Name    string `json:"name"`
	Created bool   `json:"created"`
}

// GitStackedActionCommit describes the commit result of a stacked action.
type GitStackedActionCommit struct {
	SHA     string `json:"sha"`
	Message string `json:"message"`
}

// GitStackedActionPush describes the push result of a stacked action.
type GitStackedActionPush struct {
	Remote string `json:"remote"`
	Branch string `json:"branch"`
}

// GitStackedActionPullRequest describes the PR result of a stacked action.
type GitStackedActionPullRequest struct {
	Number int    `json:"number"`
	URL    string `json:"url"`
	Title  string `json:"title"`
}

// GitActionProgressEvent is a push event for git action progress.
type GitActionProgressEvent struct {
	ActionId string  `json:"actionId"`
	Cwd      string  `json:"cwd"`
	Action   string  `json:"action"`
	Kind     string  `json:"kind"`
	Message  *string `json:"message,omitempty"`
	Step     *string `json:"step,omitempty"`
	Progress *int    `json:"progress,omitempty"`
	Total    *int    `json:"total,omitempty"`
	Error    *string `json:"error,omitempty"`
}
