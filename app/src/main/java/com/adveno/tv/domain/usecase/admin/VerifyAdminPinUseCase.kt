package com.adveno.tv.domain.usecase.admin

import android.util.Log
import com.adveno.tv.core.network.NetworkError
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.storage.AdminSessionStorage
import com.adveno.tv.data.remote.datasource.ScreenRemoteDataSource

/**
 * Use case to verify a 6-digit administrator PIN and establish a temporary admin session.
 *
 * Security Rules:
 * - Never logs or persists the entered PIN in plain text.
 * - On backend success, securely stores the returned raw session token in [AdminSessionStorage].
 * - Never logs or exposes the raw session token.
 * - Leaves the existing screen credential intact.
 */
class VerifyAdminPinUseCase(
    private val screenRemoteDataSource: ScreenRemoteDataSource,
    private val adminSessionStorage: AdminSessionStorage
) {

    suspend operator fun invoke(pin: String): NetworkResult<Unit> {
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            return NetworkResult.Error(
                NetworkError.Unknown("PIN must be exactly 6 digits.")
            )
        }

        Log.d(TAG, "Submitting admin PIN verification request...")
        val result = screenRemoteDataSource.verifyAdminPin(pin)

        return when (result) {
            is NetworkResult.Success -> {
                val token = result.data.rawToken
                if (!token.isNullOrBlank()) {
                    adminSessionStorage.saveAdminSession(token)
                    Log.d(TAG, "Admin session token established successfully.")
                    NetworkResult.Success(Unit)
                } else {
                    Log.e(TAG, "Admin session verification succeeded but no session token was returned.")
                    NetworkResult.Error(
                        NetworkError.Unknown("No admin session token returned by server.")
                    )
                }
            }
            is NetworkResult.Error -> {
                Log.w(TAG, "Admin PIN verification failed: ${result.error.message}")
                result
            }
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    companion object {
        private const val TAG = "VerifyAdminPinUseCase"
    }
}
