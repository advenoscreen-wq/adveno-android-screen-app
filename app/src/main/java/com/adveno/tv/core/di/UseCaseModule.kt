package com.adveno.tv.core.di

import com.adveno.tv.domain.repository.PairingRepository
import com.adveno.tv.domain.repository.PlaylistRepository
import com.adveno.tv.domain.repository.ScreenRepository
import com.adveno.tv.domain.repository.SyncRepository
import com.adveno.tv.domain.usecase.pairing.CreatePairingSessionUseCase
import com.adveno.tv.domain.usecase.pairing.IssueReconnectCredentialUseCase
import com.adveno.tv.domain.usecase.pairing.PollPairingStatusUseCase
import com.adveno.tv.domain.usecase.pairing.RegisterNewScreenUseCase
import com.adveno.tv.domain.usecase.playlist.GetAssignedPlaylistUseCase
import com.adveno.tv.domain.usecase.screen.LogoutScreenUseCase
import com.adveno.tv.domain.usecase.screen.VerifyCredentialUseCase
import com.adveno.tv.domain.usecase.sync.AcknowledgeSyncUseCase
import com.adveno.tv.domain.usecase.sync.CheckSyncStatusUseCase

object UseCaseModule {

    fun provideCreatePairingSessionUseCase(repository: PairingRepository): CreatePairingSessionUseCase {
        return CreatePairingSessionUseCase(repository)
    }

    fun providePollPairingStatusUseCase(repository: PairingRepository): PollPairingStatusUseCase {
        return PollPairingStatusUseCase(repository)
    }

    fun provideIssueReconnectCredentialUseCase(repository: PairingRepository): IssueReconnectCredentialUseCase {
        return IssueReconnectCredentialUseCase(repository)
    }

    fun provideRegisterNewScreenUseCase(repository: PairingRepository): RegisterNewScreenUseCase {
        return RegisterNewScreenUseCase(repository)
    }

    fun provideLogoutScreenUseCase(repository: ScreenRepository): LogoutScreenUseCase {
        return LogoutScreenUseCase(repository)
    }

    fun provideVerifyCredentialUseCase(repository: ScreenRepository): VerifyCredentialUseCase {
        return VerifyCredentialUseCase(repository)
    }

    fun provideGetAssignedPlaylistUseCase(repository: PlaylistRepository): GetAssignedPlaylistUseCase {
        return GetAssignedPlaylistUseCase(repository)
    }

    fun provideCheckSyncStatusUseCase(repository: SyncRepository): CheckSyncStatusUseCase {
        return CheckSyncStatusUseCase(repository)
    }

    fun provideAcknowledgeSyncUseCase(repository: SyncRepository): AcknowledgeSyncUseCase {
        return AcknowledgeSyncUseCase(repository)
    }
}

