package com.adveno.tv.core.di

import android.content.Context
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.core.storage.SecureScreenCredentialStorage

object StorageModule {

    fun provideScreenCredentialStorage(context: Context): ScreenCredentialStorage {
        return SecureScreenCredentialStorage(context)
    }

    fun providePairingSessionStorage(): PairingSessionStorage {
        return PairingSessionStorage()
    }
}

