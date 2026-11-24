package com.adveno.tv.presentation.screens.ads

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

/**
 * Type of advertisement media supported by the digital signage player.
 */
enum class MediaType {
    VIDEO,
    IMAGE
}

/**
 * Represents an advertisement item in the local playback playlist.
 * Supports both downloaded local disk files and embedded fallback raw resources.
 */
sealed interface AdItem {
    val id: String
    val type: MediaType
    val title: String
    val durationMs: Long
    val localFilePath: String?
    val playlistId: String?
    val playlistItemId: String?
    val mediaAssetId: String?
    val playlistVersion: Int?
    val mediaChecksum: String?

    /**
     * Local MP4 video advertisement item.
     * Played using AndroidX Media3 ExoPlayer.
     */
    data class Video(
        override val id: String = "ad_video_1",
        override val title: String = "Adveno Video Ad (10s)",
        override val durationMs: Long = 10_000L,
        @RawRes val rawResId: Int? = null,
        val filePath: String? = null,
        override val playlistId: String? = null,
        override val playlistItemId: String? = null,
        override val mediaAssetId: String? = null,
        override val playlistVersion: Int? = null,
        override val mediaChecksum: String? = null
    ) : AdItem {
        override val type: MediaType = MediaType.VIDEO
        override val localFilePath: String? get() = filePath
    }

    /**
     * Local image advertisement item (PNG, JPG, WEBP).
     * Rendered in Jetpack Compose.
     */
    data class Image(
        override val id: String = "ad_image_1",
        override val title: String = "Adveno Display Banner (5s)",
        override val durationMs: Long = 5_000L,
        @DrawableRes val drawableResId: Int? = null,
        val filePath: String? = null,
        override val playlistId: String? = null,
        override val playlistItemId: String? = null,
        override val mediaAssetId: String? = null,
        override val playlistVersion: Int? = null,
        override val mediaChecksum: String? = null
    ) : AdItem {
        override val type: MediaType = MediaType.IMAGE
        override val localFilePath: String? get() = filePath
    }
}

val AdItem.isRealAd: Boolean
    get() = !playlistId.isNullOrBlank() && !playlistItemId.isNullOrBlank()
