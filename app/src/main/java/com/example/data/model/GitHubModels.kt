package com.example.data.model

import com.squareup.moshi.Json

data class GitHubUser(
    val id: Long = 0,
    val login: String = "",
    val name: String? = null,
    val avatar_url: String = "",
    val bio: String? = null,
    val company: String? = null,
    val blog: String? = null,
    val location: String? = null,
    val email: String? = null,
    val twitter_username: String? = null,
    val public_repos: Int = 0,
    val total_private_repos: Int = 0,
    val owned_private_repos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
    val html_url: String = ""
)

data class UpdateUserRequest(
    val name: String? = null,
    val bio: String? = null,
    val company: String? = null,
    val location: String? = null,
    val blog: String? = null,
    val email: String? = null,
    val twitter_username: String? = null
)

data class Repository(
    val id: Long = 0,
    val name: String = "",
    val full_name: String = "",
    val private: Boolean = false,
    val owner: GitHubUser? = null,
    val html_url: String = "",
    val description: String? = null,
    val fork: Boolean = false,
    val url: String = "",
    val clone_url: String = "",
    val ssh_url: String = "",
    val default_branch: String = "main",
    val stargazers_count: Int = 0,
    val watchers_count: Int = 0,
    val forks_count: Int = 0,
    val open_issues_count: Int = 0,
    val language: String? = null,
    val archived: Boolean = false,
    val disabled: Boolean = false,
    val visibility: String = "public",
    val topics: List<String> = emptyList(),
    val created_at: String? = null,
    val updated_at: String? = null,
    val pushed_at: String? = null,
    val size: Long = 0
)

data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    val private: Boolean = false,
    val auto_init: Boolean = false,
    val gitignore_template: String? = null,
    val license_template: String? = null
)

data class UpdateRepoRequest(
    val name: String? = null,
    val description: String? = null,
    val private: Boolean? = null,
    val archived: Boolean? = null,
    val default_branch: String? = null
)

data class ContentItem(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0,
    val url: String = "",
    val html_url: String = "",
    val git_url: String = "",
    val download_url: String? = null,
    val type: String = "file", // "file", "dir", "symlink", "submodule"
    val content: String? = null,
    val encoding: String? = null
)

data class CreateUpdateFileRequest(
    val message: String,
    val content: String, // base64 encoded
    val sha: String? = null,
    val branch: String? = null
)

data class DeleteFileRequest(
    val message: String,
    val sha: String,
    val branch: String? = null
)

data class Branch(
    val name: String = "",
    val commit: BranchCommit = BranchCommit(),
    @Json(name = "protected") val isProtected: Boolean = false
)

data class BranchCommit(
    val sha: String = "",
    val url: String = ""
)

data class GitRef(
    val ref: String = "",
    val node_id: String? = null,
    val url: String = "",
    val `object`: GitObject = GitObject()
)

data class GitObject(
    val sha: String = "",
    val type: String = "",
    val url: String = ""
)

data class CreateRefRequest(
    val ref: String, // "refs/heads/branch-name"
    val sha: String
)

data class CommitItem(
    val sha: String = "",
    val node_id: String? = null,
    val commit: CommitDetail = CommitDetail(),
    val author: GitHubUser? = null,
    val committer: GitHubUser? = null,
    val html_url: String = "",
    val stats: CommitStats? = null,
    val files: List<CommitFile>? = null
)

data class CommitDetail(
    val author: CommitAuthor = CommitAuthor(),
    val committer: CommitAuthor = CommitAuthor(),
    val message: String = "",
    val comment_count: Int = 0
)

data class CommitAuthor(
    val name: String = "",
    val email: String = "",
    val date: String = ""
)

data class CommitStats(
    val total: Int = 0,
    val additions: Int = 0,
    val deletions: Int = 0
)

data class CommitFile(
    val filename: String = "",
    val additions: Int = 0,
    val deletions: Int = 0,
    val changes: Int = 0,
    val status: String = "",
    val patch: String? = null
)

data class Issue(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val user: GitHubUser? = null,
    val state: String = "open", // "open", "closed"
    val locked: Boolean = false,
    val comments: Int = 0,
    val created_at: String = "",
    val updated_at: String = "",
    val closed_at: String? = null,
    val body: String? = null,
    val html_url: String = "",
    val pull_request: Any? = null, // present if this issue is actually a PR
    val labels: List<IssueLabel> = emptyList(),
    val assignees: List<GitHubUser> = emptyList()
) {
    val isPullRequest: Boolean get() = pull_request != null
}

data class IssueLabel(
    val id: Long = 0,
    val name: String = "",
    val color: String = "",
    val description: String? = null
)

data class CreateIssueRequest(
    val title: String,
    val body: String? = null,
    val labels: List<String>? = null,
    val assignees: List<String>? = null
)

data class UpdateIssueRequest(
    val title: String? = null,
    val body: String? = null,
    val state: String? = null, // "open" or "closed"
    val state_reason: String? = null
)

data class IssueComment(
    val id: Long = 0,
    val user: GitHubUser? = null,
    val created_at: String = "",
    val updated_at: String = "",
    val body: String = "",
    val html_url: String = ""
)

data class CreateCommentRequest(
    val body: String
)

data class PullRequest(
    val id: Long = 0,
    val number: Int = 0,
    val state: String = "open",
    val title: String = "",
    val user: GitHubUser? = null,
    val body: String? = null,
    val created_at: String = "",
    val updated_at: String = "",
    val closed_at: String? = null,
    val merged_at: String? = null,
    val html_url: String = "",
    val head: PullBranch = PullBranch(),
    val base: PullBranch = PullBranch(),
    val draft: Boolean = false,
    val mergeable: Boolean? = null,
    val mergeable_state: String? = null,
    val comments: Int = 0,
    val commits: Int = 0,
    val additions: Int = 0,
    val deletions: Int = 0,
    val changed_files: Int = 0
)

data class PullBranch(
    val label: String = "",
    val ref: String = "",
    val sha: String = "",
    val repo: Repository? = null
)

data class MergePullRequestRequest(
    val commit_title: String? = null,
    val commit_message: String? = null,
    val merge_method: String = "merge" // "merge", "squash", "rebase"
)

data class MergeResult(
    val sha: String = "",
    val merged: Boolean = false,
    val message: String = ""
)

data class NotificationItem(
    val id: String = "",
    val repository: Repository = Repository(),
    val subject: NotificationSubject = NotificationSubject(),
    val reason: String = "",
    val unread: Boolean = true,
    val updated_at: String = "",
    val last_read_at: String? = null,
    val url: String = ""
)

data class NotificationSubject(
    val title: String = "",
    val url: String? = null,
    val latest_comment_url: String? = null,
    val type: String = "" // "Issue", "PullRequest", "Commit", "Release", etc.
)

data class MarkAllReadRequest(
    val last_read_at: String? = null
)

data class Gist(
    val id: String = "",
    val description: String? = null,
    val public: Boolean = false,
    val owner: GitHubUser? = null,
    val files: Map<String, GistFile> = emptyMap(),
    val comments: Int = 0,
    val html_url: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)

data class GistFile(
    val filename: String = "",
    val type: String = "",
    val language: String? = null,
    val raw_url: String = "",
    val size: Long = 0,
    val content: String? = null
)

data class CreateGistRequest(
    val description: String? = null,
    val public: Boolean = false,
    val files: Map<String, GistFileContent>
)

data class GistFileContent(
    val content: String
)

data class UpdateGistRequest(
    val description: String? = null,
    val files: Map<String, GistFileContent?>
)

data class Collaborator(
    val id: Long = 0,
    val login: String = "",
    val avatar_url: String = "",
    val permissions: CollaboratorPermissions? = null,
    val role_name: String? = null
)

data class CollaboratorPermissions(
    val pull: Boolean = false,
    val triage: Boolean = false,
    val push: Boolean = false,
    val maintain: Boolean = false,
    val admin: Boolean = false
)

data class AddCollaboratorRequest(
    val permission: String = "push" // "pull", "triage", "push", "maintain", "admin"
)

data class Webhook(
    val id: Long = 0,
    val name: String = "web",
    val active: Boolean = true,
    val events: List<String> = listOf("push"),
    val config: WebhookConfig = WebhookConfig(),
    val updated_at: String = "",
    val created_at: String = ""
)

data class WebhookConfig(
    val url: String = "",
    val content_type: String = "json",
    val secret: String? = null,
    val insecure_ssl: String = "0"
)

data class CreateWebhookRequest(
    val name: String = "web",
    val active: Boolean = true,
    val events: List<String> = listOf("push"),
    val config: WebhookConfig
)

data class Release(
    val id: Long = 0,
    val tag_name: String = "",
    val target_commitish: String = "main",
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val created_at: String = "",
    val published_at: String? = null,
    val html_url: String = "",
    val author: GitHubUser? = null
)

data class CreateReleaseRequest(
    val tag_name: String,
    val target_commitish: String = "main",
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false
)

data class SearchResult<T>(
    val total_count: Int = 0,
    val incomplete_results: Boolean = false,
    val items: List<T> = emptyList()
)

data class CodeSearchItem(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val html_url: String = "",
    val repository: Repository = Repository()
)
