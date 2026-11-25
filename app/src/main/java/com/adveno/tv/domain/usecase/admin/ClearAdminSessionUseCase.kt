package com.adveno.tv.domain.usecase.admin

import com.adveno.tv.core.storage.AdminSessionStorage

/**
 * Use case to revoke and clear the local admin session.
 *
 * Clears only the temporary admin session token while preserving screen credentials.
 */
class ClearAdminSessionUseCase(
    private val adminSessionStorage: AdminSessionStorage
) {
    operator fun invoke() {
        adminSessionStorage.clearAdminSession()
    }
}
