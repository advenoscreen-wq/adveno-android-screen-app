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
 */
sealed interface AdItem {
    val id: String
    val type: MediaType
    val title: String
    val durationMs: Long

    /**
     * Local MP4 video advertisement item.
     * Default duration is 10 seconds (10,000 ms).
     */
    data class Video(
        override val id: String = "ad_video_1",
        override val title: String = "Adveno Video Ad (10s)",
        override val durationMs: Long = 10_000L,
        @RawRes val rawResId: Int
    ) : AdItem {
        override val type: MediaType = MediaType.VIDEO
    }

    /**
     * Local image advertisement item.
     * Default display duration is 5 seconds (5,000 ms).
     */
    data class Image(
        override val id: String = "ad_image_1",
        override val title: String = "Adveno Display Banner (5s)",
        override val durationMs: Long = 5_000L,
        @DrawableRes val drawableResId: Int
    ) : AdItem {
        override val type: MediaType = MediaType.IMAGE
    }
}
