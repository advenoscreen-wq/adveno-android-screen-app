package com.adveno.tv.presentation.screens.connecting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Connecting / Connected success screen state and ViewModel.
 */
class ConnectingUnitTest {

    @Test
    fun connectingViewModel_initialStateIsConnecting() {
        val viewModel = ConnectingViewModel()
        val state = viewModel.uiState.value

        assertNotNull(state)
        assertEquals("Adveno", state.brandName)
        assertEquals("Connecting...", state.heading)
        assertEquals("Linking this screen to your Adveno account", state.description)
        assertFalse(state.isConnected)
    }

    @Test
    fun connectingViewModel_markConnectedTransitionsStateCorrectly() {
        val viewModel = ConnectingViewModel()
        viewModel.markConnected()

        val state = viewModel.uiState.value
        assertTrue(state.isConnected)
        assertEquals("Connected!", state.heading)
        assertEquals("Your screen is now connected to Adveno.", state.description)
    }

    @Test
    fun connectingViewModel_resetRestoresInitialState() {
        val viewModel = ConnectingViewModel()
        viewModel.markConnected()
        assertTrue(viewModel.uiState.value.isConnected)

        viewModel.reset()
        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertEquals("Connecting...", state.heading)
        assertEquals("Linking this screen to your Adveno account", state.description)
    }
}
