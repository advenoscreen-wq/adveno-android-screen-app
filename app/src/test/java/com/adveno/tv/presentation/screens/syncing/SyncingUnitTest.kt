package com.adveno.tv.presentation.screens.syncing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Ads Synchronization screen state and ViewModel.
 */
class SyncingUnitTest {

    @Test
    fun syncingUiState_initialDefaultsAreCorrect() {
        val state = SyncingUiState()

        assertEquals("Adveno", state.brandName)
        assertFalse(state.isSynced)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals("0%", state.percentageText)
        assertEquals("Syncing Ads...", state.heading)
        assertEquals("Downloading the latest content to this screen.", state.description)
    }

    @Test
    fun syncingUiState_percentageFormattingMatchesProgress() {
        val state70 = SyncingUiState(progress = 0.70f)
        assertEquals("70%", state70.percentageText)

        val state100 = SyncingUiState(isSynced = true, progress = 1.0f)
        assertEquals("100%", state100.percentageText)
        assertEquals("Synced!", state100.heading)
        assertEquals("Your advertisements are ready.", state100.description)
        assertTrue(state100.isSynced)
    }

    @Test
    fun syncingViewModel_initialStateMatchesDefaults() {
        val viewModel = SyncingViewModel()
        val state = viewModel.uiState.value

        assertNotNull(state)
        assertFalse(state.isSynced)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals("Syncing Ads...", state.heading)
    }
}

