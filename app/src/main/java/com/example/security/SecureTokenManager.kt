package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "prime_repo_vault"
        private const val KEY_ENCRYPTED_TOKEN = "enc_github_pat"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "PrimeRepoMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // AndroidKeyStore init error handled gracefully
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    @Synchronized
    fun saveToken(token: String): Boolean {
        if (token.isBlank()) return false
        return try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(token.trim().toByteArray(Charsets.UTF_8))
            
            // Combine IV + ciphertext
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            val base64Payload = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString(KEY_ENCRYPTED_TOKEN, base64Payload).commit()
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    fun getToken(): String? {
        val encodedPayload = prefs.getString(KEY_ENCRYPTED_TOKEN, null) ?: return null
        return try {
            val combined = Base64.decode(encodedPayload, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return null

            val iv = ByteArray(GCM_IV_LENGTH)
            val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)

            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }

    @Synchronized
    fun clearToken() {
        prefs.edit().remove(KEY_ENCRYPTED_TOKEN).commit()
    }

    /**
     * Utility to redact token strings when creating user-facing logs or messages
     */
    fun redactToken(value: String): String {
        return if (value.length > 8) {
            "${value.take(4)}...${value.takeLast(4)}"
        } else {
            "***"
        }
    }
}
