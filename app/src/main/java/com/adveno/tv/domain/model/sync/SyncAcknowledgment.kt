package com.adveno.tv.domain.model.sync

/**
 * Domain entity representing the confirmation of a successfully applied revision.
 */
data class SyncAcknowledgment(
    val appliedRevision: Int,
    val isAccepted: Boolean = true
)

