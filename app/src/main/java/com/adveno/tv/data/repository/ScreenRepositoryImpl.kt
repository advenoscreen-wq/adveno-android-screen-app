package com.adveno.tv.data.repository

import com.adveno.tv.core.network.NetworkError
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

    override fun verifyCredential(): Flow<NetworkResult<Boolean>> = flow {
        val credential = credentialStorage.getCredential()
        if (credential.isNullOrBlank()) {
            emit(NetworkResult.Success(false))
            return@flow
        }

        val result = remoteDataSource.verifyCredential(credential)
        when (result) {
            is NetworkResult.Success -> {
                val isValid = result.data.valid
                if (!isValid) {
                    credentialStorage.clearCredential()
                }
                emit(NetworkResult.Success(isValid))
            }
            is NetworkResult.Error -> {
                // If 401 SCREEN_DISCONNECTED/INVALID_SCREEN_CREDENTIAL or 404 SCREEN_NOT_FOUND, clear invalid credential
                val isRevokedOrNotFound = when (val err = result.error) {
                    is NetworkError.DeviceRevoked -> true
                    is NetworkError.HttpError -> err.httpCode == 401 || err.httpCode == 404
                    else -> false
                }
                if (isRevokedOrNotFound) {
                    credentialStorage.clearCredential()
                }
                emit(NetworkResult.Error(result.error))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading)
            }
        }
    }

    override fun logoutScreen(): Flow<NetworkResult<Unit>> = flow {
        val result = remoteDataSource.logout()

        when (result) {
            is NetworkResult.Success -> {
                // Server revoked the credential successfully; clear local device store
                credentialStorage.clearCredential()
            }
            is NetworkResult.Error -> {
                // If backend explicitly confirms credential is already revoked/disconnected, clear local credential
                if (result.error is NetworkError.DeviceRevoked) {
                    credentialStorage.clearCredential()
                }
            }
            is NetworkResult.Loading -> {}
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

