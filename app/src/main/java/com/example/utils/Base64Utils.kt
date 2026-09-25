package com.example.utils

import android.util.Base64

object Base64Utils {

    fun encodeUtf8(plainText: String): String {
        return Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun decodeUtf8(base64Text: String): String {
        return try {
            val cleaned = base64Text.replace("\n", "").replace("\r", "").trim()
            val bytes = Base64.decode(cleaned, Base64.DEFAULT)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            base64Text
        }
    }
}
