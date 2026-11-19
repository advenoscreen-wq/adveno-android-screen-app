package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Nested media asset payload within playlist item responses.
 */
@Serializable
data class MediaResponse(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("media_type")
    val mediaType: String,

    @SerialName("mime_type")
    val mimeType: String,

    @SerialName("storage_path")
    val storagePath: String,

    @SerialName("file_size")
    val fileSize: Long,

    @SerialName("checksum")
    val checksum: String,

    @SerialName("width")
    val width: Int? = null,

    @SerialName("height")
    val height: Int? = null,

    @SerialName("duration_seconds")
    val durationSeconds: Int? = null,

    @SerialName("status")
    val status: String,

    @SerialName("media_url")
    val mediaUrl: String
)

