package app.saeon.trace.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.saeon.trace.SaeonApplication
import app.saeon.trace.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ManualCheckState(
    val text: String = "", val shared: Boolean = false,
    val source: RiskSource = RiskSource.MANUAL_TEXT,
    val result: List<RiskEvent>? = null, val busy: Boolean = false, val error: String? = null,
    val delivery: Long = 0
)
/** Text exists only in this in-memory ViewModel. Deliberately not SavedStateHandle,
 * Room, clipboard history, analytics, logs or an outgoing attestation field.
 */
class SafetyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as SaeonApplication).graph.repository
    private val _state = MutableStateFlow(ManualCheckState())
    val state = _state.asStateFlow()
    private var analysisJob: Job? = null
    private var generation = 0L
    private fun invalidateAnalysis() {
        generation++
        analysisJob?.cancel()
        analysisJob = null
    }
    fun receive(text: String) {
        invalidateAnalysis()
        _state.value = ManualCheckState(text = text.take(SignalExtractor.MAX_INPUT + 1), shared = true,
            source = RiskSource.SHARED_TEXT, delivery = generation,
            error = if (text.length > SignalExtractor.MAX_INPUT) "4,000자까지 확인할 수 있어요. 필요한 내용만 남겨 주세요." else null)
    }
    fun edit(text: String, source: RiskSource? = null) {
        invalidateAnalysis()
        _state.update { it.copy(text = text.take(SignalExtractor.MAX_INPUT + 1), result = null, busy = false,
            source = source ?: it.source, error = null) }
    }
    fun showError(message: String) { _state.update { it.copy(error = message) } }
    fun clear() { invalidateAnalysis(); _state.value = ManualCheckState() }
    fun analyze() {
        val input = _state.value
        if (input.busy) return
        _state.update { it.copy(busy = true, error = null) }
        val requestGeneration = generation
        analysisJob = viewModelScope.launch {
            try {
                val events = withContext(Dispatchers.Default) { SignalExtractor.extract(input.text, repository.clock.now(), input.source) }
                ensureActive()
                if (requestGeneration != generation) return@launch
                repository.addSignals(events)
                // A new share or edit must never be cleared by an older request.
                if (requestGeneration == generation) _state.update { it.copy(text = "", result = events, busy = false) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                if (requestGeneration == generation) _state.update { it.copy(busy = false, error = if (error is BankFailure) error.message else
                    "내용을 확인하지 못했어요. 송금은 실행하지 않았습니다. 다시 시도하거나 공식 은행 경로로 확인해 주세요.") }
            }
        }
    }
}
