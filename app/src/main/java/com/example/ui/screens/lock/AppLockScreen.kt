package com.example.ui.screens.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.security.AppLockManager
import com.example.ui.components.PrimeRepoLogo
import com.example.utils.HapticUtils

@Composable
fun AppLockScreen(
    appLockManager: AppLockManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        (context as? FragmentActivity)?.let { activity ->
            appLockManager.showBiometricPrompt(
                activity = activity,
                onSuccess = onUnlocked,
                onError = { /* fallback to PIN available on screen */ }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PrimeRepoLogo(size = 80.dp)

            Text(
                text = "Prime Repo Locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Please authenticate to access your GitHub repositories and credentials.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    (context as? FragmentActivity)?.let { activity ->
                        HapticUtils.performConfirm(context)
                        appLockManager.showBiometricPrompt(
                            activity = activity,
                            onSuccess = onUnlocked,
                            onError = { /* stay on lock */ }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock with Biometrics")
            }

            // PIN fallback
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Or enter backup PIN", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        placeholder = { Text("Default: 1234") },
                        singleLine = true,
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (appLockManager.verifyPin(pinInput)) {
                                HapticUtils.performSuccess(context)
                                appLockManager.unlock()
                                onUnlocked()
                            } else {
                                HapticUtils.performWarning(context)
                                pinError = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlock with PIN")
                    }
                }
            }
        }
    }
}
