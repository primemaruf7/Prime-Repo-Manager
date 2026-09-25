package com.example.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockTimeout(val millis: Long, val label: String) {
    IMMEDIATELY(0L, "Immediately"),
    ONE_MINUTE(60_000L, "1 minute"),
    FIVE_MINUTES(300_000L, "5 minutes")
}

class AppLockManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("prime_repo_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_LOCK_TIMEOUT = "app_lock_timeout_ms"
        private const val KEY_BACKUP_PIN = "app_backup_pin"
    }

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L

    init {
        // If app lock is enabled, start locked
        if (isLockEnabled()) {
            _isLocked.value = true
        }
    }

    fun isLockEnabled(): Boolean = prefs.getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
        if (!enabled) {
            _isLocked.value = false
        }
    }

    fun getTimeout(): LockTimeout {
        val ms = prefs.getLong(KEY_LOCK_TIMEOUT, 0L)
        return LockTimeout.values().find { it.millis == ms } ?: LockTimeout.IMMEDIATELY
    }

    fun setTimeout(timeout: LockTimeout) {
        prefs.edit().putLong(KEY_LOCK_TIMEOUT, timeout.millis).apply()
    }

    fun setBackupPin(pin: String) {
        prefs.edit().putString(KEY_BACKUP_PIN, pin).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saved = prefs.getString(KEY_BACKUP_PIN, null) ?: return pin == "1234"
        return saved == pin
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun lockNow() {
        if (isLockEnabled()) {
            _isLocked.value = true
        }
    }

    fun onAppForegrounded() {
        if (!isLockEnabled()) return
        val elapsed = System.currentTimeMillis() - backgroundTimestamp
        val timeout = getTimeout().millis
        if (backgroundTimestamp > 0 && elapsed >= timeout) {
            _isLocked.value = true
        }
    }

    fun onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()
    }

    fun canAuthenticateBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Prime Repo")
            .setSubtitle("Authenticate using biometrics or device PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    _isLocked.value = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed. Please try again.")
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
