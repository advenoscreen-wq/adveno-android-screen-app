package com.adveno.tv.domain.model.pairing

/**
 * Lifecycle status of a pairing session.
 */
enum class PairingSessionStatus {
    /**
     * Session is active and waiting for dashboard QR scan.
     */
    PENDING,

    /**
     * Dashboard user scanned the QR code; connection is in progress.
     */
    CONNECTING,

    /**
     * Screen has been successfully paired and credential issued.
     */
    PAIRED,

    /**
     * 10-minute validity window expired without completion.
     */
    EXPIRED
}

