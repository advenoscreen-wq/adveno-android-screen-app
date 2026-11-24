package com.adveno.tv.domain.repository

import com.adveno.tv.domain.model.event.PlaybackEvent

interface PlaybackEventRepository {
    suspend fun recordPlaybackStart(event: PlaybackEvent)
    suspend fun recordPlaybackComplete(eventId: String, completedAtUtc: String)
    suspend fun getPendingEvents(excludeEventId: String?, limit: Int = 100): List<PlaybackEvent>
    suspend fun deleteEvents(eventIds: List<String>)
    suspend fun getPendingEventCount(excludeEventId: String? = null): Int
}
