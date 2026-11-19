package com.adveno.tv.domain.model.pairing

/**
 * Domain entity representing an active screen pairing session.
 */
data class PairingSession(
    val sessionId: String,
    val pairingToken: String,
    val expiresAt: String,
    val pairingType: PairingType,
    val status: PairingSessionStatus = PairingSessionStatus.PENDING
)

