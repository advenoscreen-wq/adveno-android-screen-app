package com.adveno.tv.presentation.screens.ads

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

/**
 * Represents an advertisement item in the local playback playlist.
 */
sealed interface AdItem {
    val id: String
    val durationMs: Long

    /**
     * Local MP4 video advertisement item.
     * Default duration is 10 seconds (10,000 ms).
     */
    data class Video(
        override val id: String = "video_ad",
        override val durationMs: Long = 10_000L,
        @RawRes val rawResId: Int
    ) : AdItem

    /**
     * Local image advertisement item.
     * Default display duration is 5 seconds (5,000 ms).
     */
    data class Image(
        override val id: String = "image_ad",
        override val durationMs: Long = 5_000L,
        @DrawableRes val drawableResId: Int
    ) : AdItem
}

