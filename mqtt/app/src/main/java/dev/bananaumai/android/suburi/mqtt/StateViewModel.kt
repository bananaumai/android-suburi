package dev.bananaumai.android.suburi.mqtt

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StateViewModel : ViewModel() {
    enum class State {
        STOPPED, RUNNING
    }

    val currentState: MutableLiveData<State> by lazy {
        MutableLiveData<State>().apply {
            value = State.STOPPED
        }
    }

    fun flip() {
        currentState.value = when(currentState.value) {
            State.STOPPED -> State.RUNNING
            else -> State.STOPPED
        }
    }
}
