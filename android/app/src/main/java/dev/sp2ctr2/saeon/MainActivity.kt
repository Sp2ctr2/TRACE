package dev.sp2ctr2.saeon

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.sp2ctr2.saeon.ui.SaeonApp

class MainActivity : FragmentActivity() {
    val model: BankViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        setContent { SaeonApp(model, ::authenticate, ::canAuthenticate) }
        if(savedInstanceState==null) consumeShare(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); consumeShare(intent) }
    private fun consumeShare(intent: Intent?) {
        if(intent?.action==Intent.ACTION_SEND && intent.type=="text/plain") {
            val text=intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            intent.removeExtra(Intent.EXTRA_TEXT)
            model.receiveShared(text)
        }
    }
    private fun authFlags() = if(android.os.Build.VERSION.SDK_INT>=30) BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL else BiometricManager.Authenticators.BIOMETRIC_STRONG
    private fun canAuthenticate() = BiometricManager.from(this).canAuthenticate(authFlags())==BiometricManager.BIOMETRIC_SUCCESS
    private fun authenticate(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if(!canAuthenticate()) { onFailure("사용 가능한 기기 인증이 없어요. 시연 확인을 선택해 주세요."); return }
        val prompt=BiometricPrompt(this,ContextCompat.getMainExecutor(this),object: BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onFailure("인증을 마치지 않았어요. 돈은 나가지 않았습니다.") }
        })
        val info=BiometricPrompt.PromptInfo.Builder().setTitle("새온은행 거래 확인").setSubtitle("기기 인증 후에도 TRACE의 거래 정책을 확인합니다.").setAllowedAuthenticators(authFlags())
        if(android.os.Build.VERSION.SDK_INT<30) info.setNegativeButtonText("취소")
        prompt.authenticate(info.build())
    }
}
