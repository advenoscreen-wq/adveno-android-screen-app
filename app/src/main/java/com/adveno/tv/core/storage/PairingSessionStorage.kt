package com.adveno.tv.core.storage

import com.adveno.tv.domain.model.pairing.PairingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Storage manager for active pairing sessions.
 * Holds the current session ID, raw pairing token, expiration timestamp, and pairing type.
 * Note: The pairing token is temporary (10-minute expiry) and used exclusively
 * for polling status, issuing reconnect credentials, and initial registration.
 */
class PairingSessionStorage {

    data class PairingSessionData(
        val sessionId: String,
        val pairingToken: String,
        val expiresAt: String,
        val pairingType: PairingType = PairingType.NEW_SCREEN
    )

    private val _currentSession = MutableStateFlow<PairingSessionData?>(null)
    val currentSession: StateFlow<PairingSessionData?> = _currentSession.asStateFlow()

    fun saveSession(
        sessionId: String,
        pairingToken: String,
        expiresAt: String,
        pairingType: PairingType = PairingType.NEW_SCREEN
    ) {
        _currentSession.value = PairingSessionData(
            sessionId = sessionId,
            pairingToken = pairingToken,
            expiresAt = expiresAt,
            pairingType = pairingType
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

    fun getPairingType(): PairingType? {
        return _currentSession.value?.pairingType
    }

    fun hasActiveSession(): Boolean {
        return _currentSession.value != null
    }

    fun clearSession() {
        _currentSession.value = null
    }
}

