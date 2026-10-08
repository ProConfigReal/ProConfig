package com.proconfig.app.service

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object StateBus {
    const val DISCONNECTED = 0
    const val CONNECTING = 1
    const val CONNECTED = 2
    const val DISCONNECTING = 3

    private val _state = MutableLiveData(DISCONNECTED)
    val state: LiveData<Int> = _state

    private val _speedUp = MutableLiveData(0L)
    val speedUp: LiveData<Long> = _speedUp

    private val _speedDown = MutableLiveData(0L)
    val speedDown: LiveData<Long> = _speedDown

    val totalUp = AtomicLong(0)
    val totalDown = AtomicLong(0)
    val connectedAt = AtomicLong(0)

    private val speedDirty = AtomicBoolean(false)

    fun setState(s: Int) {
        _state.postValue(s)
        if (s == CONNECTED) {
            connectedAt.set(System.currentTimeMillis())
            totalUp.set(0)
            totalDown.set(0)
        }
        if (s == DISCONNECTED) {
            _speedUp.postValue(0)
            _speedDown.postValue(0)
        }
    }

    fun postSpeed(up: Long, down: Long) {
        _speedUp.postValue(up)
        _speedDown.postValue(down)
        totalUp.addAndGet(up)
        totalDown.addAndGet(down)
        speedDirty.set(true)
    }

    fun currentSpeed(): Pair<Long, Long> {
        return Pair(_speedUp.value ?: 0L, _speedDown.value ?: 0L)
    }
}
