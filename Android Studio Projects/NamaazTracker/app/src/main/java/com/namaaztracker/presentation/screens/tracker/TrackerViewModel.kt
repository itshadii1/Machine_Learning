package com.namaaztracker.presentation.screens.tracker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.namaaztracker.domain.model.NamaazSession
import com.namaaztracker.domain.model.NamaazType
import com.namaaztracker.domain.model.Posture
import com.namaaztracker.domain.model.RakahState
import com.namaaztracker.domain.usecase.PredictPostureUseCase
import com.namaaztracker.domain.usecase.UpdateRakahStateUseCase
import com.namaaztracker.util.HapticType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class TrackerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val predictPostureUseCase: PredictPostureUseCase,
    private val updateRakahStateUseCase: UpdateRakahStateUseCase,
) : ViewModel() {

    private val namaazType: NamaazType = NamaazType.valueOf(
        savedStateHandle.get<String>("namaazType") ?: NamaazType.FAJR.name,
    )

    private val _uiState = MutableStateFlow(
        TrackerUiState(session = NamaazSession(namaazType = namaazType)),
    )
    val uiState = _uiState.asStateFlow()

    private val _hapticEvents = MutableSharedFlow<HapticType>(extraBufferCapacity = 1)
    val hapticEvents = _hapticEvents.asSharedFlow()

    private val rawPostureFlow = MutableSharedFlow<Posture>(extraBufferCapacity = 20)
    private val apiCallInProgress = AtomicBoolean(false)

    // Require N consecutive detections of the same posture before triggering a transition.
    // At 4 fps this means ~750 ms of stable hold — robust against single-frame flickers.
    private val STABLE_FRAME_COUNT = 3

    init {
        viewModelScope.launch {
            var lastPosture = Posture.UNKNOWN
            var consecutiveCount = 0

            rawPostureFlow.collect { posture ->
                if (posture == Posture.UNKNOWN) {
                    consecutiveCount = 0
                    return@collect
                }
                if (posture == lastPosture) {
                    consecutiveCount++
                    if (consecutiveCount == STABLE_FRAME_COUNT) {
                        applyTransition(posture)
                    }
                } else {
                    lastPosture = posture
                    consecutiveCount = 1
                }
            }
        }
    }

    fun onFrameCaptured(jpegBytes: ByteArray) {
        if (!apiCallInProgress.compareAndSet(false, true)) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val posture = predictPostureUseCase(jpegBytes)
                _uiState.update { it.copy(currentPosture = posture, apiConnected = true) }
                rawPostureFlow.emit(posture)
            } catch (_: Exception) {
                // Silently swallow API errors so prayer is not interrupted
            } finally {
                apiCallInProgress.set(false)
            }
        }
    }

    private fun applyTransition(posture: Posture) {
        val current = _uiState.value
        val prevRakat = current.session.completedRakat
        val newSession = updateRakahStateUseCase(posture, current.session)

        if (newSession == current.session) return

        _uiState.update { it.copy(session = newSession) }

        when {
            newSession.currentState == RakahState.COMPLETE ->
                _hapticEvents.tryEmit(HapticType.PRAYER_COMPLETE)

            newSession.completedRakat > prevRakat ->
                _hapticEvents.tryEmit(HapticType.RAKAH_COMPLETE)
        }
    }
}
