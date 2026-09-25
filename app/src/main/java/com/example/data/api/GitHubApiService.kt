package com.example.data.api

import com.example.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    // User Profile
    @GET("user")
    suspend fun getCurrentUser(): GitHubUser

    @PATCH("user")
    suspend fun updateUser(@Body req: UpdateUserRequest): GitHubUser

    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GitHubUser

    // Repositories
    @GET("user/repos")
    suspend fun getUserRepos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30,
        @Query("sort") sort: String? = "updated",
        @Query("direction") direction: String? = "desc",
        @Query("visibility") visibility: String? = "all",
        @Query("affiliation") affiliation: String? = "owner,collaborator,organization_member"
    ): List<Repository>

    @GET("users/{username}/repos")
    suspend fun getUserPublicRepos(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<Repository>

    @POST("user/repos")
    suspend fun createRepo(@Body req: CreateRepoRequest): Repository

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Repository

    @PATCH("repos/{owner}/{repo}")
    suspend fun updateRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body req: UpdateRepoRequest
    ): Repository

    @DELETE("repos/{owner}/{repo}")
    suspend fun deleteRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @GET("user/starred/{owner}/{repo}")
    suspend fun checkStar(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstarRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/forks")
    suspend fun forkRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Repository

    // Contents & Code Browser
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): ResponseBody

    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ref") ref: String? = null
    ): ContentItem

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body req: CreateUpdateFileRequest
    ): ResponseBody

    @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/{path}", hasBody = true)
    suspend fun deleteFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body req: DeleteFileRequest
    ): ResponseBody

    // Branches
    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): List<Branch>

    @GET("repos/{owner}/{repo}/git/ref/{ref}")
    suspend fun getRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "ref", encoded = true) ref: String
    ): GitRef

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body req: CreateRefRequest
    ): GitRef

    @DELETE("repos/{owner}/{repo}/git/refs/{ref}")
    suspend fun deleteRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "ref", encoded = true) ref: String
    ): Response<Unit>

    // Commits
    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") sha: String? = null,
        @Query("path") path: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<CommitItem>

    @GET("repos/{owner}/{repo}/commits/{ref}")
    suspend fun getCommitDetail(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("ref") ref: String
    ): CommitItem

    // Issues
    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String? = "open",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<Issue>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body req: CreateIssueRequest
    ): Issue

    @PATCH("repos/{owner}/{repo}/issues/{issue_number}")
    suspend fun updateIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body req: UpdateIssueRequest
    ): Issue

    @GET("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun getIssueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int
    ): List<IssueComment>

    @POST("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun createIssueComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body req: CreateCommentRequest
    ): IssueComment

    // Pull Requests
    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String? = "open",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<PullRequest>

    @GET("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun getPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int
    ): PullRequest

    @PUT("repos/{owner}/{repo}/pulls/{pull_number}/merge")
    suspend fun mergePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body req: MergePullRequestRequest
    ): MergeResult

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(
        @Query("all") all: Boolean = true,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<NotificationItem>

    @PATCH("notifications/threads/{thread_id}")
    suspend fun markNotificationRead(
        @Path("thread_id") threadId: String
    ): Response<Unit>

    @PUT("notifications")
    suspend fun markAllNotificationsRead(
        @Body req: MarkAllReadRequest = MarkAllReadRequest()
    ): Response<Unit>

    // Gists
    @GET("gists")
    suspend fun getGists(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<Gist>

    @GET("gists/{gist_id}")
    suspend fun getGist(
        @Path("gist_id") gistId: String
    ): Gist

    @POST("gists")
    suspend fun createGist(
        @Body req: CreateGistRequest
    ): Gist

    @PATCH("gists/{gist_id}")
    suspend fun updateGist(
        @Path("gist_id") gistId: String,
        @Body req: UpdateGistRequest
    ): Gist

    @DELETE("gists/{gist_id}")
    suspend fun deleteGist(
        @Path("gist_id") gistId: String
    ): Response<Unit>

    // Search
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30,
        @Query("sort") sort: String? = null
    ): SearchResult<Repository>

    @GET("search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): SearchResult<GitHubUser>

    @GET("search/code")
    suspend fun searchCode(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): SearchResult<CodeSearchItem>

    @GET("search/issues")
    suspend fun searchIssues(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): SearchResult<Issue>

    // Languages
    @GET("repos/{owner}/{repo}/languages")
    suspend fun getRepoLanguages(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Map<String, Long>

    // Collaborators
    @GET("repos/{owner}/{repo}/collaborators")
    suspend fun getCollaborators(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<Collaborator>

    @PUT("repos/{owner}/{repo}/collaborators/{username}")
    suspend fun addCollaborator(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("username") username: String,
        @Body req: AddCollaboratorRequest
    ): Response<Unit>

    @DELETE("repos/{owner}/{repo}/collaborators/{username}")
    suspend fun removeCollaborator(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("username") username: String
    ): Response<Unit>

    // Webhooks
    @GET("repos/{owner}/{repo}/hooks")
    suspend fun getWebhooks(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<Webhook>

    @POST("repos/{owner}/{repo}/hooks")
    suspend fun createWebhook(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body req: CreateWebhookRequest
    ): Webhook

    @DELETE("repos/{owner}/{repo}/hooks/{hook_id}")
    suspend fun deleteWebhook(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("hook_id") hookId: Long
    ): Response<Unit>

    // Releases
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<Release>

    @POST("repos/{owner}/{repo}/releases")
    suspend fun createRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body req: CreateReleaseRequest
    ): Release

    @DELETE("repos/{owner}/{repo}/releases/{release_id}")
    suspend fun deleteRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("release_id") releaseId: Long
    ): Response<Unit>

    // Social / Follow
    @GET("user/following/{username}")
    suspend fun checkFollowing(
        @Path("username") username: String
    ): Response<Unit>

    @PUT("user/following/{username}")
    suspend fun followUser(
        @Path("username") username: String
    ): Response<Unit>

    @DELETE("user/following/{username}")
    suspend fun unfollowUser(
        @Path("username") username: String
    ): Response<Unit>

    @GET("users/{username}/followers")
    suspend fun getFollowers(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GitHubUser>

    @GET("users/{username}/following")
    suspend fun getFollowing(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GitHubUser>
}
