package app.saeon.trace

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import app.saeon.trace.core.*
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

class MainActivity : FragmentActivity() {
    val model: BankViewModel by viewModels()
    val safetyModel: SafetyViewModel by viewModels()
    var navigation: NavHostController? = null
        internal set
    private var speech: SpeechRecognizer? = null
    private var voiceActive by mutableStateOf(false)
    private var biometricPrompt: BiometricPrompt? = null
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoice() else safetyModel.showError("음성 입력을 쓰려면 마이크 권한이 필요해요. 내용을 직접 입력할 수도 있습니다. 녹음이나 송금은 실행하지 않았습니다.")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        receiveSharedText(intent)
        setContent {
            val preferences by model.preferences.collectAsStateWithLifecycle()
            SaeonTheme(preferences.easyMode) {
                SaeonApp(model, safetyModel, onNavigationReady = { navigation = it },
                    onBiometric = ::authenticate, onVoice = { microphonePermission.launch(Manifest.permission.RECORD_AUDIO) },
                    onStopVoice = ::stopVoice, voiceActive = voiceActive)
            }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); receiveSharedText(intent) }
    private fun receiveSharedText(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_SEND || incoming.type != "text/plain") return
        val text = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        // Strip the original immediately. Rotation or state saving cannot re-ingest it.
        incoming.removeExtra(Intent.EXTRA_TEXT)
        incoming.clipData = null
        intent = Intent(this, MainActivity::class.java)
        safetyModel.receive(text)
    }
    private fun authenticate(challenge: AuthorizationChallenge) {
        val allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG
        if (BiometricManager.from(this).canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
            model.showError("이 기기에서는 생체 인증을 사용할 수 없어요. 시연 확인을 선택하면 가상 거래 인증을 진행할 수 있습니다. 아직 돈은 나가지 않았습니다.")
            return
        }
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                model.authorize(challenge, AuthMethod.BIOMETRIC)
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode in setOf(BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_CANCELED)) {
                    model.cancelAuthorization()
                } else {
                    model.showError("생체 인증을 마치지 못했어요. 다시 인증하거나 시연 확인을 선택해 주세요. 송금은 실행하지 않았습니다.")
                }
            }
        }).also {
            it.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("송금 확인")
                .setSubtitle("기기 인증 뒤 거래 맥락을 별도로 확인합니다.")
                .setAllowedAuthenticators(allowed).setNegativeButtonText("취소").build())
        }
    }
    private fun startVoice() {
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            safetyModel.showError("이 기기에는 기기 내 음성 인식이 준비되어 있지 않아요. 내용을 직접 입력해 주세요. 음성을 외부 서비스로 보내지 않았습니다.")
            return
        }
        stopVoice()
        try {
            speech = SpeechRecognizer.createOnDeviceSpeechRecognizer(this).also { recognizer ->
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { voiceActive = true }
                    override fun onBeginningOfSpeech() { voiceActive = true }
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() { voiceActive = false }
                    override fun onError(error: Int) {
                        stopVoice()
                        safetyModel.showError("기기에서 음성을 받아 적지 못했어요. 다시 말하거나 직접 입력해 주세요. 분석이나 송금은 실행하지 않았습니다.")
                    }
                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        stopVoice()
                        if (text.isNotBlank()) safetyModel.edit(text, RiskSource.USER_VOICE)
                        else safetyModel.showError("입력된 말이 없어요. 다시 말하거나 직접 입력해 주세요.")
                    }
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
                voiceActive = true
                recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
            }
        } catch (_: Exception) {
            stopVoice()
            safetyModel.showError("기기 내 음성 입력을 시작하지 못했어요. 내용을 직접 입력해 주세요. 녹음을 저장하거나 전송하지 않았습니다.")
        }
    }
    private fun stopVoice() {
        val active = speech
        speech = null
        active?.cancel()
        active?.destroy()
        voiceActive = false
    }
    override fun onStop() { stopVoice(); super.onStop() }
}
