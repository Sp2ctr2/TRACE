package app.saeon.trace.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.preferencesStore by preferencesDataStore("saeon_preferences")
enum class ReadingMode(val label: String) { STANDARD("기본"), LARGE("큰 글씨"), CHILD("어린이") }
data class BankPreferences(
    val easyMode: Boolean = false, val biometric: Boolean = false,
    val notifications: Boolean = true, val hideBalance: Boolean = false,
    val favoriteIds: Set<String> = setOf("seoyeon", "family", "minjun"),
    val childMode: Boolean = false
) {
    val readingMode: ReadingMode get() = when { childMode -> ReadingMode.CHILD; easyMode -> ReadingMode.LARGE; else -> ReadingMode.STANDARD }
}
class Preferences(context: Context) {
    private val store = context.applicationContext.preferencesStore
    private val easy = booleanPreferencesKey("easy")
    private val child = booleanPreferencesKey("child_mode")
    private val biometric = booleanPreferencesKey("biometric")
    private val notifications = booleanPreferencesKey("notifications")
    private val hideBalance = booleanPreferencesKey("hide_balance")
    private val favorites = stringSetPreferencesKey("favorites")
    val flow: Flow<BankPreferences> = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        BankPreferences((it[easy] ?: false) || (it[child] ?: false), it[biometric] ?: false,
            it[notifications] ?: true, it[hideBalance] ?: false,
            it[favorites] ?: setOf("seoyeon", "family", "minjun"), it[child] ?: false)
    }
    suspend fun readingMode(value: ReadingMode) { store.edit {
        it[easy] = value != ReadingMode.STANDARD
        it[child] = value == ReadingMode.CHILD
    } }
    suspend fun easy(value: Boolean) { readingMode(if (value) ReadingMode.LARGE else ReadingMode.STANDARD) }
    suspend fun biometric(value: Boolean) { store.edit { it[biometric] = value } }
    suspend fun notifications(value: Boolean) { store.edit { it[notifications] = value } }
    suspend fun hideBalance(value: Boolean) { store.edit { it[hideBalance] = value } }
    suspend fun favorite(id: String, enabled: Boolean) { store.edit {
        val previous = it[favorites] ?: setOf("seoyeon", "family", "minjun")
        it[favorites] = if (enabled) previous + id else previous - id
    } }
    suspend fun reset() { store.edit { it.clear() } }
}
