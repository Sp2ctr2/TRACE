package app.saeon.trace.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.saeon.trace.SaeonApplication
import app.saeon.trace.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicLong

data class ManualCheckState(
    val text: String = "", val shared: Boolean = false,
    val source: RiskSource = RiskSource.MANUAL_TEXT,
    val result: List<RiskEvent>? = null, val busy: Boolean = false, val error: String? = null,
    val delivery: Long = 0
)
/** Raw input is only held in memory, never saved in the ledger or saved state.
 * Each explicit confirmation owns one generation. New input, cancellation and
 * leaving the screen invalidate pending work; old results cannot erase a new share.
 */
class SafetyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as SaeonApplication).graph.repository
    private val _state = MutableStateFlow(ManualCheckState())
    val state = _state.asStateFlow()
    private val generation = AtomicLong()
    private var analysis: Job? = null
    private fun invalidate() {
        generation.incrementAndGet()
        analysis?.cancel()
        analysis = null
    }
    fun receive(text: String) {
        invalidate()
        _state.value = ManualCheckState(text = text.take(SignalExtractor.MAX_INPUT + 1), shared = true,
            source = RiskSource.SHARED_TEXT, delivery = System.nanoTime(),
            error = if (text.length > SignalExtractor.MAX_INPUT) "4,000자까지 확인할 수 있어요. 필요한 내용만 남겨 주세요." else null)
    }
    fun edit(text: String, source: RiskSource? = null) {
        invalidate()
        _state.update { it.copy(text = text.take(SignalExtractor.MAX_INPUT + 1), result = null,
            source = source ?: it.source, busy = false, error = null) }
    }
    fun showError(message: String) { _state.update { it.copy(error = message) } }
    fun clear() {
        invalidate()
        _state.value = ManualCheckState()
    }
    fun analyze() {
        val input = _state.value
        if (input.busy) return
        val ticket = generation.incrementAndGet()
        _state.update { it.copy(busy = true, error = null) }
        analysis = viewModelScope.launch {
            try {
                val events = withContext(Dispatchers.Default) {
                    SignalExtractor.extract(input.text, repository.clock.now(), input.source)
                }
                ensureActive()
                repository.change { bank, _ ->
                    if (generation.get() != ticket) throw CancellationException("Input generation changed")
                    bank.copy(events = (bank.events + events).distinctBy { it.id })
                }
                ensureActive()
                if (generation.get() == ticket) _state.update { it.copy(text = "", result = events, busy = false) }
            } catch (cancel: CancellationException) {
                if (generation.get() == ticket) _state.update { it.copy(busy = false) }
                throw cancel
            } catch (error: Exception) {
                if (generation.get() == ticket) _state.update { it.copy(busy = false, error = if (error is BankFailure) error.message else
                    "내용을 확인하지 못했어요. 송금은 실행하지 않았습니다. 다시 시도하거나 공식 은행 경로로 확인해 주세요.") }
            } finally {
                if (generation.get() == ticket) analysis = null
            }
        }
    }
}
