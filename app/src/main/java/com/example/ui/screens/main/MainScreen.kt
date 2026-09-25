package com.example.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.BottomTab
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.profile.UserProfileScreen
import com.example.ui.screens.repo.RepositoriesScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.viewmodel.PrimeRepoViewModel

@Composable
fun MainScreen(
    viewModel: PrimeRepoViewModel,
    onNavigateToCreateRepo: () -> Unit,
    onNavigateToRepoDetail: (owner: String, repo: String) -> Unit,
    onNavigateToGists: () -> Unit,
    onNavigateToUserProfile: (username: String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route
    val unreadNotifications by viewModel.unreadNotificationsCount.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomTab.values().forEach { tab ->
                    val isSelected = currentDestination == tab.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentDestination != tab.route) {
                                bottomNavController.navigate(tab.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            if (tab == BottomTab.NOTIFICATIONS && unreadNotifications > 0) {
                                BadgedBox(badge = { Badge { Text("$unreadNotifications") } }) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomTab.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomTab.HOME.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCreateRepo = onNavigateToCreateRepo,
                    onNavigateToGists = onNavigateToGists,
                    onNavigateToSearch = {
                        bottomNavController.navigate(BottomTab.SEARCH.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToNotifications = {
                        bottomNavController.navigate(BottomTab.NOTIFICATIONS.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            composable(BottomTab.REPOSITORIES.route) {
                RepositoriesScreen(
                    viewModel = viewModel,
                    onRepoClick = onNavigateToRepoDetail,
                    onCreateRepoClick = onNavigateToCreateRepo
                )
            }

            composable(BottomTab.NOTIFICATIONS.route) {
                NotificationsScreen(
                    viewModel = viewModel,
                    onRepoClick = onNavigateToRepoDetail
                )
            }

            composable(BottomTab.SEARCH.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onRepoClick = onNavigateToRepoDetail,
                    onUserClick = onNavigateToUserProfile
                )
            }

            composable(BottomTab.PROFILE.route) {
                UserProfileScreen(
                    username = "",
                    viewModel = viewModel,
                    onBack = null,
                    onEditProfileClick = onNavigateToEditProfile,
                    onRepoClick = onNavigateToRepoDetail,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}
