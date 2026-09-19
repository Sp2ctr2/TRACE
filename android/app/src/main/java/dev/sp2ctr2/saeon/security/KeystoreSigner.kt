package dev.sp2ctr2.saeon.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dev.sp2ctr2.saeon.domain.ProofSigner
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/** Transaction binding demo. This is not Android hardware attestation or a bank server. */
class KeystoreSigner : ProofSigner {
    private val alias = "saeon.demo.transaction.v1"
    private fun store(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    @Synchronized private fun ensureKey(): KeyStore {
        val ks = store()
        if(!ks.containsAlias(alias)) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256).build())
                generateKeyPair()
            }
        }
        return store()
    }
    override fun sign(payload: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run {
        initSign(ensureKey().getKey(alias, null) as PrivateKey); update(payload); sign()
    }
    override fun verify(payload: ByteArray, signature: ByteArray): Boolean = runCatching {
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(ensureKey().getCertificate(alias).publicKey); update(payload); verify(signature)
        }
    }.getOrDefault(false)
}
