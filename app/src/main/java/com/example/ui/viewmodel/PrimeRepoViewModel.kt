package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.NetworkModule
import com.example.data.api.RateLimitInfo
import com.example.data.api.RateLimitManager
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ApiResult
import com.example.data.repository.GitHubRepository
import com.example.security.AppLockManager
import com.example.security.SecureTokenManager
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: GitHubUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class PrimeRepoViewModel(application: Application) : AndroidViewModel(application) {

    val tokenManager = SecureTokenManager(application)
    val appLockManager = AppLockManager(application)
    private val database = AppDatabase.getInstance(application)

    private var apiService = NetworkModule.provideGitHubApiService { tokenManager.getToken() }
    var repository = GitHubRepository(apiService, database)
        private set

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<GitHubUser?>(null)
    val currentUser: StateFlow<GitHubUser?> = _currentUser.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _userRepos = MutableStateFlow<List<Repository>>(emptyList())
    val userRepos: StateFlow<List<Repository>> = _userRepos.asStateFlow()

    private val _isLoadingRepos = MutableStateFlow(false)
    val isLoadingRepos: StateFlow<Boolean> = _isLoadingRepos.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    val unreadNotificationsCount: StateFlow<Int> = _notifications.map { list ->
        list.count { it.unread }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val rateLimit: StateFlow<RateLimitInfo> = RateLimitManager.rateLimit

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _snackbarChannel = Channel<String>(Channel.BUFFERED)
    val snackbarEvents = _snackbarChannel.receiveAsFlow()

    init {
        checkExistingAuth()
    }

    private fun checkExistingAuth() {
        if (tokenManager.hasToken()) {
            loadUserProfileAndInitialData()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun postMessage(msg: String) {
        viewModelScope.launch {
            _snackbarChannel.send(msg)
        }
    }

    fun connectWithToken(token: String, onSuccess: () -> Unit) {
        if (token.isBlank()) {
            _authState.value = AuthState.Error("Token cannot be blank")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // Test token with fresh API service
            val tempApiService = NetworkModule.provideGitHubApiService { token.trim() }
            val tempRepo = GitHubRepository(tempApiService, database)

            when (val result = tempRepo.getCurrentUser()) {
                is ApiResult.Success -> {
                    val saved = tokenManager.saveToken(token.trim())
                    if (saved) {
                        // Re-initialize repository with saved token
                        apiService = NetworkModule.provideGitHubApiService { tokenManager.getToken() }
                        repository = GitHubRepository(apiService, database)
                        _currentUser.value = result.data
                        _authState.value = AuthState.Authenticated(result.data)
                        _isOffline.value = result.isOffline
                        loadDashboardData()
                        postMessage("Connected as ${result.data.login}")
                        onSuccess()
                    } else {
                        _authState.value = AuthState.Error("Failed to store token securely in Keystore")
                    }
                }
                is ApiResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            tokenManager.clearToken()
            repository.clearCache()
            _currentUser.value = null
            _userRepos.value = emptyList()
            _notifications.value = emptyList()
            _authState.value = AuthState.Idle
            postMessage("Logged out securely")
            onLoggedOut()
        }
    }

    fun loadUserProfileAndInitialData() {
        viewModelScope.launch {
            when (val res = repository.getCurrentUser()) {
                is ApiResult.Success -> {
                    _currentUser.value = res.data
                    _authState.value = AuthState.Authenticated(res.data)
                    _isOffline.value = res.isOffline
                    loadDashboardData()
                }
                is ApiResult.Error -> {
                    _authState.value = AuthState.Error(res.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun loadDashboardData() {
        loadRepositories()
        loadNotifications()
    }

    fun loadRepositories(sort: String = "updated", visibility: String = "all") {
        viewModelScope.launch {
            _isLoadingRepos.value = true
            when (val res = repository.getUserRepos(page = 1, perPage = 50, sort = sort, visibility = visibility)) {
                is ApiResult.Success -> {
                    _userRepos.value = res.data
                    _isOffline.value = res.isOffline
                }
                is ApiResult.Error -> {
                    postMessage(res.message)
                }
                is ApiResult.Loading -> {}
            }
            _isLoadingRepos.value = false
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            when (val res = repository.getNotifications(all = true, page = 1)) {
                is ApiResult.Success -> {
                    _notifications.value = res.data
                }
                is ApiResult.Error -> {}
                is ApiResult.Loading -> {}
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            when (repository.markNotificationRead(id)) {
                is ApiResult.Success -> {
                    _notifications.value = _notifications.value.map {
                        if (it.id == id) it.copy(unread = false) else it
                    }
                    postMessage("Notification marked as read")
                }
                is ApiResult.Error -> {
                    postMessage("Failed to mark as read")
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            when (repository.markAllNotificationsRead()) {
                is ApiResult.Success -> {
                    _notifications.value = _notifications.value.map { it.copy(unread = false) }
                    postMessage("All notifications marked as read")
                }
                is ApiResult.Error -> {
                    postMessage("Failed to mark all as read")
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun createRepository(
        name: String,
        description: String,
        isPrivate: Boolean,
        initReadme: Boolean,
        gitignore: String?,
        license: String?,
        onSuccess: (Repository) -> Unit
    ) {
        viewModelScope.launch {
            val req = CreateRepoRequest(
                name = name,
                description = description.ifBlank { null },
                private = isPrivate,
                auto_init = initReadme,
                gitignore_template = gitignore,
                license_template = license
            )
            when (val res = repository.createRepository(req)) {
                is ApiResult.Success -> {
                    _userRepos.value = listOf(res.data) + _userRepos.value
                    postMessage("Repository '${res.data.name}' created successfully")
                    onSuccess(res.data)
                }
                is ApiResult.Error -> {
                    postMessage("Error: ${res.message}")
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun deleteRepository(owner: String, repo: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val res = repository.deleteRepo(owner, repo)) {
                is ApiResult.Success -> {
                    _userRepos.value = _userRepos.value.filterNot { it.name.equals(repo, true) }
                    postMessage("Repository $owner/$repo deleted")
                    onSuccess()
                }
                is ApiResult.Error -> {
                    postMessage("Failed to delete repository: ${res.message}")
                }
                is ApiResult.Loading -> {}
            }
        }
    }
}
