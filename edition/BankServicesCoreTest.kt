package app.saeon.trace.core

import org.junit.Assert.*
import org.junit.Test

class BankServicesCoreTest {
    private val now=Fixtures.epoch
    private fun failure(code:String?=null,block:()->Unit) {
        try { block();fail("Expected transaction rejection") }
        catch(e:BankFailure){if(code!=null)assertEquals(code,e.code)}
    }
    @Test fun savingsConservesTotal() {
        val initial=Fixtures.initial()
        val result=BankEngine.saveToSavings(initial,100000,"save-1",now)
        assertEquals(initial.balance-100000,result.balance)
        assertEquals(initial.savings+100000,result.savings)
        assertEquals(initial.balance+initial.savings,result.balance+result.savings)
        assertEquals(1,result.receipts.count{it.intentId=="save-1"})
    }
    @Test fun savingsRetryDoesNotDebitTwice() {
        val first=BankEngine.saveToSavings(Fixtures.initial(),50000,"same-operation",now)
        assertEquals(first,BankEngine.saveToSavings(first,50000,"same-operation",now+1))
    }
    @Test fun zeroSavingsIsRejected(){failure("SAVINGS_AMOUNT"){BankEngine.saveToSavings(Fixtures.initial(),0,"zero",now)}}
    @Test fun negativeSavingsIsRejected(){failure("SAVINGS_AMOUNT"){BankEngine.saveToSavings(Fixtures.initial(),-1,"negative",now)}}
    @Test fun savingsOverdraftIsRejected(){failure("SAVINGS_AMOUNT"){BankEngine.saveToSavings(Fixtures.initial(),Fixtures.START_BALANCE+1,"too-much",now)}}
    @Test fun lockedAccountCannotReviewNewTransfer() {
        val locked=BankEngine.lockAccount(Fixtures.initial(),true)
        failure("ACCOUNT_LOCKED"){BankEngine.review(locked,TransferDraft(Fixtures.seoyeon,32000),now)}
    }
    @Test fun lockedAccountCannotBringSavings() {
        failure("ACCOUNT_LOCKED"){BankEngine.bringFromSavings(BankEngine.lockAccount(Fixtures.initial(),true),1000,"bring",now)}
    }
    @Test fun lockedAccountCannotSave() {
        failure("ACCOUNT_LOCKED"){BankEngine.saveToSavings(BankEngine.lockAccount(Fixtures.initial(),true),1000,"save",now)}
    }
    @Test fun lockingInvalidatesAuthorizationWithoutMovingMoney() {
        val reviewed=BankEngine.review(Fixtures.initial(),TransferDraft(Fixtures.seoyeon,32000),now)
        val (pending,challenge)=BankEngine.prepare(reviewed,reviewed.currentTransferId!!,now)
        val authorized=BankEngine.authorize(pending,challenge,AuthMethod.DEMO_CONFIRMATION,now)
        val packet=BankEngine.attest(authorized,authorized.currentTransferId!!,now,"key")
        val locked=BankEngine.lockAccount(authorized,true)
        assertEquals(Fixtures.START_BALANCE,locked.balance)
        assertNull(locked.current!!.authorization)
        failure{BankEngine.finish(locked,locked.currentTransferId!!,packet,now,true)}
    }
    @Test fun unlockDoesNotRestoreAnOldAuthorization() {
        val reviewed=BankEngine.review(Fixtures.initial(),TransferDraft(Fixtures.seoyeon,32000),now)
        val (pending,challenge)=BankEngine.prepare(reviewed,reviewed.currentTransferId!!,now)
        val unlocked=BankEngine.lockAccount(BankEngine.lockAccount(pending,true),false)
        assertFalse(unlocked.accountLocked)
        failure{BankEngine.authorize(unlocked,challenge,AuthMethod.DEMO_CONFIRMATION,now)}
        assertEquals(Fixtures.START_BALANCE,unlocked.balance)
    }
    @Test fun internalRoundTripIsConservative() {
        val initial=Fixtures.initial()
        val saved=BankEngine.saveToSavings(initial,230000,"save-trip",now)
        val brought=BankEngine.bringFromSavings(saved,230000,"bring-trip",now)
        assertEquals(initial.balance,brought.balance)
        assertEquals(initial.savings,brought.savings)
    }
    @Test fun completedInternalOperationCanBeReadAfterLocking() {
        val saved=BankEngine.saveToSavings(Fixtures.initial(),1000,"completed",now)
        val locked=BankEngine.lockAccount(saved,true)
        assertEquals(locked,BankEngine.saveToSavings(locked,1000,"completed",now+10))
    }
}
