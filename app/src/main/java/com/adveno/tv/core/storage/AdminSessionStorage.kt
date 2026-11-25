package com.adveno.tv.core.storage

/**
 * Storage contract for temporary admin-session tokens.
 *
 * Rules:
 * - Admin-session token is an additional authorization credential, not a replacement for screen credential.
 * - Stored separately from the screen credential.
 * - Cleared when the session expires, is revoked, or when user exits Protected Settings.
 * - Must never survive application restarts.
 */
interface AdminSessionStorage {
    fun saveAdminSession(token: String)
    fun getAdminSessionToken(): String?
    fun hasActiveAdminSession(): Boolean
    fun clearAdminSession()
}
