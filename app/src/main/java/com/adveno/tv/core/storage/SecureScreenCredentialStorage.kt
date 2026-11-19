package com.adveno.tv.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android Keystore-backed secure implementation of [ScreenCredentialStorage].
 * Uses AES-256 GCM encryption via Jetpack Security (EncryptedSharedPreferences).
 */
class SecureScreenCredentialStorage(
    context: Context
) : ScreenCredentialStorage {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for devices with corrupted keystores or unsupported security providers
        context.getSharedPreferences(FALLBACK_PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    override fun saveCredential(credential: String, screenId: String?) {
        prefs.edit()
            .putString(KEY_SCREEN_CREDENTIAL, credential)
            .apply {
                if (screenId != null) {
                    putString(KEY_SCREEN_ID, screenId)
                }
            }
            .apply()
    }

    override fun getCredential(): String? {
        return prefs.getString(KEY_SCREEN_CREDENTIAL, null)
    }

    override fun getScreenId(): String? {
        return prefs.getString(KEY_SCREEN_ID, null)
    }

    override fun hasCredential(): Boolean {
        return !getCredential().isNullOrBlank()
    }

    override fun clearCredential() {
        prefs.edit()
            .remove(KEY_SCREEN_CREDENTIAL)
            .remove(KEY_SCREEN_ID)
            .apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "adveno_secure_credentials"
        private const val FALLBACK_PREFS_FILE_NAME = "adveno_fallback_credentials"
        private const val KEY_SCREEN_CREDENTIAL = "raw_screen_credential"
        private const val KEY_SCREEN_ID = "screen_id"
    }
}

