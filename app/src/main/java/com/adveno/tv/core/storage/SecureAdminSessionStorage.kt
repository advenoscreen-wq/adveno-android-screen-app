package com.adveno.tv.core.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe implementation of [AdminSessionStorage].
 *
 * Stores the temporary session token strictly in memory during the active session.
 * Guaranteed to clear on application restart, session revocation, or exiting Protected Settings.
 * Never logs or exposes the token in plain text.
 */
class SecureAdminSessionStorage : AdminSessionStorage {

    private val _sessionToken = MutableStateFlow<String?>(null)
    val sessionToken: StateFlow<String?> = _sessionToken.asStateFlow()

    override fun saveAdminSession(token: String) {
        _sessionToken.value = token
    }

    override fun getAdminSessionToken(): String? {
        return _sessionToken.value
    }

    override fun hasActiveAdminSession(): Boolean {
        return !_sessionToken.value.isNullOrBlank()
    }

    override fun clearAdminSession() {
        _sessionToken.value = null
    }
}
