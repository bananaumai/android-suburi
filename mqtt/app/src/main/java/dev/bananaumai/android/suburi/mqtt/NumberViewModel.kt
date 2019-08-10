package dev.bananaumai.android.suburi.mqtt

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NumberViewModel : ViewModel() {
    val currentNumber: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
}
