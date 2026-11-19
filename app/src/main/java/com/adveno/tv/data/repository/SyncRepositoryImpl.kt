package com.adveno.tv.data.repository

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.data.mapper.SyncMapper
import com.adveno.tv.data.remote.datasource.ScreenRemoteDataSource
import com.adveno.tv.domain.model.sync.ScreenSyncCheck
import com.adveno.tv.domain.model.sync.SyncAcknowledgment
import com.adveno.tv.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SyncRepositoryImpl(
    private val remoteDataSource: ScreenRemoteDataSource
) : SyncRepository {

    override fun checkSyncStatus(): Flow<NetworkResult<ScreenSyncCheck>> = flow {
        val result = remoteDataSource.syncCheck()
        val domainResult = result.map { SyncMapper.toDomain(it) }
        emit(domainResult)
    }

    override fun acknowledgeAppliedRevision(
        appliedRevision: Int
    ): Flow<NetworkResult<SyncAcknowledgment>> = flow {
        val request = SyncMapper.toRequestDto(appliedRevision)
        val result = remoteDataSource.acknowledgeSync(request)
        val domainResult = result.map {
            SyncMapper.toAcknowledgmentDomain(appliedRevision, isAccepted = true)
        }
        emit(domainResult)
    }
}

