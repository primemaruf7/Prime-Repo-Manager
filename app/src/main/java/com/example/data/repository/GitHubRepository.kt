package com.example.data.repository

import com.example.data.api.GitHubApiService
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T, val isOffline: Boolean = false) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null, val isNetworkError: Boolean = false) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

class GitHubRepository(
    private val api: GitHubApiService,
    private val database: AppDatabase
) {

    private fun parseError(throwable: Throwable): ApiResult.Error {
        return when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                val message = when (code) {
                    401 -> "Unauthorized: Your GitHub Personal Access Token is invalid or expired."
                    403 -> "Forbidden: You don't have permission or GitHub rate limit was reached."
                    404 -> "Resource not found (404)."
                    409 -> "Conflict: A conflict occurred (e.g., SHA mismatch or file already exists)."
                    422 -> "Validation failed (422): Check input parameters."
                    429 -> "GitHub rate limit exceeded. Please wait a few moments."
                    in 500..599 -> "GitHub server error ($code). Please try again later."
                    else -> "GitHub API error: HTTP $code"
                }
                ApiResult.Error(message = message, code = code)
            }
            is UnknownHostException -> ApiResult.Error(message = "Network unavailable: Unable to reach GitHub servers.", isNetworkError = true)
            is SocketTimeoutException -> ApiResult.Error(message = "Connection timed out. Please check your internet connection.", isNetworkError = true)
            is IOException -> ApiResult.Error(message = "Network error: ${throwable.localizedMessage ?: "Unknown I/O error"}", isNetworkError = true)
            else -> ApiResult.Error(message = throwable.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    suspend fun getCurrentUser(forceRefresh: Boolean = false): ApiResult<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val user = api.getCurrentUser()
            // Cache user in Room
            database.userDao().insertUser(
                CachedUserEntity(
                    id = user.id,
                    login = user.login,
                    name = user.name,
                    avatarUrl = user.avatar_url,
                    bio = user.bio,
                    company = user.company,
                    location = user.location,
                    blog = user.blog,
                    publicRepos = user.public_repos,
                    totalPrivateRepos = user.total_private_repos,
                    followers = user.followers,
                    following = user.following,
                    htmlUrl = user.html_url
                )
            )
            ApiResult.Success(user)
        } catch (e: Exception) {
            // Check cache
            val cached = database.userDao().getCachedUserOnce()
            if (cached != null) {
                ApiResult.Success(
                    GitHubUser(
                        id = cached.id,
                        login = cached.login,
                        name = cached.name,
                        avatar_url = cached.avatarUrl,
                        bio = cached.bio,
                        company = cached.company,
                        location = cached.location,
                        blog = cached.blog,
                        public_repos = cached.publicRepos,
                        total_private_repos = cached.totalPrivateRepos,
                        followers = cached.followers,
                        following = cached.following,
                        html_url = cached.htmlUrl
                    ),
                    isOffline = true
                )
            } else {
                parseError(e)
            }
        }
    }

    suspend fun updateUser(req: UpdateUserRequest): ApiResult<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val user = api.updateUser(req)
            ApiResult.Success(user)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getUserRepos(
        page: Int = 1,
        perPage: Int = 30,
        sort: String = "updated",
        direction: String = "desc",
        visibility: String = "all"
    ): ApiResult<List<Repository>> = withContext(Dispatchers.IO) {
        try {
            val repos = api.getUserRepos(
                page = page,
                perPage = perPage,
                sort = sort,
                direction = direction,
                visibility = visibility
            )
            // Cache page 1 in Room
            if (page == 1) {
                val entities = repos.map {
                    CachedRepoEntity(
                        id = it.id,
                        name = it.name,
                        fullName = it.full_name,
                        isPrivate = it.private,
                        ownerLogin = it.owner?.login ?: "",
                        ownerAvatar = it.owner?.avatar_url ?: "",
                        description = it.description,
                        language = it.language,
                        stars = it.stargazers_count,
                        forks = it.forks_count,
                        openIssues = it.open_issues_count,
                        defaultBranch = it.default_branch,
                        cloneUrl = it.clone_url,
                        sshUrl = it.ssh_url,
                        isArchived = it.archived,
                        isFork = it.fork,
                        visibility = it.visibility,
                        updatedAt = it.updated_at
                    )
                }
                database.repoDao().insertRepos(entities)
            }
            ApiResult.Success(repos)
        } catch (e: Exception) {
            val cached = database.repoDao().getAllReposOnce()
            if (cached.isNotEmpty()) {
                val mapped = cached.map {
                    Repository(
                        id = it.id,
                        name = it.name,
                        full_name = it.fullName,
                        private = it.isPrivate,
                        owner = GitHubUser(login = it.ownerLogin, avatar_url = it.ownerAvatar),
                        description = it.description,
                        language = it.language,
                        stargazers_count = it.stars,
                        forks_count = it.forks,
                        open_issues_count = it.openIssues,
                        default_branch = it.defaultBranch,
                        clone_url = it.cloneUrl,
                        ssh_url = it.sshUrl,
                        archived = it.isArchived,
                        fork = it.isFork,
                        visibility = it.visibility,
                        updated_at = it.updatedAt
                    )
                }
                ApiResult.Success(mapped, isOffline = true)
            } else {
                parseError(e)
            }
        }
    }

    suspend fun createRepository(req: CreateRepoRequest): ApiResult<Repository> = withContext(Dispatchers.IO) {
        try {
            val repo = api.createRepo(req)
            ApiResult.Success(repo)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getRepo(owner: String, repo: String): ApiResult<Repository> = withContext(Dispatchers.IO) {
        try {
            val r = api.getRepo(owner, repo)
            ApiResult.Success(r)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun updateRepo(owner: String, repo: String, req: UpdateRepoRequest): ApiResult<Repository> = withContext(Dispatchers.IO) {
        try {
            val r = api.updateRepo(owner, repo, req)
            ApiResult.Success(r)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun deleteRepo(owner: String, repo: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteRepo(owner, repo)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun checkStar(owner: String, repo: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = api.checkStar(owner, repo)
            resp.code() == 204
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleStar(owner: String, repo: String, currentlyStarred: Boolean): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (currentlyStarred) {
                api.unstarRepo(owner, repo)
                ApiResult.Success(false)
            } else {
                api.starRepo(owner, repo)
                ApiResult.Success(true)
            }
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun forkRepo(owner: String, repo: String): ApiResult<Repository> = withContext(Dispatchers.IO) {
        try {
            val r = api.forkRepo(owner, repo)
            ApiResult.Success(r)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getContents(owner: String, repo: String, path: String, ref: String? = null): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val body = api.getContents(owner, repo, path, ref)
            ApiResult.Success(body.string())
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getReadme(owner: String, repo: String, ref: String? = null): ApiResult<ContentItem> = withContext(Dispatchers.IO) {
        try {
            val readme = api.getReadme(owner, repo, ref)
            ApiResult.Success(readme)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun createOrUpdateFile(
        owner: String,
        repo: String,
        path: String,
        req: CreateUpdateFileRequest
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val body = api.createOrUpdateFile(owner, repo, path, req)
            ApiResult.Success(body.string())
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun deleteFile(
        owner: String,
        repo: String,
        path: String,
        req: DeleteFileRequest
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val body = api.deleteFile(owner, repo, path, req)
            ApiResult.Success(body.string())
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getBranches(owner: String, repo: String): ApiResult<List<Branch>> = withContext(Dispatchers.IO) {
        try {
            val list = api.getBranches(owner, repo)
            ApiResult.Success(list)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun createBranch(owner: String, repo: String, branchName: String, sourceSha: String): ApiResult<GitRef> = withContext(Dispatchers.IO) {
        try {
            val ref = api.createRef(owner, repo, CreateRefRequest(ref = "refs/heads/$branchName", sha = sourceSha))
            ApiResult.Success(ref)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun deleteBranch(owner: String, repo: String, branchName: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteRef(owner, repo, "heads/$branchName")
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getCommits(
        owner: String,
        repo: String,
        sha: String? = null,
        path: String? = null,
        page: Int = 1
    ): ApiResult<List<CommitItem>> = withContext(Dispatchers.IO) {
        try {
            val list = api.getCommits(owner, repo, sha, path, page)
            ApiResult.Success(list)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getCommitDetail(owner: String, repo: String, ref: String): ApiResult<CommitItem> = withContext(Dispatchers.IO) {
        try {
            val commit = api.getCommitDetail(owner, repo, ref)
            ApiResult.Success(commit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getIssues(
        owner: String,
        repo: String,
        state: String = "open",
        page: Int = 1
    ): ApiResult<List<Issue>> = withContext(Dispatchers.IO) {
        try {
            val issues = api.getIssues(owner, repo, state, page)
            ApiResult.Success(issues)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun createIssue(owner: String, repo: String, req: CreateIssueRequest): ApiResult<Issue> = withContext(Dispatchers.IO) {
        try {
            val issue = api.createIssue(owner, repo, req)
            ApiResult.Success(issue)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun updateIssue(
        owner: String,
        repo: String,
        issueNumber: Int,
        req: UpdateIssueRequest
    ): ApiResult<Issue> = withContext(Dispatchers.IO) {
        try {
            val issue = api.updateIssue(owner, repo, issueNumber, req)
            ApiResult.Success(issue)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getIssueComments(owner: String, repo: String, issueNumber: Int): ApiResult<List<IssueComment>> = withContext(Dispatchers.IO) {
        try {
            val comments = api.getIssueComments(owner, repo, issueNumber)
            ApiResult.Success(comments)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun addIssueComment(owner: String, repo: String, issueNumber: Int, body: String): ApiResult<IssueComment> = withContext(Dispatchers.IO) {
        try {
            val comment = api.createIssueComment(owner, repo, issueNumber, CreateCommentRequest(body))
            ApiResult.Success(comment)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getPullRequests(
        owner: String,
        repo: String,
        state: String = "open",
        page: Int = 1
    ): ApiResult<List<PullRequest>> = withContext(Dispatchers.IO) {
        try {
            val prs = api.getPullRequests(owner, repo, state, page)
            ApiResult.Success(prs)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getPullRequestDetail(owner: String, repo: String, pullNumber: Int): ApiResult<PullRequest> = withContext(Dispatchers.IO) {
        try {
            val pr = api.getPullRequest(owner, repo, pullNumber)
            ApiResult.Success(pr)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun mergePullRequest(
        owner: String,
        repo: String,
        pullNumber: Int,
        req: MergePullRequestRequest
    ): ApiResult<MergeResult> = withContext(Dispatchers.IO) {
        try {
            val result = api.mergePullRequest(owner, repo, pullNumber, req)
            ApiResult.Success(result)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getNotifications(all: Boolean = true, page: Int = 1): ApiResult<List<NotificationItem>> = withContext(Dispatchers.IO) {
        try {
            val list = api.getNotifications(all, page)
            if (page == 1) {
                val entities = list.map {
                    CachedNotificationEntity(
                        id = it.id,
                        repoFullName = it.repository.full_name,
                        title = it.subject.title,
                        type = it.subject.type,
                        reason = it.reason,
                        unread = it.unread,
                        updatedAt = it.updated_at
                    )
                }
                database.notificationDao().insertNotifications(entities)
            }
            ApiResult.Success(list)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun markNotificationRead(threadId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.markNotificationRead(threadId)
            database.notificationDao().markAsRead(threadId)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun markAllNotificationsRead(): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.markAllNotificationsRead()
            database.notificationDao().clearNotifications()
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getGists(page: Int = 1): ApiResult<List<Gist>> = withContext(Dispatchers.IO) {
        try {
            val gists = api.getGists(page)
            ApiResult.Success(gists)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun createGist(req: CreateGistRequest): ApiResult<Gist> = withContext(Dispatchers.IO) {
        try {
            val gist = api.createGist(req)
            ApiResult.Success(gist)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun deleteGist(gistId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteGist(gistId)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun searchRepositories(query: String, page: Int = 1, sort: String? = null): ApiResult<SearchResult<Repository>> = withContext(Dispatchers.IO) {
        try {
            val result = api.searchRepositories(query, page, 30, sort)
            ApiResult.Success(result)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun searchUsers(query: String, page: Int = 1): ApiResult<SearchResult<GitHubUser>> = withContext(Dispatchers.IO) {
        try {
            val result = api.searchUsers(query, page, 30)
            ApiResult.Success(result)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun searchCode(query: String, page: Int = 1): ApiResult<SearchResult<CodeSearchItem>> = withContext(Dispatchers.IO) {
        try {
            val result = api.searchCode(query, page, 30)
            ApiResult.Success(result)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun searchIssues(query: String, page: Int = 1): ApiResult<SearchResult<Issue>> = withContext(Dispatchers.IO) {
        try {
            val result = api.searchIssues(query, page, 30)
            ApiResult.Success(result)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getRepoLanguages(owner: String, repo: String): ApiResult<Map<String, Long>> = withContext(Dispatchers.IO) {
        try {
            val map = api.getRepoLanguages(owner, repo)
            ApiResult.Success(map)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getCollaborators(owner: String, repo: String): ApiResult<List<Collaborator>> = withContext(Dispatchers.IO) {
        try {
            val list = api.getCollaborators(owner, repo)
            ApiResult.Success(list)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun addCollaborator(owner: String, repo: String, username: String, permission: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.addCollaborator(owner, repo, username, AddCollaboratorRequest(permission))
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun removeCollaborator(owner: String, repo: String, username: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.removeCollaborator(owner, repo, username)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getWebhooks(owner: String, repo: String): ApiResult<List<Webhook>> = withContext(Dispatchers.IO) {
        try {
            val list = api.getWebhooks(owner, repo)
            ApiResult.Success(list)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun createWebhook(owner: String, repo: String, req: CreateWebhookRequest): ApiResult<Webhook> = withContext(Dispatchers.IO) {
        try {
            val hook = api.createWebhook(owner, repo, req)
            ApiResult.Success(hook)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun deleteWebhook(owner: String, repo: String, hookId: Long): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteWebhook(owner, repo, hookId)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getReleases(owner: String, repo: String, page: Int = 1): ApiResult<List<Release>> = withContext(Dispatchers.IO) {
        try {
            val list = api.getReleases(owner, repo, page)
            ApiResult.Success(list)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun createRelease(owner: String, repo: String, req: CreateReleaseRequest): ApiResult<Release> = withContext(Dispatchers.IO) {
        try {
            val release = api.createRelease(owner, repo, req)
            ApiResult.Success(release)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun deleteRelease(owner: String, repo: String, releaseId: Long): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteRelease(owner, repo, releaseId)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getUserProfile(username: String): ApiResult<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val user = api.getUser(username)
            ApiResult.Success(user)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun getUserPublicRepos(username: String, page: Int = 1): ApiResult<List<Repository>> = withContext(Dispatchers.IO) {
        try {
            val repos = api.getUserPublicRepos(username, page)
            ApiResult.Success(repos)
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun checkFollowing(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = api.checkFollowing(username)
            resp.code() == 204
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleFollow(username: String, currentlyFollowing: Boolean): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (currentlyFollowing) {
                api.unfollowUser(username)
                ApiResult.Success(false)
            } else {
                api.followUser(username)
                ApiResult.Success(true)
            }
        } catch (e: Exception) {
            parseError(e)
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        database.userDao().clearUser()
        database.repoDao().clearRepos()
        database.notificationDao().clearNotifications()
        database.searchDao().clearSearches()
    }
}
