package io.j3mobile.protocol

import kotlinx.serialization.Serializable

/** Input for a git status request. */
@Serializable
data class GitStatusInput(
    val cwd: String,
)

/** Result of a git status request. */
@Serializable
data class GitStatusResult(
    val branch: String? = null,
    val hasWorkingTreeChanges: Boolean,
    val workingTree: GitWorkingTree,
    val hasUpstream: Boolean,
    val aheadCount: Int,
    val behindCount: Int,
    val pr: GitPullRequest? = null,
)

/** Summarizes changes in the working tree. */
@Serializable
data class GitWorkingTree(
    val files: List<GitFileChange> = emptyList(),
    val insertions: Int,
    val deletions: Int,
)

/** Describes a single changed file. */
@Serializable
data class GitFileChange(
    val path: String,
    val insertions: Int,
    val deletions: Int,
)

/** Describes a pull request associated with a branch. */
@Serializable
data class GitPullRequest(
    val number: Int,
    val title: String,
    val url: String,
    val baseBranch: String,
    val headBranch: String,
    val state: String,
)

/** Input for a git pull request. */
@Serializable
data class GitPullInput(
    val cwd: String,
)

/** Result of a git pull operation. */
@Serializable
data class GitPullResult(
    val success: Boolean,
    val message: String,
)

/** Input for listing branches. */
@Serializable
data class GitListBranchesInput(
    val cwd: String,
)

/** Describes a git branch. */
@Serializable
data class GitBranch(
    val name: String,
    val isCurrent: Boolean,
    val isRemote: Boolean,
    val commit: String,
)

/** Result of listing branches. */
@Serializable
data class GitListBranchesResult(
    val branches: List<GitBranch> = emptyList(),
)

/** Input for creating a branch. */
@Serializable
data class GitCreateBranchInput(
    val cwd: String,
    val branchName: String,
    val startPoint: String? = null,
)

/** Input for checking out a branch. */
@Serializable
data class GitCheckoutInput(
    val cwd: String,
    val branchName: String,
)

/** Input for initializing a git repository. */
@Serializable
data class GitInitInput(
    val cwd: String,
)

/** Input for creating a worktree. */
@Serializable
data class GitCreateWorktreeInput(
    val cwd: String,
    val path: String,
    val branchName: String? = null,
)

/** Result of creating a worktree. */
@Serializable
data class GitCreateWorktreeResult(
    val path: String,
    val branch: String,
)

/** Input for removing a worktree. */
@Serializable
data class GitRemoveWorktreeInput(
    val cwd: String,
    val path: String,
)

/** Input for running a stacked git action. */
@Serializable
data class GitRunStackedActionInput(
    val actionId: String,
    val cwd: String,
    val action: String,
    val commitMessage: String? = null,
    val featureBranch: Boolean? = null,
    val filePaths: List<String>? = null,
    val modelSelection: ModelSelection,
)

/** Result of a stacked git action. */
@Serializable
data class GitRunStackedActionResult(
    val action: String,
    val branch: GitStackedActionBranch? = null,
    val commit: GitStackedActionCommit? = null,
    val push: GitStackedActionPush? = null,
    val pr: GitStackedActionPullRequest? = null,
)

/** Branch result of a stacked action. */
@Serializable
data class GitStackedActionBranch(
    val name: String,
    val created: Boolean,
)

/** Commit result of a stacked action. */
@Serializable
data class GitStackedActionCommit(
    val sha: String,
    val message: String,
)

/** Push result of a stacked action. */
@Serializable
data class GitStackedActionPush(
    val remote: String,
    val branch: String,
)

/** PR result of a stacked action. */
@Serializable
data class GitStackedActionPullRequest(
    val number: Int,
    val url: String,
    val title: String,
)

/** Push event for git action progress. */
@Serializable
data class GitActionProgressEvent(
    val actionId: String,
    val cwd: String,
    val action: String,
    val kind: String,
    val message: String? = null,
    val step: String? = null,
    val progress: Int? = null,
    val total: Int? = null,
    val error: String? = null,
)
