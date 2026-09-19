package dev.sp2ctr2.saeon.domain

import kotlin.test.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JvmSigner:ProofSigner {
    private val pair=KeyPairGenerator.getInstance("EC").apply{initialize(ECGenParameterSpec("secp256r1"))}.generateKeyPair()
    override fun sign(payload:ByteArray):ByteArray=Signature.getInstance("SHA256withECDSA").run{initSign(pair.private);update(payload);sign()}
    override fun verify(payload:ByteArray,signature:ByteArray):Boolean=runCatching{Signature.getInstance("SHA256withECDSA").run{initVerify(pair.public);update(payload);verify(signature)}}.getOrDefault(false)
}
class BankEngineTest {
    private val now=Fixtures.CLOCK
    private val signer=JvmSigner()
    private fun review(scenario:Scenario=Scenario.NORMAL,amount:Long=Fixtures.defaultAmount(scenario)):BankState {
        val s=Fixtures.seed(0,scenario)
        return BankEngine.review(BankEngine.begin(s,Fixtures.defaultRecipient(scenario),amount,now))
    }
    private fun send(s:BankState):BankState=BankEngine.commit(s,BankEngine.authorize(s,signer,now),signer,now)
    @Test fun initialLedgerAndAssetsAgree(){val s=Fixtures.seed(0);assertTrue(BankEngine.ledgerIsConsistent(s));assertEquals(15_240_000L,s.totalAssets);assertEquals(7_240_000L,s.netAssets)}
    @Test fun normalCompletes(){val s=send(review());assertEquals(12_808_000L,s.balance);assertEquals(1,s.receipts.size);assertEquals(Phase.COMPLETE,s.draft!!.phase);assertTrue(BankEngine.ledgerIsConsistent(s))}
    @Test fun doubleSubmitIsIdempotent(){val s=review();val p=BankEngine.authorize(s,signer,now);val once=BankEngine.commit(s,p,signer,now);assertEquals(once,BankEngine.commit(once,p,signer,now+10))}
    @Test fun holdCannotCommitEvenWithoutAssess(){val s=review(Scenario.IMPERSONATION);assertFailsWith<BankRuleException>{send(s)};assertEquals(Fixtures.INITIAL_BALANCE,s.balance)}
    @Test fun assessedHoldCannotAuthorize(){val s=BankEngine.assess(review(Scenario.IMPERSONATION),now);assertEquals(Phase.HOLD,s.draft!!.phase);assertFailsWith<BankRuleException>{BankEngine.authorize(s,signer,now)}}
    @Test fun holdSurvivesSerialization(){val s=BankEngine.assess(review(Scenario.IMPERSONATION),now);val restored=Json.decodeFromString<BankState>(Json.encodeToString(s));assertTrue(restored.heldCase);assertEquals(Decision.HOLD,BankEngine.evaluate(restored,restored.draft!!,now))}
    @Test fun holdDoesNotExpireIntoAllow(){val s=BankEngine.assess(review(Scenario.IMPERSONATION),now);assertEquals(Decision.HOLD,BankEngine.evaluate(s.copy(risks=emptyList()),s.draft!!,now+99_000_000))}
    @Test fun cancelDoesNotClearCase(){val held=BankEngine.assess(review(Scenario.IMPERSONATION),now);val cancelled=BankEngine.cancel(held);val next=BankEngine.begin(cancelled,Fixtures.friend,1,now);assertEquals(Decision.HOLD,BankEngine.evaluate(next,next.draft!!,now))}
    @Test fun heldDraftCannotBeEdited(){val held=BankEngine.assess(review(Scenario.IMPERSONATION),now);assertFailsWith<BankRuleException>{BankEngine.edit(held,1)}}
    @Test fun warnNeedsAcknowledgement(){val s=review(Scenario.WARN);assertEquals(Decision.WARN,BankEngine.evaluate(s,s.draft!!,now));assertFailsWith<BankRuleException>{send(s)}}
    @Test fun warningRequiresActualWarnPhase(){assertFailsWith<BankRuleException>{BankEngine.acknowledgeWarning(review())}}
    @Test fun warnCanReturnToReview(){val s=BankEngine.acknowledgeWarning(BankEngine.assess(review(Scenario.WARN),now));assertEquals(Phase.REVIEW,s.draft!!.phase);assertEquals(Phase.COMPLETE,send(s).draft!!.phase)}
    @Test fun amountChangeDropsWarningConsent(){val s=BankEngine.acknowledgeWarning(BankEngine.assess(review(Scenario.WARN),now));val changed=BankEngine.edit(s,1);assertFalse(changed.draft!!.warnAcknowledged);assertNotEquals(s.draft!!.id,changed.draft!!.id)}
    @Test fun loanNeedsRoute(){val s=review(Scenario.LOAN);assertEquals(Decision.VERIFY,BankEngine.evaluate(s,s.draft!!,now));assertFailsWith<BankRuleException>{send(s)}}
    @Test fun lookupDoesNotDebit(){val s=BankEngine.lookupRoute(BankEngine.assess(review(Scenario.LOAN),now),now,false);assertEquals(Fixtures.INITIAL_BALANCE,s.balance);assertTrue(s.receipts.isEmpty());assertEquals(Phase.ROUTE,s.draft!!.phase)}
    @Test fun routeCreatesNewTransaction(){val s=BankEngine.lookupRoute(BankEngine.assess(review(Scenario.LOAN),now),now,false);val next=BankEngine.acceptRoute(s,now);assertNotEquals(s.draft!!.id,next.draft!!.id);assertEquals(Fixtures.official,next.draft!!.recipient);assertEquals(Phase.REVIEW,next.draft!!.phase)}
    @Test fun officialRepaymentUpdatesBothBalances(){val s=BankEngine.acceptRoute(BankEngine.lookupRoute(BankEngine.assess(review(Scenario.LOAN),now),now,false),now);val paid=send(s);assertEquals(4_840_000L,paid.balance);assertEquals(0L,paid.loan);assertEquals(1,paid.receipts.size);assertTrue(BankEngine.ledgerIsConsistent(paid))}
    @Test fun unknownNeverFallsBack(){val s=BankEngine.lookupRoute(BankEngine.assess(review(Scenario.LOOKUP_FAILURE),now),now,true);assertEquals(Phase.UNKNOWN,s.draft!!.phase);assertEquals(Fixtures.INITIAL_BALANCE,s.balance);assertTrue(s.receipts.isEmpty());assertFailsWith<BankRuleException>{send(s)}}
    @Test fun expiredRouteRejected(){val s=BankEngine.lookupRoute(BankEngine.assess(review(Scenario.LOAN),now),now,false);assertFailsWith<BankRuleException>{BankEngine.acceptRoute(s,now+120_001)}}
    @Test fun changedAmountInvalidatesPermit(){val s=BankEngine.acceptRoute(BankEngine.lookupRoute(BankEngine.assess(review(Scenario.LOAN),now),now,false),now);val changed=BankEngine.review(BankEngine.edit(s,100));assertNull(changed.draft!!.route);assertEquals(Decision.VERIFY,BankEngine.evaluate(changed,changed.draft!!,now))}
    @Test fun changedRecipientInvalidatesSignature(){val s=review();val proof=BankEngine.authorize(s,signer,now);val changed=s.copy(draft=s.draft!!.copy(recipient=Fixtures.family));assertFailsWith<BankRuleException>{BankEngine.commit(changed,proof,signer,now)}}
    @Test fun changedAmountInvalidatesSignature(){val s=review();val proof=BankEngine.authorize(s,signer,now);val changed=s.copy(draft=s.draft!!.copy(amount=33_000));assertFailsWith<BankRuleException>{BankEngine.commit(changed,proof,signer,now)}}
    @Test fun changedPurposeInvalidatesSignature(){val s=review();val proof=BankEngine.authorize(s,signer,now);val changed=s.copy(draft=s.draft!!.copy(purpose=Purpose.OTHER));assertFailsWith<BankRuleException>{BankEngine.commit(changed,proof,signer,now)}}
    @Test fun resetEpochInvalidatesSignature(){val s=review();val proof=BankEngine.authorize(s,signer,now);assertFailsWith<BankRuleException>{BankEngine.commit(s.copy(epoch="reset"),proof,signer,now)}}
    @Test fun expiredProofRejected(){val s=review();val proof=BankEngine.authorize(s,signer,now);assertFailsWith<BankRuleException>{BankEngine.commit(s,proof,signer,now+60_001)}}
    @Test fun futureProofRejected(){val s=review();val proof=BankEngine.authorize(s,signer,now+1_000);assertFailsWith<BankRuleException>{BankEngine.commit(s,proof,signer,now)}}
    @Test fun tamperedSignatureRejected(){val s=review();val p=BankEngine.authorize(s,signer,now);assertFailsWith<BankRuleException>{BankEngine.commit(s,p.copy(signature=byteArrayOf(1,2,3)),signer,now)}}
    @Test fun wrongKeyRejected(){val s=review();val p=BankEngine.authorize(s,signer,now);assertFailsWith<BankRuleException>{BankEngine.commit(s,p,JvmSigner(),now)}}
    @Test fun zeroAmountRejected(){assertFailsWith<BankRuleException>{review(amount=0)}}
    @Test fun negativeAmountRejected(){assertFailsWith<BankRuleException>{review(amount=-1)}}
    @Test fun balanceExceededRejected(){assertFailsWith<BankRuleException>{review(amount=99_000_000)}}
    @Test fun perTransferLimitEnforced(){val s=review();assertNotNull(BankEngine.validationError(s.copy(dailyLimit=10_000),s.draft!!))}
    @Test fun overRepaymentRejected(){assertFailsWith<BankRuleException>{review(Scenario.LOAN,8_000_001)}}
    @Test fun expiredLinkNoLongerCreatesWarning(){val s=review(Scenario.WARN);assertEquals(Decision.ALLOW,BankEngine.evaluate(s,s.draft!!,now+16*60_000))}
    @Test fun futureSignalsIgnored(){val s=review().let{it.copy(risks=listOf(RiskEvent("x",RiskType.SUSPICIOUS_LINK,now+1000,now+99_000,EventSource.USER_SHARED,"signal")))};assertEquals(Decision.ALLOW,BankEngine.evaluate(s,s.draft!!,now))}
    @Test fun savingsMovePreservesAssets(){val s=Fixtures.seed(0);val next=BankEngine.bringFromSavings(s,100_000,now);assertEquals(s.totalAssets,next.totalAssets);assertEquals(12_940_000L,next.balance);assertEquals(2_300_000L,next.savings);assertTrue(BankEngine.ledgerIsConsistent(next))}
    @Test fun savingsOverdrawRejected(){assertFailsWith<BankRuleException>{BankEngine.bringFromSavings(Fixtures.seed(0),2_400_001,now)}}
    @Test fun serializedCompletedLedgerStaysConsistent(){val s=send(review());val restored=Json.decodeFromString<BankState>(Json.encodeToString(s));assertEquals(s,restored);assertTrue(BankEngine.ledgerIsConsistent(restored))}
    @Test fun normalMessageDoesNotCreateImpersonation(){val r=LocalRiskAnalyzer.analyze(Fixtures.message(Scenario.NORMAL),now);assertFalse(r.any{it.type==RiskType.IMPERSONATION})}
    @Test fun compoundImpersonationProducesThreeReasons(){val r=LocalRiskAnalyzer.analyze(Fixtures.message(Scenario.IMPERSONATION),now);assertTrue(r.map{it.type}.containsAll(listOf(RiskType.IMPERSONATION,RiskType.URGENCY,RiskType.FINANCIAL_INSTRUCTION)))}
    @Test fun rawTextIsNotRetained(){val unique="비밀_나만의_문장_123";val r=LocalRiskAnalyzer.analyze("지금 안전계좌로 이체하세요 $unique",now);assertFalse(Json.encodeToString(r).contains(unique))}
    @Test fun analyzerHasLengthBound(){assertFailsWith<IllegalArgumentException>{LocalRiskAnalyzer.analyze("a".repeat(8_001),now)}}
    @Test fun educationCaveatDoesNotPretendToBeClassifier(){assertTrue(LocalRiskAnalyzer.analyze("사기 예방 교육: 돈을 보내지 마세요",now).isEmpty())}
    @Test fun manyTransfersMaintainLedger(){var s=Fixtures.seed(0);repeat(100){s=BankEngine.review(BankEngine.begin(s,Fixtures.friend,1L,now+it));val p=BankEngine.authorize(s,signer,now+it);s=BankEngine.commit(s,p,signer,now+it);assertTrue(BankEngine.ledgerIsConsistent(s))};assertEquals(Fixtures.INITIAL_BALANCE-100,s.balance);assertEquals(100,s.receipts.map{it.transactionId}.distinct().size)}
}
