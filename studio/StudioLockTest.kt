package app.saeon.trace.core
import org.junit.Assert.*
import org.junit.Test
class StudioLockTest {
 private val now=Fixtures.epoch
 private fun reviewed():BankState=BankEngine.review(Fixtures.initial(DemoScenario.NORMAL,now),TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT),now)
 private fun blocked(action:()->Unit){try{action();fail("Expected account lock") }catch(e:BankFailure){assertEquals("ACCOUNT_LOCKED",e.code)}}
 @Test fun cannotReviewLockedAccount(){blocked{BankEngine.review(reviewed().copy(accountLocked=true),TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT),now)}}
 @Test fun cannotPrepareLockedAccount(){val s=reviewed().copy(accountLocked=true);blocked{BankEngine.prepare(s,s.currentTransferId!!,now)}}
 @Test fun cannotAuthorizeAfterLock(){val r=reviewed();val(s,c)=BankEngine.prepare(r,r.currentTransferId!!,now);blocked{BankEngine.authorize(s.copy(accountLocked=true),c,AuthMethod.DEMO_CONFIRMATION,now)}}
 @Test fun lockBetweenAuthenticationAndCommitPreventsDebit(){
  val r=reviewed();val(p,c)=BankEngine.prepare(r,r.currentTransferId!!,now)
  val a=BankEngine.authorize(p,c,AuthMethod.DEMO_CONFIRMATION,now)
  val packet=BankEngine.attest(a,a.currentTransferId!!,now,"unit")
  blocked{BankEngine.finish(a.copy(accountLocked=true),a.currentTransferId!!,packet,now,true)}
  assertEquals(Fixtures.START_BALANCE,a.balance)
 }
 @Test fun unlockDoesNotGrantAuthorization(){val r=reviewed();val(s,c)=BankEngine.prepare(r,r.currentTransferId!!,now);blocked{BankEngine.authorize(s.copy(accountLocked=true),c,AuthMethod.DEMO_CONFIRMATION,now)};val reopened=BankEngine.cancelAuthorization(s.copy(accountLocked=false),c.intentId);assertEquals(TransferStage.REVIEW,reopened.current!!.stage);assertNull(reopened.current!!.authorization)}
}
