package com.adveno.tv.domain.model.media

/**
 * Domain entity representing an advertisement media asset.
 */
data class Media(
    val id: String,
    val name: String,
    val mediaType: MediaType,
    val mimeType: String,
    val storagePath: String,
    val fileSize: Long,
    val checksum: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationSeconds: Int? = null,
    val status: MediaStatus = MediaStatus.ACTIVE,
    val mediaUrl: String
)

