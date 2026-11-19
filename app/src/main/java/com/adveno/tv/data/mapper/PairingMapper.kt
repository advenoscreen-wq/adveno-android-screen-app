package com.adveno.tv.data.mapper

import com.adveno.tv.data.remote.dto.response.CreatePairedScreenResponse
import com.adveno.tv.data.remote.dto.response.CreateScreenPairingSessionResponse
import com.adveno.tv.data.remote.dto.response.IssuePairedScreenCredentialResponse
import com.adveno.tv.data.remote.dto.response.ScreenPairingSessionStatusResponse
import com.adveno.tv.domain.model.pairing.PairingSession
import com.adveno.tv.domain.model.pairing.PairingSessionStatus
import com.adveno.tv.domain.model.pairing.PairingType
import com.adveno.tv.domain.model.screen.Screen
import com.adveno.tv.domain.model.screen.ScreenCredential
import com.adveno.tv.domain.model.screen.ScreenStatus

object PairingMapper {

    fun toDomain(response: CreateScreenPairingSessionResponse, pairingType: PairingType): PairingSession {
        return PairingSession(
            sessionId = response.sessionId,
            pairingToken = response.pairingToken,
            expiresAt = response.expiresAt,
            pairingType = pairingType,
            status = PairingSessionStatus.PENDING
        )
    }

    fun toDomain(response: ScreenPairingSessionStatusResponse): PairingSessionStatus {
        return when (response.status.uppercase()) {
            "PENDING" -> PairingSessionStatus.PENDING
            "CONNECTING" -> PairingSessionStatus.CONNECTING
            "PAIRED" -> PairingSessionStatus.PAIRED
            "EXPIRED" -> PairingSessionStatus.EXPIRED
            else -> PairingSessionStatus.PENDING
        }
    }

    fun toDomain(response: IssuePairedScreenCredentialResponse): ScreenCredential {
        return ScreenCredential(
            screenId = response.screenId,
            rawCredential = response.credential
        )
    }

    fun toDomain(response: CreatePairedScreenResponse): Screen {
        return Screen(
            id = response.screenId,
            screenCode = response.screenCode,
            name = response.name,
            status = ScreenStatus.ACTIVE,
            credential = response.credential
        )
    }
}

