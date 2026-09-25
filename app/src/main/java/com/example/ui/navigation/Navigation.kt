package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object TokenEntry : Screen("token_entry")
    object Main : Screen("main")
    object CreateRepo : Screen("create_repo")
    object Settings : Screen("settings")
    object EditProfile : Screen("edit_profile")
    object Gists : Screen("gists")

    // Repo scoped routes
    object RepoDetail : Screen("repo/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "repo/$owner/$repo"
    }

    object CodeBrowser : Screen("code_browser/{owner}/{repo}?path={path}") {
        fun createRoute(owner: String, repo: String, path: String = "") = "code_browser/$owner/$repo?path=$path"
    }

    object FileViewer : Screen("file_viewer/{owner}/{repo}?path={path}&name={name}") {
        fun createRoute(owner: String, repo: String, path: String, name: String) =
            "file_viewer/$owner/$repo?path=$path&name=$name"
    }

    object FileEdit : Screen("file_edit/{owner}/{repo}?path={path}&sha={sha}") {
        fun createRoute(owner: String, repo: String, path: String = "", sha: String = "") =
            "file_edit/$owner/$repo?path=$path&sha=$sha"
    }

    object Branches : Screen("branches/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "branches/$owner/$repo"
    }

    object Commits : Screen("commits/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "commits/$owner/$repo"
    }

    object Issues : Screen("issues/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "issues/$owner/$repo"
    }

    object PullRequests : Screen("pull_requests/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "pull_requests/$owner/$repo"
    }

    object Releases : Screen("releases/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "releases/$owner/$repo"
    }

    object Collaborators : Screen("collaborators/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "collaborators/$owner/$repo"
    }

    object Webhooks : Screen("webhooks/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "webhooks/$owner/$repo"
    }

    object UserProfile : Screen("user_profile/{username}") {
        fun createRoute(username: String) = "user_profile/$username"
    }
}

enum class BottomTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home_tab", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    REPOSITORIES("repos_tab", "Repositories", Icons.Filled.Folder, Icons.Outlined.Folder),
    NOTIFICATIONS("notifications_tab", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    SEARCH("search_tab", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    PROFILE("profile_tab", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}
