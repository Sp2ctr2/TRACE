package app.saeon.trace

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriticalSingle11Test:UiHarness(){
    private fun reset(){
        fresh()
        compose.runOnIdle{compose.activity.model.uiTestAuthentication=true}
        Thread.sleep(700)
    }
    @Test fun review(){
        reset();createReview(DemoScenario.NORMAL);capture("04_transfer_review",audit=true)
    }
    @Test fun gate(){
        reset();createReview(DemoScenario.NORMAL);authorizeOnly();Thread.sleep(180);capture("05_trace_check",audit=false)
    }
    @Test fun complete(){
        reset();createReview(DemoScenario.NORMAL)
        val transferId=state.currentTransferId!!
        runBlocking {
            repository.change { current, now ->
                val rec=current.record(transferId)
                val after=current.balance-rec.intent.amount
                val receipt=TransferReceipt("SHOT-"+transferId.take(8),transferId,rec.intent.recipient,rec.intent.amount,now,rec.intent.purpose,after,"shot-"+transferId)
                current.withRecord(rec.copy(stage=TransferStage.COMPLETE,decision=PolicyDecision.ALLOW))
                    .copy(balance=after,receipts=listOf(receipt)+current.receipts,draft=null)
            }
        }
        navigate("transfer_state");waitScreen("transfer_complete");capture("06_allow_complete",audit=true)
    }
    @Test fun demo(){
        reset();compose.runOnIdle{compose.activity.model.stageUnlock()};navigate("presenter");waitScreen("demo_center");capture("11_demo_center",audit=true)
    }
}
