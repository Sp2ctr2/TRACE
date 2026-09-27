package app.saeon.trace.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.saeon.trace.core.AuthMethod
import app.saeon.trace.core.AuthorizationChallenge
import app.saeon.trace.ui.BankViewModel

/** Register during Activity.onCreate so ActivityResult and BiometricPrompt reattach after rotation.
 * Pending nonces live in the ViewModel. Late callbacks cannot authorize a different transaction.
 * This is local device re-authorization, not bank-server identity or a signing certificate.
 */
class DeviceAuthentication(private val activity:FragmentActivity,private val model:BankViewModel){
    private val credential=activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val nonce=model.pendingCredentialNonce
        model.pendingCredentialNonce=null
        val challenge=model.interaction.value.authChallenge
        if(nonce!=null&&challenge?.nonce==nonce){
            if(result.resultCode==Activity.RESULT_OK)model.authorize(challenge,AuthMethod.DEVICE_CREDENTIAL)
            else model.cancelAuthorization()
        }
    }
    private val prompt=BiometricPrompt(activity,ContextCompat.getMainExecutor(activity),object:BiometricPrompt.AuthenticationCallback(){
        override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){
            val nonce=model.pendingBiometricNonce;model.pendingBiometricNonce=null
            val challenge=model.interaction.value.authChallenge
            if(nonce!=null&&challenge?.nonce==nonce){
                model.authorize(challenge,if(result.authenticationType==BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL)AuthMethod.DEVICE_CREDENTIAL else AuthMethod.BIOMETRIC)
            }
        }
        override fun onAuthenticationError(errorCode:Int,errString:CharSequence){
            val nonce=model.pendingBiometricNonce;model.pendingBiometricNonce=null
            val challenge=model.interaction.value.authChallenge
            if(nonce==null||challenge?.nonce!=nonce)return
            if(errorCode==BiometricPrompt.ERROR_NEGATIVE_BUTTON&&Build.VERSION.SDK_INT<30){startCredential(challenge);return}
            model.cancelAuthorization()
            if(errorCode !in setOf(BiometricPrompt.ERROR_USER_CANCELED,BiometricPrompt.ERROR_CANCELED,BiometricPrompt.ERROR_NEGATIVE_BUTTON))
                model.showError("기기 인증을 완료하지 못했습니다. 다시 시도해 주세요.")
        }
    })
    fun authenticate(challenge:AuthorizationChallenge){
        if(model.interaction.value.authChallenge!=challenge)return
        if(model.pendingBiometricNonce!=null||model.pendingCredentialNonce!=null)return
        val strong=BiometricManager.Authenticators.BIOMETRIC_STRONG
        if(BiometricManager.from(activity).canAuthenticate(strong)==BiometricManager.BIOMETRIC_SUCCESS){
            model.pendingBiometricNonce=challenge.nonce
            val info=BiometricPrompt.PromptInfo.Builder().setTitle("송금 인증").setSubtitle("받는 분과 금액을 확인하세요.")
            if(Build.VERSION.SDK_INT>=30)info.setAllowedAuthenticators(strong or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            else info.setAllowedAuthenticators(strong).setNegativeButtonText("기기 잠금으로 확인")
            try{prompt.authenticate(info.build())}catch(_:Exception){model.pendingBiometricNonce=null;startCredential(challenge)}
        }else startCredential(challenge)
    }
    @Suppress("DEPRECATION")
    private fun startCredential(challenge:AuthorizationChallenge){
        if(model.interaction.value.authChallenge!=challenge)return
        val keyguard=activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if(!keyguard.isDeviceSecure){
            model.cancelAuthorization();model.showError("기기 화면 잠금을 먼저 설정해 주세요. PIN·패턴·비밀번호 또는 지문으로 송금을 인증합니다.");return
        }
        val intent=keyguard.createConfirmDeviceCredentialIntent("송금 인증","기기 잠금을 해제해 본인을 확인하세요.")
        if(intent==null){model.cancelAuthorization();model.showError("기기 인증을 시작하지 못했습니다.");return}
        model.pendingCredentialNonce=challenge.nonce
        try{credential.launch(intent)}catch(_:Exception){model.pendingCredentialNonce=null;model.cancelAuthorization();model.showError("기기 인증을 시작하지 못했습니다.")}
    }
}
