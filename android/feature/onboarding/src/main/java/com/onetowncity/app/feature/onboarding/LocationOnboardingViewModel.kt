package com.onetowncity.app.feature.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** How the location step ended. Either way the user moves on — location is never a hard gate. */
enum class LocationOutcome { Granted, ManualCity }

enum class LocationStage {
    /** Explains why location is needed *before* the system dialog appears. */
    Rationale,

    /** The system dialog was declined; manual city selection is offered. */
    Denied,
}

data class LocationOnboardingUiState(val stage: LocationStage = LocationStage.Rationale)

/**
 * MVI-style state holder: [state] is what the screen shows, [outcomes] are one-shot navigation events (a Channel, so a
 * rotation can never replay a finished step). It holds no business rules — which city a location maps to is decided by
 * the backend.
 */
class LocationOnboardingViewModel : ViewModel() {
    private val _state = MutableStateFlow(LocationOnboardingUiState())
    val state: StateFlow<LocationOnboardingUiState> = _state.asStateFlow()

    private val _outcomes = Channel<LocationOutcome>(Channel.BUFFERED)
    val outcomes: Flow<LocationOutcome> = _outcomes.receiveAsFlow()

    /** [granted] is true when at least approximate location was allowed. */
    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _outcomes.trySend(LocationOutcome.Granted)
        } else {
            _state.update { it.copy(stage = LocationStage.Denied) }
        }
    }

    fun onChooseManually() {
        _outcomes.trySend(LocationOutcome.ManualCity)
    }
}
