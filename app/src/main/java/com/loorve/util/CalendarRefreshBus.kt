package com.loorve.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRefreshBus @Inject constructor() {
    private val _refreshEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvent: SharedFlow<Unit> = _refreshEvent.asSharedFlow()

    fun notifyRefresh() {
        _refreshEvent.tryEmit(Unit)
    }
}