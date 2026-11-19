package com.adveno.tv.domain.model.pairing

/**
 * Type of pairing initiated by the screen.
 */
enum class PairingType {
    /**
     * Initial registration of a newly provisioned TV screen.
     */
    NEW_SCREEN,

    /**
     * Re-linking or rotating credentials for an existing known screen.
     */
    RECONNECT_EXISTING
}

