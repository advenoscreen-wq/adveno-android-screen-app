package com.adveno.tv.domain.model.sync

/**
 * Domain synchronization state.
 * Note: The backend heartbeat only returns `need_sync`; this status enum
 * represents the client-side progression through the download and verification phase.
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

