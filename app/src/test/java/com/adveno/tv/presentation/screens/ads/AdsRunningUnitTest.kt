package com.adveno.tv.presentation.screens.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Ads Running screen architecture, playlist loop,
 * progress updates, and control state transitions.
 */
class AdsRunningUnitTest {

    private val testPlaylist: List<AdItem> = listOf(
        AdItem.Video(
            id = "test_video",
            durationMs = 10_000L,
            rawResId = 1
        ),
        AdItem.Image(
            id = "test_image",
            durationMs = 5_000L,
            drawableResId = 2
        )
    )

    @Test
    fun adsRunningUiState_initialStateIsCorrect() {
        val viewModel = AdsRunningViewModel(customPlaylist = testPlaylist)
        val state = viewModel.uiState.value

        assertNotNull(state.currentItem)
        assertTrue(state.currentItem is AdItem.Video)
        assertEquals(10_000L, state.currentItem?.durationMs)
        assertEquals(0f, state.currentProgress, 0.001f)
        assertTrue(state.isControlsVisible)
    }

    @Test
    fun adsRunningViewModel_videoProgressUpdatesSmoothly() {
        val viewModel = AdsRunningViewModel(customPlaylist = testPlaylist)

        viewModel.updateVideoProgress(positionMs = 2_500L, durationMs = 10_000L)
        assertEquals(0.25f, viewModel.uiState.value.currentProgress, 0.001f)

        viewModel.updateVideoProgress(positionMs = 5_000L, durationMs = 10_000L)
        assertEquals(0.50f, viewModel.uiState.value.currentProgress, 0.001f)

        viewModel.updateVideoProgress(positionMs = 10_000L, durationMs = 10_000L)
        assertEquals(1.0f, viewModel.uiState.value.currentProgress, 0.001f)
    }

    @Test
    fun adsRunningViewModel_videoCompletionAdvancesToImage() {
        val viewModel = AdsRunningViewModel(customPlaylist = testPlaylist)

        // When video finishes, advance to the image item
        viewModel.onVideoCompleted(startTimer = false)
        val state = viewModel.uiState.value

        assertNotNull(state.currentItem)
        assertTrue(state.currentItem is AdItem.Image)
        assertEquals(5_000L, state.currentItem?.durationMs)
        assertEquals(0f, state.currentProgress, 0.001f)
    }

    @Test
    fun adsRunningViewModel_imageCompletionLoopsBackToVideo() {
        val viewModel = AdsRunningViewModel(customPlaylist = testPlaylist)

        // Video -> Image
        viewModel.onVideoCompleted(startTimer = false)
        assertTrue(viewModel.uiState.value.currentItem is AdItem.Image)

        // Image -> Loops back to Video
        viewModel.advanceToNextItem(startTimer = false)
        assertTrue(viewModel.uiState.value.currentItem is AdItem.Video)
        assertEquals(0f, viewModel.uiState.value.currentProgress, 0.001f)
    }

    @Test
    fun adsRunningViewModel_initialControlsVisible() {
        val viewModel = AdsRunningViewModel(customPlaylist = testPlaylist)
        assertTrue(viewModel.uiState.value.isControlsVisible)
    }

    @Test
    fun adsRunningViewModel_onVideoErrorSafelyAdvancesToNextItem() {
        val viewModel = AdsRunningViewModel(customPlaylist = testPlaylist)
        assertTrue(viewModel.uiState.value.currentItem is AdItem.Video)

        // Error falls back to Image instead of freezing
        viewModel.onVideoError(startTimer = false)
        assertTrue(viewModel.uiState.value.currentItem is AdItem.Image)
    }
}

