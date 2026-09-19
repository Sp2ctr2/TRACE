package app.saeon.trace

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import app.saeon.trace.data.SnapshotCodec
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class RepositoryDeviceTest : UiHarness() {
    @Test fun concurrentCommitIsOneDurableDebit() {
        fresh(); createReview(DemoScenario.NORMAL); authorizeOnly()
        val id = state.currentTransferId!!
        runBlocking { coroutineScope { List(8) { async(Dispatchers.IO) { repository.finish(id) } }.awaitAll() } }
        assertEquals(12_808_000L, state.balance)
        assertEquals(1, state.receipts.count { !it.seed })
        val persisted = SnapshotCodec.decode(runBlocking { graph.database.snapshots().read()!!.payload })
        assertEquals(state.balance, persisted.balance)
        assertEquals(1, persisted.receipts.count { !it.seed })
    }
    @Test fun signedPacketFailsOnAnyBindingTamper() {
        fresh(); createReview(DemoScenario.NORMAL); authorizeOnly()
        val packet = graph.keystore.sign(BankEngine.attest(state, state.currentTransferId!!, graph.clock.now(), graph.keystore.keyId))
        assertTrue(graph.keystore.verify(packet))
        assertFalse(graph.keystore.verify(packet.copy(binding = "00".repeat(32))))
        assertFalse(graph.keystore.verify(packet.copy(decision = PolicyDecision.HOLD)))
        assertFalse(graph.keystore.verify(packet.copy(rawContentExported = true)))
        assertFalse(graph.keystore.verify(packet.copy(nonce = "changed")))
        assertEquals(Fixtures.START_BALANCE, state.balance)
    }
    @Test fun privateKeyHasNoExportEncoding() {
        fresh(); createReview(DemoScenario.NORMAL); authorizeOnly()
        graph.keystore.sign(BankEngine.attest(state, state.currentTransferId!!, graph.clock.now(), graph.keystore.keyId))
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertNull(store.getKey("saeon.trace.attestation.v1", null).encoded)
    }
    @Test fun roomSnapshotRoundTripsHeldStateExactly() {
        evaluated(DemoScenario.IMPERSONATION)
        val encoded = SnapshotCodec.encode(state)
        val decoded = SnapshotCodec.decode(encoded)
        assertEquals(state, decoded)
        runBlocking { repository.initialize() }
        assertEquals(TransferStage.HOLD, state.current!!.stage)
        assertEquals(Fixtures.START_BALANCE, state.balance)
    }
    @Test fun interruptedAuthorizationIsRecoveredWithoutCommit() {
        fresh(); createReview(DemoScenario.NORMAL); authorizeOnly()
        runBlocking { repository.initialize() }
        assertEquals(TransferStage.REVIEW, state.current!!.stage)
        assertNull(state.current!!.authorization)
        assertTrue(state.receipts.none { !it.seed })
        assertEquals(Fixtures.START_BALANCE, state.balance)
    }
    @Test fun runtimeHasNoNetworkSmsContactsOrCallLogPermission() {
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        listOf(Manifest.permission.INTERNET, Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION).forEach { assertFalse("Unexpected permission $it", it in permissions) }
    }
    @Test fun favoritesDoNotChangeRecipientRiskIdentity() {
        fresh(DemoScenario.IMPERSONATION)
        runBlocking { graph.preferences.favorite(Fixtures.kim.id, true) }
        assertFalse(state.recipients.first { it.id == Fixtures.kim.id }.known)
        createReview(DemoScenario.IMPERSONATION); authorizeOnly(); finish()
        assertEquals(TransferStage.HOLD, state.current!!.stage)
    }
    @Test fun staleAuthorizationCallbackCannotApproveNewIntent() {
        fresh(); createReview(DemoScenario.NORMAL)
        val oldId = state.currentTransferId!!
        val old = runBlocking { repository.prepare(oldId) }
        runBlocking {
            repository.cancelAuthorization(oldId)
            repository.editReview(oldId)
            repository.setDraft(TransferDraft(Fixtures.family, 100_000, Purpose.FAMILY))
            repository.reviewDraft()
        }
        try { runBlocking { repository.authorize(old, AuthMethod.DEMO_CONFIRMATION) }; fail("Old callback accepted") }
        catch (expected: BankFailure) { assertEquals("STALE_CALLBACK", expected.code) }
        assertTrue(state.receipts.none { !it.seed })
        assertNull(state.current!!.authorization)
    }
}
