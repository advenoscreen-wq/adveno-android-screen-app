package com.adveno.tv.data.remote.downloader

import android.content.Context
import android.util.Log
import com.adveno.tv.core.network.NetworkConfig
import com.adveno.tv.domain.model.media.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Clean Architecture data-layer component responsible for downloading, verifying,
 * and caching media assets (images and videos) to app-private internal storage.
 */
interface MediaDownloader {

    /**
     * Downloads the given [media] asset to app-private storage, verifying its transport integrity.
     * Streams large video files directly to disk without exhausting memory.
     * Reuses existing valid files if available and verified.
     */
    suspend fun downloadMedia(
        media: Media,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): File

    /**
     * Returns the local file if it exists and passes validation.
     */
    fun getMediaFile(media: Media): File?
}

class MediaDownloaderImpl(
    private val okHttpClient: OkHttpClient,
    private val context: Context
) : MediaDownloader {

    private val mediaDir: File
        get() = File(context.filesDir, "media").apply {
            if (!exists()) mkdirs()
        }

    override fun getMediaFile(media: Media): File? {
        val safeMediaId = sanitizeFileName(media.id)
        val extension = getExtension(media.mimeType, media.storagePath, media.mediaUrl)
        val file = File(mediaDir, "media_${safeMediaId}.$extension")
        return if (file.exists() && isFileValid(file)) file else null
    }

    override suspend fun downloadMedia(
        media: Media,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val safeMediaId = sanitizeFileName(media.id)
        val extension = getExtension(media.mimeType, media.storagePath, media.mediaUrl)
        val targetFile = File(mediaDir, "media_${safeMediaId}.$extension")

        // 1. Check if valid local file already exists and can be safely reused
        if (targetFile.exists() && isFileValid(targetFile)) {
            Log.d(TAG, "Reusing existing valid media file: ${targetFile.name} (${targetFile.length()} bytes)")
            return@withContext targetFile
        }

        // 2. Stream download to a temporary file in app-private storage
        val tempFile = File(mediaDir, "temp_${safeMediaId}_${System.currentTimeMillis()}.tmp")

        try {
            // Strip screen authentication headers so public media buckets (Supabase, S3) accept the download cleanly
            val request = Request.Builder()
                .url(media.mediaUrl)
                .header(NetworkConfig.HEADER_NO_DEVICE_AUTH, "true")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Download failed for media ${media.id} (${media.name}): HTTP ${response.code}")
            }

            val body = response.body ?: throw IOException("Empty response body for ${media.mediaUrl}")
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        onProgress(totalRead, contentLength)
                    }
                    output.flush()
                }
            }

            // 3. Validation: file existence, non-empty, and transport stream completeness
            if (!tempFile.exists() || tempFile.length() == 0L || !tempFile.canRead()) {
                throw IOException("Downloaded file is empty or unreadable: ${tempFile.name}")
            }

            // Verify against HTTP Content-Length only when the server provides a known positive value
            if (contentLength > 0L && tempFile.length() != contentLength) {
                throw IOException(
                    "Download incomplete for ${media.name}: received ${tempFile.length()} of $contentLength bytes"
                )
            }

            // Note on metadata: The backend contract currently provides raw 'file_size' and 'checksum'
            // without documenting algorithm semantics (e.g. content MD5 vs ETag vs upload ID).
            // Per contract rules, we avoid guessing or inventing algorithms and verify integrity
            // through complete HTTP transport EOF and non-empty file readability.
            if (media.checksum.isNotBlank()) {
                Log.d(TAG, "Media metadata checksum present ('${media.checksum}'). Algorithm unestablished; relying on verified HTTP transport stream.")
            }
            if (media.fileSize > 0L) {
                Log.d(TAG, "Media metadata file_size: ${media.fileSize} (actual downloaded bytes: ${tempFile.length()})")
            }

            // 4. Atomically commit temporary file to target final destination
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Log.d(TAG, "Successfully downloaded and verified: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            targetFile
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            throw e
        }
    }

    private fun isFileValid(file: File): Boolean {
        return file.exists() && file.length() > 0L && file.canRead()
    }

    private fun sanitizeFileName(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
    }

    private fun getExtension(mimeType: String?, storagePath: String?, mediaUrl: String?): String {
        val pathExt = storagePath?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it.length in 2..4 }
        if (!pathExt.isNullOrBlank()) return pathExt

        val urlExt = mediaUrl?.substringBefore('?')?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it.length in 2..4 }
        if (!urlExt.isNullOrBlank()) return urlExt

        return when (mimeType?.lowercase()) {
            "video/mp4" -> "mp4"
            "video/webm" -> "webm"
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            "image/webp" -> "webp"
            else -> if (mimeType?.startsWith("video/") == true) "mp4" else "jpg"
        }
    }

    companion object {
        private const val TAG = "MediaDownloader"
    }
}
