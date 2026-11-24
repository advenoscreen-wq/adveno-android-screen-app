package com.adveno.tv.domain.usecase.event

import android.util.Log
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.data.remote.datasource.ScreenRemoteDataSource
import com.adveno.tv.data.remote.dto.request.PlaybackEventBatchRequest
import com.adveno.tv.data.remote.dto.request.PlaybackEventDto
import com.adveno.tv.domain.repository.PlaybackEventRepository

/**
 * Use case to report pending ad playback events to the backend.
 *
 * Rules:
 * - Batches up to 100 events per request.
 * - Excludes the currently playing ad's event (if supplied).
 * - Completed events AND prior incomplete events (played_complete_at == null) are reported.
 * - Deletes reported events from local SQLite database ONLY upon confirmed HTTP 202 / success.
 * - In case of network loss or API failure, preserves pending events for next retry.
 */
class ReportPlaybackEventsUseCase(
    private val eventRepository: PlaybackEventRepository,
    private val screenRemoteDataSource: ScreenRemoteDataSource
) {

    suspend operator fun invoke(
        excludeCurrentEventId: String? = null
    ): NetworkResult<Int> {
        var totalReported = 0
        val batchLimit = 100

        while (true) {
            val pendingEvents = eventRepository.getPendingEvents(
                excludeEventId = excludeCurrentEventId,
                limit = batchLimit
            )

            if (pendingEvents.isEmpty()) {
                Log.d(TAG, "No pending eligible playback events to report.")
                return NetworkResult.Success(totalReported)
            }

            Log.d(TAG, "Reporting batch of ${pendingEvents.size} playback events (excluding $excludeCurrentEventId)...")

            val dtos = pendingEvents.map { event ->
                PlaybackEventDto(
                    eventId = event.eventId,
                    playlistId = event.playlistId,
                    playlistItemId = event.playlistItemId,
                    mediaAssetId = event.mediaAssetId,
                    playlistVersion = event.playlistVersion,
                    mediaChecksum = event.mediaChecksum,
                    playedAt = event.playedAt,
                    playedCompleteAt = event.playedCompleteAt
                )
            }

            val request = PlaybackEventBatchRequest(events = dtos)
            val result = screenRemoteDataSource.reportPlaybackEvents(request)

            when (result) {
                is NetworkResult.Success -> {
                    val reportedIds = pendingEvents.map { it.eventId }
                    Log.d(TAG, "Batch of ${reportedIds.size} events reported successfully. Deleting from local database.")
                    eventRepository.deleteEvents(reportedIds)
                    totalReported += reportedIds.size

                    // If fewer than batch limit were returned, we have reported everything eligible
                    if (pendingEvents.size < batchLimit) {
                        return NetworkResult.Success(totalReported)
                    }
                }
                is NetworkResult.Error -> {
                    Log.w(TAG, "Failed to report playback events batch: ${result.error.message}. Retaining remaining events.")
                    return result
                }
                is NetworkResult.Loading -> {
                    // Not expected from remoteDataSource, continue
                }
            }
        }
    }

    companion object {
        private const val TAG = "ReportPlaybackEvents"
    }
}
