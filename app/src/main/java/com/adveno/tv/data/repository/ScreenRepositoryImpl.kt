package com.adveno.tv.data.repository

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.data.remote.datasource.ScreenRemoteDataSource
import com.adveno.tv.domain.repository.ScreenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ScreenRepositoryImpl(
    private val remoteDataSource: ScreenRemoteDataSource,
    private val credentialStorage: ScreenCredentialStorage
) : ScreenRepository {

    override fun logoutScreen(): Flow<NetworkResult<Unit>> = flow {
        val result = remoteDataSource.logout()

        result.onSuccess {
            // Server revoked the credential successfully; clear local device store
            credentialStorage.clearCredential()
        }

        emit(result)
    }

    override fun hasActiveCredential(): Boolean {
        return credentialStorage.hasCredential()
    }

    override fun clearLocalCredentials() {
        credentialStorage.clearCredential()
    }
}

