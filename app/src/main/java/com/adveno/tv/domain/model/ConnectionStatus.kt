package com.adveno.tv.domain.model

/**
 * Represents the state of the TV screen connection to the Adveno service/host.
 */
sealed interface ConnectionStatus {
    data object Disconnected : ConnectionStatus
    data object Connecting : ConnectionStatus
    data object Connected : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}

