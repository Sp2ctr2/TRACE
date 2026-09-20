package app.saeon.trace.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.saeon.trace.SaeonApplication
import app.saeon.trace.core.*
import app.saeon.trace.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class InteractionState(
    val busy: Boolean = false,
    val error: String? = null,
    val authChallenge: AuthorizationChallenge? = null,
    val routeLoading: Boolean = false
)
class BankViewModel(application: Application) : AndroidViewModel(application) {
    val graph = (application as SaeonApplication).graph
    val repository = graph.repository
    val bank = repository.state
    val fatal = repository.fatal
    val preferences = graph.preferences.flow.stateIn(viewModelScope, SharingStarted.Eagerly, BankPreferences())
    private val _interaction = MutableStateFlow(InteractionState())
    val interaction: StateFlow<InteractionState> = _interaction.asStateFlow()
    private var approvalJob: Job? = null
    private fun errorMessage(error: Exception): String = if (error is BankFailure) error.message else BankRepository.storageMessage
    fun dismissError() { _interaction.update { it.copy(error = null) } }
    fun showError(message: String) { _interaction.update { it.copy(error = message) } }
    fun act(action: suspend () -> Unit) {
        if (_interaction.value.busy) return
        _interaction.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { action() }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { _interaction.update { it.copy(error = errorMessage(error)) } }
            finally { _interaction.update { it.copy(busy = false, routeLoading = false) } }
        }
    }
    fun storeDraft(draft: TransferDraft) {
        viewModelScope.launch {
            try { repository.setDraft(draft) }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { showError(errorMessage(error)) }
        }
    }
    fun startRecipient(recipient: Recipient, done: () -> Unit) = act {
        val scenario = bank.value?.scenario ?: DemoScenario.NORMAL
        val matchesFixture = recipient.id == Fixtures.recipient(scenario).id
        repository.setDraft(TransferDraft(recipient, if (matchesFixture) Fixtures.amount(scenario) else 0,
            if (matchesFixture) Fixtures.purpose(scenario) else Purpose.GENERAL))
        done()
    }
    fun review(draft: TransferDraft, done: () -> Unit) = act {
        repository.review(draft)
        done()
    }
    fun editReview(id: String, done: () -> Unit) = act { repository.editReview(id); done() }
    fun requestAuthorization(id: String) = act {
        val challenge = repository.prepare(id)
        _interaction.update { it.copy(authChallenge = challenge) }
    }
    fun authorize(challenge: AuthorizationChallenge, method: AuthMethod) {
        if (_interaction.value.busy || _interaction.value.authChallenge != challenge) return
        _interaction.update { it.copy(busy = true, authChallenge = null, error = null) }
        approvalJob = viewModelScope.launch {
            try {
                repository.authorize(challenge, method)
                delay(550)
                repository.finish(challenge.intentId)
            } catch (cancel: CancellationException) {
                withContext(NonCancellable) { runCatching { repository.cancelAuthorization(challenge.intentId) } }
                throw cancel
            } catch (error: Exception) {
                runCatching { repository.cancelAuthorization(challenge.intentId, errorMessage(error)) }
                showError(errorMessage(error))
            } finally { _interaction.update { it.copy(busy = false) } }
        }
    }
    fun cancelAuthorization() {
        val id = _interaction.value.authChallenge?.intentId ?: bank.value?.currentTransferId
        approvalJob?.cancel()
        _interaction.update { it.copy(authChallenge = null) }
        if (id != null) viewModelScope.launch {
            try { repository.cancelAuthorization(id) }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { showError(errorMessage(error)) }
        }
    }
    fun acknowledge(id: String) = act { repository.acknowledge(id) }
    fun resolveRoute(id: String) = act {
        _interaction.update { it.copy(routeLoading = true) }
        delay(350)
        repository.resolveRoute(id)
    }
    fun useRoute(id: String) = act { repository.useRoute(id) }
    fun cancelTransfer(id: String, done: () -> Unit) = act { repository.cancel(id); done() }
    fun resume(id: String, done: () -> Unit) = act { repository.selectTransfer(id); done() }
    fun reset(scenario: DemoScenario, done: () -> Unit) = act {
        repository.reset(scenario)
        graph.preferences.reset()
        graph.preferences.easy(scenario == DemoScenario.EASY)
        _interaction.update { it.copy(authChallenge = null) }
        done()
    }
    fun startDemo(scenario: DemoScenario, done: () -> Unit) = act {
        repository.reset(scenario)
        if (scenario == DemoScenario.EASY) graph.preferences.readingMode(ReadingMode.LARGE)
        _interaction.update { it.copy(authChallenge = null) }
        done()
    }
    fun retryStorage() = act { repository.initialize() }
    fun preference(action: suspend Preferences.() -> Unit) {
        viewModelScope.launch {
            try { graph.preferences.action() }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { showError("설정을 저장하지 못했어요. 다시 시도해 주세요. 거래 상태와 잔액은 변경하지 않았습니다.") }
        }
    }
}
