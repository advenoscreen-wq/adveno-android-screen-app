package com.adveno.tv.data.mapper

import com.adveno.tv.data.remote.dto.request.ScreenSyncAcknowledgmentRequest
import com.adveno.tv.data.remote.dto.response.ScreenSyncCheckResponse
import com.adveno.tv.domain.model.sync.ScreenSyncCheck
import com.adveno.tv.domain.model.sync.SyncAcknowledgment

object SyncMapper {

    fun toDomain(response: ScreenSyncCheckResponse): ScreenSyncCheck {
        return ScreenSyncCheck(
            needSync = response.needSync,
            desiredRevision = response.desiredRevision
        )
    }

    fun toRequestDto(appliedRevision: Int): ScreenSyncAcknowledgmentRequest {
        return ScreenSyncAcknowledgmentRequest(
            appliedRevision = appliedRevision
        )
    }

    fun toAcknowledgmentDomain(appliedRevision: Int, isAccepted: Boolean = true): SyncAcknowledgment {
        return SyncAcknowledgment(
            appliedRevision = appliedRevision,
            isAccepted = isAccepted
        )
    }
}
