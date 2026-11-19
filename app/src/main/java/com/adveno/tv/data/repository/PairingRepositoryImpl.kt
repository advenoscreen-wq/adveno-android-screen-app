package com.adveno.tv.data.repository

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.data.mapper.PairingMapper
import com.adveno.tv.data.remote.datasource.PairingRemoteDataSource
import com.adveno.tv.data.remote.dto.request.CreatePairedScreenRequest
import com.adveno.tv.data.remote.dto.request.CreateScreenPairingSessionRequest
import com.adveno.tv.domain.model.pairing.PairingSession
import com.adveno.tv.domain.model.pairing.PairingSessionStatus
import com.adveno.tv.domain.model.pairing.PairingType
import com.adveno.tv.domain.model.screen.Screen
import com.adveno.tv.domain.model.screen.ScreenCredential
import com.adveno.tv.domain.repository.PairingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PairingRepositoryImpl(
    private val remoteDataSource: PairingRemoteDataSource,
    private val sessionStorage: PairingSessionStorage,
    private val credentialStorage: ScreenCredentialStorage
) : PairingRepository {

    override fun createPairingSession(
        type: PairingType
    ): Flow<NetworkResult<PairingSession>> = flow {
        val request = CreateScreenPairingSessionRequest(pairingType = type.name)
        val result = remoteDataSource.createPairingSession(request)

        val domainResult = result.map { response ->
            sessionStorage.saveSession(
                sessionId = response.sessionId,
                pairingToken = response.pairingToken,
                expiresAt = response.expiresAt,
                pairingType = type
            )
            PairingMapper.toDomain(response, type)
        }

        emit(domainResult)
    }

    override fun getPairingStatus(
        sessionId: String
    ): Flow<NetworkResult<PairingSessionStatus>> = flow {
        val pairingToken = sessionStorage.getPairingToken()
        val result = remoteDataSource.pollPairingStatus(sessionId, pairingToken)
        val domainResult = result.map { PairingMapper.toDomain(it) }
        emit(domainResult)
    }

    override fun issueReconnectCredential(
        sessionId: String
    ): Flow<NetworkResult<ScreenCredential>> = flow {
        val pairingToken = sessionStorage.getPairingToken()
        val result = remoteDataSource.issueReconnectCredential(sessionId, pairingToken)

        val domainResult = result.map { response ->
            val domainCredential = PairingMapper.toDomain(response)
            // Persist the newly issued raw credential
            credentialStorage.saveCredential(
                credential = domainCredential.rawCredential,
                screenId = domainCredential.screenId
            )
            // Clean up temporary pairing token session
            sessionStorage.clearSession()
            domainCredential
        }

        emit(domainResult)
    }

    override fun registerNewScreen(
        sessionId: String,
        name: String,
        deviceModel: String?,
        appVersion: String?
    ): Flow<NetworkResult<Screen>> = flow {
        val pairingToken = sessionStorage.getPairingToken()
        val request = CreatePairedScreenRequest(
            name = name,
            deviceModel = deviceModel,
            appVersion = appVersion
        )

        val result = remoteDataSource.registerNewScreen(
            sessionId = sessionId,
            pairingToken = pairingToken,
            request = request
        )

        val domainResult = result.map { response ->
            val domainScreen = PairingMapper.toDomain(response)
            if (!response.credential.isNullOrBlank()) {
                credentialStorage.saveCredential(
                    credential = response.credential,
                    screenId = response.screenId
                )
            }
            sessionStorage.clearSession()
            domainScreen
        }

        emit(domainResult)
    }
}

