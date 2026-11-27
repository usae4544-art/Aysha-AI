package com.aysha.assistant.wakeword

import com.aysha.assistant.core.AwarenessDecisionLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object WakeWordStatusBus {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _modelState = MutableStateFlow(WakeModelState.IDLE)
    val modelState: StateFlow<WakeModelState> = _modelState.asStateFlow()

    private val _statusText = MutableStateFlow("Wake word off")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _micEnergy = MutableStateFlow(0f)
    val micEnergy: StateFlow<Float> = _micEnergy.asStateFlow()

    private val _decisionLogs = MutableStateFlow<List<AwarenessDecisionLog>>(emptyList())
    val decisionLogs: StateFlow<List<AwarenessDecisionLog>> = _decisionLogs.asStateFlow()

    private val _wakeTriggers = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val wakeTriggers: SharedFlow<String> = _wakeTriggers.asSharedFlow()

    fun emitWakeTrigger(phrase: String) {
        _wakeTriggers.tryEmit(phrase)
    }

    fun publish(
        running: Boolean? = null,
        state: WakeModelState? = null,
        status: String? = null,
        energy: Float? = null,
    ) {
        if (running != null) _isRunning.value = running
        if (state != null) _modelState.value = state
        if (status != null) _statusText.value = status
        if (energy != null) _micEnergy.value = energy
    }

    fun logDecision(entry: AwarenessDecisionLog) {
        val current = _decisionLogs.value
        _decisionLogs.value = (listOf(entry) + current).take(60)
    }

    fun clearLogs() {
        _decisionLogs.value = emptyList()
    }
}
