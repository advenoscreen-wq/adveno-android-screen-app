package com.adveno.tv.core.storage

/**
 * Interface defining operations for persisting and retrieving raw screen credentials.
 * The backend stores only the SHA-256 hash of the credential; the TV client must
 * securely persist the raw credential and supply it via Bearer token authentication.
 */
interface ScreenCredentialStorage {
    /**
     * Persists the raw screen credential and optional associated screen ID.
     */
    fun saveCredential(credential: String, screenId: String? = null)

    /**
     * Retrieves the stored raw screen credential, or null if not yet paired.
     */
    fun getCredential(): String?

    /**
     * Retrieves the associated screen ID, if known.
     */
    fun getScreenId(): String?

    /**
     * Returns true if a valid raw credential is currently saved.
     */
    fun hasCredential(): Boolean

    /**
     * Clears all stored credentials upon screen logout or revocation.
     */
    fun clearCredential()
}

