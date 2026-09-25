package com.example.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UpdateUserRequest
import com.example.data.repository.ApiResult
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.HapticUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var bio by remember { mutableStateOf(currentUser?.bio ?: "") }
    var company by remember { mutableStateOf(currentUser?.company ?: "") }
    var location by remember { mutableStateOf(currentUser?.location ?: "") }
    var blog by remember { mutableStateOf(currentUser?.blog ?: "") }
    var twitter by remember { mutableStateOf(currentUser?.twitter_username ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                HapticUtils.performConfirm(context)
                                val req = UpdateUserRequest(
                                    name = name.ifBlank { null },
                                    bio = bio.ifBlank { null },
                                    company = company.ifBlank { null },
                                    location = location.ifBlank { null },
                                    blog = blog.ifBlank { null },
                                    twitter_username = twitter.ifBlank { null }
                                )
                                when (val res = viewModel.repository.updateUser(req)) {
                                    is ApiResult.Success -> {
                                        viewModel.postMessage("Profile updated successfully")
                                        viewModel.loadUserProfileAndInitialData()
                                        onBack()
                                    }
                                    is ApiResult.Error -> viewModel.postMessage("Update failed: ${res.message}")
                                    is ApiResult.Loading -> {}
                                }
                                isSaving = false
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Check, contentDescription = "Save profile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = blog,
                onValueChange = { blog = it },
                label = { Text("Website / Blog URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = twitter,
                onValueChange = { twitter = it },
                label = { Text("Twitter / X Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Avatar pictures are synced directly from your GitHub or Gravatar account settings on github.com.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
