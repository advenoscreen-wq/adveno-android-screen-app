package com.adveno.tv.core.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Storage manager for active pairing sessions.
 * Holds the current session ID, raw pairing token, and expiration timestamp.
 * Note: The pairing token is temporary (10-minute expiry) and used exclusively
 * for polling status, issuing reconnect credentials, and initial registration.
 */
class PairingSessionStorage {

    data class PairingSessionData(
        val sessionId: String,
        val pairingToken: String,
        val expiresAt: String
    )

    private val _currentSession = MutableStateFlow<PairingSessionData?>(null)
    val currentSession: StateFlow<PairingSessionData?> = _currentSession.asStateFlow()

    fun saveSession(sessionId: String, pairingToken: String, expiresAt: String) {
        _currentSession.value = PairingSessionData(
            sessionId = sessionId,
            pairingToken = pairingToken,
            expiresAt = expiresAt
        )
    }

    fun getPairingToken(): String? {
        return _currentSession.value?.pairingToken
    }

    fun getSessionId(): String? {
        return _currentSession.value?.sessionId
    }

    fun getExpiresAt(): String? {
        return _currentSession.value?.expiresAt
    }

    fun hasActiveSession(): Boolean {
        return _currentSession.value != null
    }

    fun clearSession() {
        _currentSession.value = null
    }
}

