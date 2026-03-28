package com.example.lits.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    val TWO_TAP_MODE = booleanPreferencesKey("two_tap_mode")
    val ZEN_MODE = booleanPreferencesKey("zen_mode")
}

class SettingsStore(private val context: Context) {

    val hapticEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[SettingsKeys.HAPTIC_ENABLED] ?: true }

    val twoTapMode: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[SettingsKeys.TWO_TAP_MODE] ?: false }

    val zenMode: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[SettingsKeys.ZEN_MODE] ?: false }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun setTwoTapMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.TWO_TAP_MODE] = enabled
        }
    }

    suspend fun setZenMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.ZEN_MODE] = enabled
        }
    }
}
