package com.adveno.tv.core.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * High-performance QR Code generator for Android TV.
 * Encodes pairing URLs into 2D Boolean matrices for rendering on Jetpack Compose Canvas.
 */
object QrCodeGenerator {

    private const val BASE_PAIRING_URL = "https://advenocustomerapp/pair-screen?token="

    /**
     * Builds the pairing deep-link URL given the raw pairing token.
     * Encodes the token in UTF-8 per specifications.
     */
    fun buildPairingUrl(pairingToken: String): String {
        val encodedToken = URLEncoder.encode(pairingToken, StandardCharsets.UTF_8.name())
        return "$BASE_PAIRING_URL$encodedToken"
    }

    /**
     * Generates a 2D boolean matrix representing QR code modules for the given content.
     * True indicates a dark module, False indicates a light module.
     *
     * @param content The string payload to encode (e.g. the pairing URL).
     * @param requestedSize Desired module dimension (e.g. 29). ZXing calculates the smallest valid Version.
     */
    fun generateMatrix(
        content: String,
        requestedSize: Int = 29
    ): Array<BooleanArray> {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name(),
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, requestedSize, requestedSize, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height

        return Array(height) { row ->
            BooleanArray(width) { col ->
                bitMatrix.get(col, row)
            }
        }
    }
}

