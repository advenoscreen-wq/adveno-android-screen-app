package com.adveno.tv.presentation.screens.scan

import com.adveno.tv.presentation.components.PlaceholderQrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Scan to Connect components and placeholder QR generation.
 */
class ScanToConnectUnitTest {

    @Test
    fun placeholderQr_matrixIsSquareAndMatchesStandardDimensions() {
        val matrix = PlaceholderQrGenerator.generatePlaceholderMatrix()
        assertEquals(PlaceholderQrGenerator.MATRIX_SIZE, matrix.size)

        for (row in matrix) {
            assertEquals(PlaceholderQrGenerator.MATRIX_SIZE, row.size)
        }
    }

    @Test
    fun placeholderQr_finderPatternsArePlacedCorrectly() {
        val matrix = PlaceholderQrGenerator.generatePlaceholderMatrix()
        val size = PlaceholderQrGenerator.MATRIX_SIZE

        // Top-left finder pattern center (row 3, col 3) must be dark
        assertTrue("Top-left finder center should be dark", matrix[3][3])
        assertTrue("Top-left finder outer corner (0,0) should be dark", matrix[0][0])
        assertTrue("Top-left finder outer corner (6,6) should be dark", matrix[6][6])

        // Top-right finder pattern center (row 3, col size - 4)
        assertTrue("Top-right finder center should be dark", matrix[3][size - 4])

        // Bottom-left finder pattern center (row size - 4, col 3)
        assertTrue("Bottom-left finder center should be dark", matrix[size - 4][3])
    }

    @Test
    fun scanToConnectViewModel_initialStateHasRequiredContent() {
        val viewModel = ScanToConnectViewModel()
        val state = viewModel.uiState.value

        assertNotNull(state)
        assertEquals("Adveno", state.brandName)
        assertEquals("Scan to Connect", state.heading)
        assertEquals(
            "Scan this QR code from your Adveno dashboard to link this screen.",
            state.description
        )
        assertTrue(state.isQrReady)
    }
}

