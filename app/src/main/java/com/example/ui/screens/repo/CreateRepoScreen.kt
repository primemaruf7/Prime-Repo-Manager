package com.example.ui.screens.repo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Repository
import com.example.ui.viewmodel.PrimeRepoViewModel
import com.example.utils.HapticUtils

private val GitignoreTemplates = listOf("None", "Android", "Node", "Python", "Java", "Kotlin", "Go", "Rust", "C++")
private val LicenseTemplates = listOf("None", "mit", "apache-2.0", "gpl-3.0", "bsd-3-clause", "isc", "unlicense")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRepoScreen(
    viewModel: PrimeRepoViewModel,
    onBack: () -> Unit,
    onRepoCreated: (owner: String, repo: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var initReadme by remember { mutableStateOf(true) }
    var selectedGitignore by remember { mutableStateOf("None") }
    var selectedLicense by remember { mutableStateOf("None") }

    var gitignoreMenuOpen by remember { mutableStateOf(false) }
    var licenseMenuOpen by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Repository", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Repo Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.filter { c -> c.isLetterOrDigit() || c == '-' || c == '_' || c == '.' } },
                label = { Text("Repository name *") },
                placeholder = { Text("e.g. awesome-kotlin-app") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                placeholder = { Text("Brief description of your project") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Visibility Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Visibility", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPrivate = false }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !isPrivate, onClick = { isPrivate = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Public", fontWeight = FontWeight.SemiBold)
                            Text("Anyone on the internet can see this repository.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPrivate = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isPrivate, onClick = { isPrivate = true })
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFD29922))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Private", fontWeight = FontWeight.SemiBold)
                            Text("You choose who can see and commit to this repository.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Initialize with README switch
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Add a README file", fontWeight = FontWeight.SemiBold)
                        Text("This is where you can write a long description for your project.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = initReadme, onCheckedChange = { initReadme = it })
                }
            }

            // .gitignore Template Selector
            Box {
                OutlinedTextField(
                    value = if (selectedGitignore == "None") "None (.gitignore)" else "$selectedGitignore (.gitignore)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Add .gitignore template") },
                    trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { gitignoreMenuOpen = true }
                )
                DropdownMenu(
                    expanded = gitignoreMenuOpen,
                    onDismissRequest = { gitignoreMenuOpen = false }
                ) {
                    GitignoreTemplates.forEach { template ->
                        DropdownMenuItem(
                            text = { Text(template) },
                            onClick = {
                                selectedGitignore = template
                                gitignoreMenuOpen = false
                            }
                        )
                    }
                }
            }

            // License Selector
            Box {
                OutlinedTextField(
                    value = if (selectedLicense == "None") "None (License)" else "${selectedLicense.uppercase()} License",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Choose a license") },
                    trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { licenseMenuOpen = true }
                )
                DropdownMenu(
                    expanded = licenseMenuOpen,
                    onDismissRequest = { licenseMenuOpen = false }
                ) {
                    LicenseTemplates.forEach { license ->
                        DropdownMenuItem(
                            text = { Text(if (license == "None") "None" else license.uppercase()) },
                            onClick = {
                                selectedLicense = license
                                licenseMenuOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        isSubmitting = true
                        HapticUtils.performConfirm(context)
                        viewModel.createRepository(
                            name = name.trim(),
                            description = description.trim(),
                            isPrivate = isPrivate,
                            initReadme = initReadme,
                            gitignore = if (selectedGitignore == "None") null else selectedGitignore,
                            license = if (selectedLicense == "None") null else selectedLicense,
                            onSuccess = { createdRepo ->
                                isSubmitting = false
                                val owner = createdRepo.owner?.login ?: "user"
                                onRepoCreated(owner, createdRepo.name)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = name.isNotBlank() && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Creating Repository...")
                } else {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Repository", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
