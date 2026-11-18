package com.adveno.tv.presentation.components

/**
 * Generates a deterministic, standard-proportioned placeholder QR code matrix (29x29).
 * Contains authentic QR code structures (Finder Patterns, Timing Patterns, Alignment Pattern,
 * and quiet separators) for high-fidelity UI demonstration without external libraries.
 *
 * When real pairing backend is connected, this can easily be swapped with ZXing or the pairing token encoder.
 */
object PlaceholderQrGenerator {

    const val MATRIX_SIZE = 29

    /**
     * Generates a 29x29 boolean matrix representing QR code modules.
     * True indicates a dark/black module, False indicates a light/white module.
     */
    fun generatePlaceholderMatrix(seed: Long = 0xADBE102026L): Array<BooleanArray> {
        val size = MATRIX_SIZE
        val matrix = Array(size) { BooleanArray(size) { false } }
        val reserved = Array(size) { BooleanArray(size) { false } }

        // 1. Finder patterns (7x7) + Separators (1 module white space)
        fun placeFinderPattern(startRow: Int, startCol: Int) {
            // Draw 7x7 finder pattern
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val row = startRow + r
                    val col = startCol + c
                    reserved[row][col] = true
                    // Outer border or inner 3x3
                    val isOuterBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInnerCenter = (r in 2..4) && (c in 2..4)
                    matrix[row][col] = isOuterBorder || isInnerCenter
                }
            }

            // Reserve quiet separator space (1 module around finder pattern)
            for (r in -1..7) {
                for (c in -1..7) {
                    val row = startRow + r
                    val col = startCol + c
                    if (row in 0 until size && col in 0 until size) {
                        reserved[row][col] = true
                    }
                }
            }
        }

        // Top-left, Top-right, Bottom-left finder patterns
        placeFinderPattern(0, 0)
        placeFinderPattern(0, size - 7)
        placeFinderPattern(size - 7, 0)

        // 2. Alignment Pattern (5x5) at (20..24, 20..24)
        val alignRow = 20
        val alignCol = 20
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val row = alignRow + r
                val col = alignCol + c
                reserved[row][col] = true
                val isOuter = r == 0 || r == 4 || c == 0 || c == 4
                val isCenter = r == 2 && c == 2
                matrix[row][col] = isOuter || isCenter
            }
        }

        // 3. Timing patterns (Row 6 and Column 6)
        for (i in 8 until size - 8) {
            val isDark = (i % 2 == 0)
            if (!reserved[6][i]) {
                matrix[6][i] = isDark
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = isDark
                reserved[i][6] = true
            }
        }

        // 4. Dark Module
        matrix[size - 8][8] = true
        reserved[size - 8][8] = true

        // 5. Fill remaining data modules with deterministic pseudo-random bits
        var rng = seed
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!reserved[r][c]) {
                    // Linear congruential generator for deterministic aesthetic distribution
                    rng = (rng * 6364136223846793005L + 1442695040888963407L)
                    val bit = ((rng ushr 32) and 1L) == 1L
                    matrix[r][c] = bit
                }
            }
        }

        return matrix
    }
}
