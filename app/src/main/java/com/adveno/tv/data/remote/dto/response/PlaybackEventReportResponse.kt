package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackEventReportResponse(
    @SerialName("stored_count")
    val storedCount: Int = 0,

    @SerialName("duplicate_count")
    val duplicateCount: Int = 0
)
