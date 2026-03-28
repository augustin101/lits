package com.example.lits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lits.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)

    val hapticEnabled: StateFlow<Boolean> = store.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = true)

    val twoTapMode: StateFlow<Boolean> = store.twoTapMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    val zenMode: StateFlow<Boolean> = store.zenMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setHapticEnabled(enabled) }
    }

    fun setTwoTapMode(enabled: Boolean) {
        viewModelScope.launch { store.setTwoTapMode(enabled) }
    }

    fun setZenMode(enabled: Boolean) {
        viewModelScope.launch { store.setZenMode(enabled) }
    }
}
