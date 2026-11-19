package com.adveno.tv.data.repository

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.data.mapper.PlaylistMapper
import com.adveno.tv.data.remote.datasource.ScreenRemoteDataSource
import com.adveno.tv.domain.model.playlist.Playlist
import com.adveno.tv.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PlaylistRepositoryImpl(
    private val remoteDataSource: ScreenRemoteDataSource
) : PlaylistRepository {

    override fun getAssignedPlaylist(): Flow<NetworkResult<Playlist?>> = flow {
        val result = remoteDataSource.getAssignedPlaylist()
        val domainResult = result.map { PlaylistMapper.toDomain(it) }
        emit(domainResult)
    }
}

