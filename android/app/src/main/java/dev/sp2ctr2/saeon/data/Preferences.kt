package dev.sp2ctr2.saeon.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.tracePreferences by preferencesDataStore("saeon-options")
data class AppOptions(val easy: Boolean = false, val biometric: Boolean = false, val reducedMotion: Boolean = false, val haptics: Boolean = true, val hideBalance: Boolean = false)
class Preferences(context: Context) {
    private val store = context.applicationContext.tracePreferences
    val options = store.data.map { p -> AppOptions(p[booleanPreferencesKey("easy")] ?: false, p[booleanPreferencesKey("biometric")] ?: false, p[booleanPreferencesKey("motion")] ?: false, p[booleanPreferencesKey("haptics")] ?: true, p[booleanPreferencesKey("hideBalance")] ?: false) }
    suspend fun set(key: String, value: Boolean) {
        require(key in setOf("easy", "biometric", "motion", "haptics", "hideBalance"))
        store.edit { it[booleanPreferencesKey(key)] = value }
    }
}
