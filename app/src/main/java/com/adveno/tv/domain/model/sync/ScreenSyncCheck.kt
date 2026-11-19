package com.adveno.tv.domain.model.sync

/**
 * Domain result of the sync-check / heartbeat request.
 */
data class ScreenSyncCheck(
    val needSync: Boolean,
    val desiredRevision: Int? = null
)
