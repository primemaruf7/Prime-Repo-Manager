package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.branches.BranchesScreen
import com.example.ui.screens.code.CodeBrowserScreen
import com.example.ui.screens.code.FileCreateEditScreen
import com.example.ui.screens.code.FileViewerScreen
import com.example.ui.screens.collaborators.CollaboratorsScreen
import com.example.ui.screens.commits.CommitsScreen
import com.example.ui.screens.gists.GistsScreen
import com.example.ui.screens.issues.IssuesScreen
import com.example.ui.screens.lock.AppLockScreen
import com.example.ui.screens.main.MainScreen
import com.example.ui.screens.profile.EditProfileScreen
import com.example.ui.screens.profile.UserProfileScreen
import com.example.ui.screens.pulls.PullRequestsScreen
import com.example.ui.screens.releases.ReleasesScreen
import com.example.ui.screens.repo.CreateRepoScreen
import com.example.ui.screens.repo.RepoDetailScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.token.TokenEntryScreen
import com.example.ui.screens.webhooks.WebhooksScreen
import com.example.ui.theme.PrimeRepoTheme
import com.example.ui.viewmodel.PrimeRepoViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : FragmentActivity() {

    private val viewModel: PrimeRepoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manage lifecycle for App Lock backgrounding
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.appLockManager.onAppForegrounded()
                Lifecycle.Event.ON_STOP -> viewModel.appLockManager.onAppBackgrounded()
                else -> {}
            }
        })

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isLocked by viewModel.appLockManager.isLocked.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                viewModel.snackbarEvents.collectLatest { msg ->
                    snackbarHostState.showSnackbar(msg)
                }
            }

            PrimeRepoTheme(themeMode = themeMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    if (isLocked) {
                        AppLockScreen(
                            appLockManager = viewModel.appLockManager,
                            onUnlocked = { viewModel.appLockManager.unlock() }
                        )
                    } else {
                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = Screen.Splash.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Splash.route) {
                                SplashScreen(
                                    viewModel = viewModel,
                                    onNavigateToMain = {
                                        navController.navigate(Screen.Main.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    },
                                    onNavigateToTokenEntry = {
                                        navController.navigate(Screen.TokenEntry.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.TokenEntry.route) {
                                TokenEntryScreen(
                                    viewModel = viewModel,
                                    onConnected = {
                                        navController.navigate(Screen.Main.route) {
                                            popUpTo(Screen.TokenEntry.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Main.route) {
                                MainScreen(
                                    viewModel = viewModel,
                                    onNavigateToCreateRepo = { navController.navigate(Screen.CreateRepo.route) },
                                    onNavigateToRepoDetail = { owner, repo ->
                                        navController.navigate(Screen.RepoDetail.createRoute(owner, repo))
                                    },
                                    onNavigateToGists = { navController.navigate(Screen.Gists.route) },
                                    onNavigateToUserProfile = { username ->
                                        navController.navigate(Screen.UserProfile.createRoute(username))
                                    },
                                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                                )
                            }

                            composable(Screen.CreateRepo.route) {
                                CreateRepoScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onRepoCreated = { owner, repo ->
                                        navController.navigate(Screen.RepoDetail.createRoute(owner, repo)) {
                                            popUpTo(Screen.CreateRepo.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(
                                route = Screen.RepoDetail.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                RepoDetailScreen(
                                    owner = owner,
                                    repoName = repo,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToCodeBrowser = { o, r ->
                                        navController.navigate(Screen.CodeBrowser.createRoute(o, r))
                                    },
                                    onNavigateToBranches = { o, r ->
                                        navController.navigate(Screen.Branches.createRoute(o, r))
                                    },
                                    onNavigateToCommits = { o, r ->
                                        navController.navigate(Screen.Commits.createRoute(o, r))
                                    },
                                    onNavigateToIssues = { o, r ->
                                        navController.navigate(Screen.Issues.createRoute(o, r))
                                    },
                                    onNavigateToPullRequests = { o, r ->
                                        navController.navigate(Screen.PullRequests.createRoute(o, r))
                                    },
                                    onNavigateToReleases = { o, r ->
                                        navController.navigate(Screen.Releases.createRoute(o, r))
                                    },
                                    onNavigateToCollaborators = { o, r ->
                                        navController.navigate(Screen.Collaborators.createRoute(o, r))
                                    },
                                    onNavigateToWebhooks = { o, r ->
                                        navController.navigate(Screen.Webhooks.createRoute(o, r))
                                    }
                                )
                            }

                            composable(
                                route = Screen.CodeBrowser.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType },
                                    navArgument("path") {
                                        type = NavType.StringType
                                        defaultValue = ""
                                    }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                val path = backStackEntry.arguments?.getString("path") ?: ""
                                CodeBrowserScreen(
                                    owner = owner,
                                    repoName = repo,
                                    initialPath = path,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToFileViewer = { o, r, p, name ->
                                        navController.navigate(Screen.FileViewer.createRoute(o, r, p, name))
                                    },
                                    onNavigateToCreateFile = { o, r, p ->
                                        navController.navigate(Screen.FileEdit.createRoute(o, r, p))
                                    }
                                )
                            }

                            composable(
                                route = Screen.FileViewer.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType },
                                    navArgument("path") { type = NavType.StringType },
                                    navArgument("name") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                val path = backStackEntry.arguments?.getString("path") ?: ""
                                val name = backStackEntry.arguments?.getString("name") ?: ""
                                FileViewerScreen(
                                    owner = owner,
                                    repoName = repo,
                                    path = path,
                                    fileName = name,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onEditFile = { o, r, p, sha ->
                                        navController.navigate(Screen.FileEdit.createRoute(o, r, p, sha))
                                    }
                                )
                            }

                            composable(
                                route = Screen.FileEdit.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType },
                                    navArgument("path") {
                                        type = NavType.StringType
                                        defaultValue = ""
                                    },
                                    navArgument("sha") {
                                        type = NavType.StringType
                                        defaultValue = ""
                                    }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                val path = backStackEntry.arguments?.getString("path") ?: ""
                                val sha = backStackEntry.arguments?.getString("sha") ?: ""
                                FileCreateEditScreen(
                                    owner = owner,
                                    repoName = repo,
                                    initialPath = path,
                                    initialSha = sha,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.Branches.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                BranchesScreen(
                                    owner = owner,
                                    repoName = repo,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.Commits.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                CommitsScreen(
                                    owner = owner,
                                    repoName = repo,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.Issues.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                IssuesScreen(
                                    owner = owner,
                                    repoName = repo,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.PullRequests.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                PullRequestsScreen(
                                    owner = owner,
                                    repoName = repo,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.Releases.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                ReleasesScreen(
                                    owner = owner,
                                    repoName = repo,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.Collaborators.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                CollaboratorsScreen(
                                    owner = owner,
                                    repoName = repo,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.Webhooks.route,
                                arguments = listOf(
                                    navArgument("owner") { type = NavType.StringType },
                                    navArgument("repo") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                                WebhooksScreen(
                                    owner = owner,
                                    repoName = repo,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.UserProfile.route,
                                arguments = listOf(
                                    navArgument("username") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val username = backStackEntry.arguments?.getString("username") ?: ""
                                UserProfileScreen(
                                    username = username,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onRepoClick = { o, r ->
                                        navController.navigate(Screen.RepoDetail.createRoute(o, r))
                                    }
                                )
                            }

                            composable(Screen.EditProfile.route) {
                                EditProfileScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Gists.route) {
                                GistsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onLoggedOut = {
                                        navController.navigate(Screen.TokenEntry.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
