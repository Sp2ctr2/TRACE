package app.saeon.trace.core
import org.junit.Test
import org.junit.Assert.*

class Bank7CoreTest {
    private val now=Fixtures.epoch
    private fun failure(block:()->Unit){try{block();fail("Expected ACCOUNT_LOCKED")}catch(e:BankFailure){assertEquals("ACCOUNT_LOCKED",e.code)}}
    private fun reviewed()=BankEngine.review(Fixtures.initial(),TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT),now)
    private fun active():BankState{val s=reviewed();val(p,ch)=BankEngine.prepare(s,s.currentTransferId!!,now);return BankEngine.authorize(p,ch,AuthMethod.DEMO_CONFIRMATION,now)}
    @Test fun freshAccountsAreUnlocked(){assertFalse(Fixtures.initial().accountLocked)}
    @Test fun lockBlocksDraftReview(){failure{BankEngine.review(Fixtures.initial().copy(accountLocked=true),TransferDraft(Fixtures.seoyeon,32000),now)}}
    @Test fun lockBlocksAuthenticationPreparation(){val s=reviewed().copy(accountLocked=true);failure{BankEngine.prepare(s,s.currentTransferId!!,now)}}
    @Test fun lockAddedAfterAuthorizationBlocksCommit(){val s=active();val packet=BankEngine.attest(s,s.currentTransferId!!,now,"test");failure{BankEngine.finish(s.copy(accountLocked=true),s.currentTransferId!!,packet,now,true)}}
    @Test fun recoveryPreservesLock(){assertTrue(BankEngine.recover(active().copy(accountLocked=true)).accountLocked)}
    @Test fun unlockedNormalFlowStillWorks(){val s=active();val r=BankEngine.finish(s,s.currentTransferId!!,BankEngine.attest(s,s.currentTransferId!!,now,"test"),now,true);assertEquals(Fixtures.START_BALANCE-32000,r.balance);assertEquals(TransferStage.COMPLETE,r.current!!.stage)}
    @Test fun lockedValidationCannotBeBypassedBySmallerAmount(){failure{BankEngine.validateAmount(Fixtures.initial().copy(accountLocked=true),1,Purpose.GENERAL,now)}}
    @Test fun terminalReplayDoesNotDebitAgainWhenLocked(){val s=active();val packet=BankEngine.attest(s,s.currentTransferId!!,now,"test");val done=BankEngine.finish(s,s.currentTransferId!!,packet,now,true).copy(accountLocked=true);assertEquals(done,BankEngine.finish(done,done.currentTransferId!!,packet,now,true))}
}
