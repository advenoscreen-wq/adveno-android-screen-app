package com.adveno.tv.presentation.screens.syncing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Ads Synchronization screen.
 * Simulates the 5-second media download and synchronization progress,
 * and manages the transition to the Synced state.
 */
class SyncingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SyncingUiState())
    val uiState: StateFlow<SyncingUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null

    /**
     * Starts the deterministic 5-second synchronization progress simulation.
     */
    fun startSyncSimulation() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _uiState.value = SyncingUiState(
                isSynced = false,
                progress = 0f,
                heading = "Syncing Ads...",
                description = "Downloading the latest content to this screen."
            )

            val totalDurationMs = 5000L
            val stepIntervalMs = 50L
            val totalSteps = (totalDurationMs / stepIntervalMs).toInt()

            for (step in 1..totalSteps) {
                delay(stepIntervalMs)
                val currentProgress = (step.toFloat() / totalSteps).coerceIn(0f, 1f)
                _uiState.update { it.copy(progress = currentProgress) }
            }

            // Exactly at 5 seconds: complete synchronization
            _uiState.update {
                it.copy(
                    isSynced = true,
                    progress = 1.0f,
                    heading = "Synced!",
                    description = "Your advertisements are ready."
                )
            }
        }
    }

    fun reset() {
        syncJob?.cancel()
        _uiState.value = SyncingUiState()
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
    }
}

