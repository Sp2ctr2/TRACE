package app.saeon.trace.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.saeon.trace.core.*
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/** A Keystore-backed signing handle. The private key is never serialized or exported. */
class KeystoreAttestation(private val alias: String = "saeon.trace.attestation.v1") {
    private val store: KeyStore by lazy { KeyStore.getInstance("AndroidKeyStore").apply { load(null) } }
    @Synchronized private fun ensureKey() {
        if (store.containsAlias(alias)) return
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false)
                .build())
        }.generateKeyPair()
    }
    private fun publicKey(): PublicKey { ensureKey(); return store.getCertificate(alias).publicKey }
    val keyId: String get() = Digests.sha256(Base64.encodeToString(publicKey().encoded, Base64.NO_WRAP))
    fun sign(packet: RiskAttestation): RiskAttestation {
        ensureKey()
        require(packet.keyId == keyId && !packet.rawContentExported)
        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(store.getKey(alias, null) as PrivateKey)
            update(packet.canonical().toByteArray(Charsets.UTF_8))
        }.sign()
        return packet.copy(signature = Base64.encodeToString(signature, Base64.NO_WRAP))
    }
    fun verify(packet: RiskAttestation): Boolean = runCatching {
        // The verifier uses the locally provisioned key, never a public key supplied by the packet.
        packet.keyId == keyId && !packet.rawContentExported && Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey()); update(packet.canonical().toByteArray(Charsets.UTF_8))
            verify(Base64.decode(packet.signature, Base64.NO_WRAP))
        }
    }.getOrDefault(false)
}

/** Offline fixture gateway, deliberately not represented as a real remote bank.
 * Signatures demonstrate binding and replay checks, not remote device trust.
 */
class DemoBankGateway(private val keystore: KeystoreAttestation) {
    fun evaluateAndCommit(state: BankState, id: String, now: Long): BankState {
        if (state.receipts.any { it.intentId == id && !it.seed }) return state
        val packet = keystore.sign(BankEngine.attest(state, id, now, keystore.keyId))
        return BankEngine.finish(state, id, packet, now, keystore.verify(packet))
    }
    fun resolve(state: BankState, id: String, now: Long): BankState =
        BankEngine.resolveRoute(state, id, now, state.scenario != DemoScenario.UNKNOWN)
}
