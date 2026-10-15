package com.jomiro.app.orobest_sign.core.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Estado de solo lectura para la vista; los trabajos se ejecutan en viewModelScope. */
abstract class StateViewModel<State>(initialState: State) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<State> = mutableState.asStateFlow()

    protected fun updateState(transform: (State) -> State) {
        mutableState.update(transform)
    }
}
