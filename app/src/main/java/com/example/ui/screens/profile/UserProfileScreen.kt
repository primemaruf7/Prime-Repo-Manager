package com.example.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GitHubUser
import com.example.data.model.Repository
import com.example.data.repository.ApiResult
import com.example.ui.components.RepositoryCard
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

private val ProfileCardColor = Color(0xFF211F26)
private val ProfileSecondary = Color(0xFFA8A5AE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    username: String,
    viewModel: PrimeRepoViewModel,
    onBack: (() -> Unit)? = null,
    onEditProfileClick: (() -> Unit)? = null,
    onRepoClick: (owner: String, repo: String) -> Unit,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val loggedInUser by viewModel.currentUser.collectAsState()

    var user by remember {
        mutableStateOf<GitHubUser?>(null)
    }

    var repos by remember {
        mutableStateOf<List<Repository>>(emptyList())
    }

    var isFollowing by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    val isSelf = remember(
        loggedInUser?.login,
        username
    ) {
        loggedInUser?.login.equals(
            username,
            ignoreCase = true
        ) || username.isEmpty()
    }

    LaunchedEffect(
        username,
        loggedInUser?.login
    ) {
        isLoading = true

        val targetUser = if (
            isSelf && loggedInUser != null
        ) {
            loggedInUser!!.login
        } else {
            username
        }

        if (targetUser.isNotBlank()) {

            when (
                val result =
                    viewModel.repository.getUserProfile(targetUser)
            ) {
                is ApiResult.Success -> {
                    user = result.data
                }

                is ApiResult.Error -> {
                    if (isSelf) {
                        user = loggedInUser
                    } else {
                        viewModel.postMessage(
                            result.message
                        )
                    }
                }

                is ApiResult.Loading -> Unit
            }

            if (!isSelf) {
                isFollowing =
                    viewModel.repository.checkFollowing(
                        targetUser
                    )
            }

            when (
                val result =
                    viewModel.repository.getUserPublicRepos(
                        targetUser
                    )
            ) {
                is ApiResult.Success -> {
                    repos = result.data
                }

                is ApiResult.Error -> Unit

                is ApiResult.Loading -> Unit
            }
        }

        isLoading = false
    }

    val backgroundColor =
        MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSelf) {
                            "My Profile"
                        } else {
                            "@$username"
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },

                actions = {

                    if (
                        isSelf &&
                        onEditProfileClick != null
                    ) {
                        IconButton(
                            onClick = onEditProfileClick
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Outlined.Edit,
                                contentDescription =
                                    "Edit profile",
                                modifier =
                                    Modifier.size(22.dp)
                            )
                        }
                    }

                    if (
                        isSelf &&
                        onNavigateToSettings != null
                    ) {
                        IconButton(
                            onClick =
                                onNavigateToSettings
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Outlined.Settings,
                                contentDescription =
                                    "Settings",
                                modifier =
                                    Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            user?.html_url?.let { url ->
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )
                                context.startActivity(intent)
                            }
                        }
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.OpenInBrowser,
                            contentDescription =
                                "Open in GitHub",
                            modifier =
                                Modifier.size(22.dp)
                        )
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            backgroundColor,
                        scrolledContainerColor =
                            backgroundColor
                    )
            )
        }
    ) { innerPadding ->

        when {

            isLoading -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment =
                        Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            user == null -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.PersonOff,
                            contentDescription = null,
                            modifier =
                                Modifier.size(42.dp),
                            tint = ProfileSecondary
                        )

                        Text(
                            text = "User not found.",
                            color = ProfileSecondary
                        )
                    }
                }
            }

            else -> {

                val currentUser = user!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),

                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = 100.dp
                    ),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    item(
                        key = "profile_header",
                        contentType = "profile_header"
                    ) {

                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(16.dp),
                            color =
                                ProfileCardColor
                        ) {
                            Column(
                                modifier =
                                    Modifier.padding(18.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(14.dp)
                            ) {

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    AsyncImage(
                                        model =
                                            currentUser.avatar_url,
                                        contentDescription =
                                            "Avatar",
                                        modifier =
                                            Modifier
                                                .size(78.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = 2.dp,
                                                    color =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary,
                                                    shape =
                                                        CircleShape
                                                ),
                                        contentScale =
                                            ContentScale.Crop
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.width(14.dp)
                                    )

                                    Column(
                                        modifier =
                                            Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text =
                                                currentUser.name
                                                    ?: currentUser.login,
                                            fontSize = 21.sp,
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.height(3.dp)
                                        )

                                        Text(
                                            text =
                                                "@${currentUser.login}",
                                            fontSize = 14.sp,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.height(7.dp)
                                        )

                                        Row(
                                            horizontalArrangement =
                                                Arrangement.spacedBy(14.dp)
                                        ) {

                                            Text(
                                                text =
                                                    "${currentUser.followers} followers",
                                                fontSize = 12.sp,
                                                fontWeight =
                                                    FontWeight.SemiBold
                                            )

                                            Text(
                                                text =
                                                    "${currentUser.following} following",
                                                fontSize = 12.sp,
                                                fontWeight =
                                                    FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                if (
                                    !currentUser.bio
                                        .isNullOrBlank()
                                ) {
                                    Text(
                                        text =
                                            currentUser.bio!!,
                                        fontSize = 14.sp,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
                                }

                                Column(
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    if (
                                        !currentUser.company
                                            .isNullOrBlank()
                                    ) {
                                        ProfileMetaRow(
                                            icon =
                                                Icons.Outlined.Business,
                                            text =
                                                currentUser.company!!
                                        )
                                    }

                                    if (
                                        !currentUser.location
                                            .isNullOrBlank()
                                    ) {
                                        ProfileMetaRow(
                                            icon =
                                                Icons.Outlined.LocationOn,
                                            text =
                                                currentUser.location!!
                                        )
                                    }

                                    if (
                                        !currentUser.blog
                                            .isNullOrBlank()
                                    ) {
                                        ProfileMetaRow(
                                            icon =
                                                Icons.Outlined.Link,
                                            text =
                                                currentUser.blog!!,
                                            tint =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                        )
                                    }
                                }

                                if (!isSelf) {

                                    Button(
                                        onClick = {
                                            if (isFollowing) {
                                                // handled below
                                            }

                                            coroutineScope.launch {

                                                HapticUtils
                                                    .performConfirm(
                                                        context
                                                    )

                                                when (
                                                    val result =
                                                        viewModel
                                                            .repository
                                                            .toggleFollow(
                                                                currentUser.login,
                                                                isFollowing
                                                            )
                                                ) {

                                                    is ApiResult.Success -> {

                                                        isFollowing =
                                                            result.data

                                                        viewModel
                                                            .postMessage(
                                                                if (
                                                                    isFollowing
                                                                ) {
                                                                    "Followed @${currentUser.login}"
                                                                } else {
                                                                    "Unfollowed @${currentUser.login}"
                                                                }
                                                            )
                                                    }

                                                    is ApiResult.Error -> {
                                                        viewModel
                                                            .postMessage(
                                                                result.message
                                                            )
                                                    }

                                                    is ApiResult.Loading ->
                                                        Unit
                                                }
                                            }
                                        },

                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        shape =
                                            RoundedCornerShape(12.dp),

                                        colors =
                                            if (isFollowing) {
                                                ButtonDefaults
                                                    .outlinedButtonColors()
                                            } else {
                                                ButtonDefaults
                                                    .buttonColors()
                                            }
                                    ) {

                                        Icon(
                                            imageVector =
                                                if (isFollowing) {
                                                    Icons.Outlined.PersonRemove
                                                } else {
                                                    Icons.Outlined.PersonAdd
                                                },
                                            contentDescription =
                                                null,
                                            modifier =
                                                Modifier.size(18.dp)
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.width(7.dp)
                                        )

                                        Text(
                                            text =
                                                if (isFollowing) {
                                                    "Unfollow"
                                                } else {
                                                    "Follow"
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(
                        key = "profile_info",
                        contentType = "profile_info"
                    ) {

                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(14.dp),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                        ) {

                            Row(
                                modifier =
                                    Modifier.padding(13.dp),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.Info,
                                    contentDescription =
                                        null,
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    modifier =
                                        Modifier.size(20.dp)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(10.dp)
                                )

                                Text(
                                    text =
                                        "Showing recent public repositories. GitHub pinned repositories are available through the web profile.",
                                    fontSize = 12.sp,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    item(
                        key = "repository_header",
                        contentType = "repository_header"
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 4.dp,
                                    bottom = 2.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    "Public Repositories",
                                fontSize = 18.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                modifier =
                                    Modifier.weight(1f)
                            )

                            Surface(
                                shape =
                                    RoundedCornerShape(10.dp),
                                color =
                                    ProfileCardColor
                            ) {
                                Text(
                                    text = "${repos.size}",
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 5.dp
                                        ),
                                    fontSize = 12.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )
                            }
                        }
                    }

                    if (repos.isEmpty()) {

                        item(
                            key = "empty_repositories",
                            contentType = "empty_repositories"
                        ) {

                            Surface(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(14.dp),
                                color =
                                    ProfileCardColor
                            ) {

                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.FolderOpen,
                                        contentDescription =
                                            null,
                                        modifier =
                                            Modifier.size(36.dp),
                                        tint =
                                            ProfileSecondary
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(8.dp)
                                    )

                                    Text(
                                        text =
                                            "No public repositories",
                                        fontWeight =
                                            FontWeight.SemiBold
                                    )

                                    Text(
                                        text =
                                            "This user doesn't have any public repositories.",
                                        fontSize = 12.sp,
                                        color =
                                            ProfileSecondary
                                    )
                                }
                            }
                        }

                    } else {

                        items(
                            items = repos,
                            key = { repo -> repo.id },
                            contentType = { "repository" }
                        ) { repo ->

                            RepositoryCard(
                                repo = repo,
                                onClick = {

                                    val ownerLogin =
                                        repo.owner?.login
                                            ?: currentUser.login

                                    onRepoClick(
                                        ownerLogin,
                                        repo.name
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color =
        MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = tint
        )

        Text(
            text = text,
            fontSize = 12.sp,
            color = tint,
            maxLines = 1
        )
    }
}