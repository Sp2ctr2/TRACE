package app.saeon.trace

import androidx.compose.ui.test.*
import app.saeon.trace.core.*
import app.saeon.trace.ui.screens.BankDesignStore
import dev.trace.scene.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class SceneDeviceTest:UiHarness(){
    private fun raw()=context.assets.open("trace/design.json").bufferedReader().use{it.readText()}
    @After fun resetDesign(){runBlocking{BankDesignStore.get(context).clear()}}
    @Test fun importPreviewDoesNotChangeLedger(){
        fresh();val before=state;navigate("studio_design");tap("studio_example");waitScreen("studio_native_home")
        Assert.assertEquals(before,state);capture("30_Studio_Import")
        tap("studio_apply");compose.waitUntil(10000){BankDesignStore.get(context).document.value!=null}
        Assert.assertEquals(before,state);navigate("home");waitScreen("studio_native_home");capture("31_Studio_Home")
    }
    @Test fun customizedHoldCannotReleaseMoney(){
        evaluated(DemoScenario.IMPERSONATION);val before=state
        val doc=JSONObject(raw());val pages=doc.getJSONArray("screens");repeat(pages.length()){i->val p=pages.getJSONObject(i);if(p.optString("id")=="hold"){p.put("blocks",org.json.JSONArray());p.getJSONObject("footer").put("action","authorize")}}
        runBlocking{BankDesignStore.get(context).apply(doc.toString())};waitScreen("studio_native_hold")
        compose.onNodeWithText("아직 돈은 나가지 않았습니다.").assertExists();tap("scene_primary");waitScreen("trace_safety_guide")
        Assert.assertEquals(before.balance,state.balance);Assert.assertEquals(TransferStage.HOLD,state.current!!.stage);Assert.assertEquals(before.receipts,state.receipts)
        navigate("transfer_state");capture("32_Studio_Protected_Hold")
    }
    @Test fun nativeVectorsRenderInTheActualApp(){
        fresh();navigate("studio_design");tap("studio_example");tap("studio_page_picker");tap("studio_page_vectors",scroll=true);waitScreen("studio_native_vectors");capture("33_Studio_Vectors")
    }
    @Test fun documentGuardsRejectInvalidShapeAndSize(){
        val raw=raw();Assert.assertEquals("guide",SceneDocument.safeAction("hold","authorize"));Assert.assertEquals("cancel",SceneDocument.safeSecondary("hold","success"))
        Assert.assertTrue(runCatching{SceneDocument.parse("{}")} .isFailure)
        Assert.assertTrue(runCatching{SceneDocument.parse(raw.replace("trace.studio/3","unknown/schema"))}.isFailure)
        val d=SceneDocument.parse(raw);Assert.assertEquals("home",d.page("home").getString("id"))
    }
    @Test fun customizedReviewStillUsesNativeAuthenticationAndOneCommit(){
        fresh();createReview(DemoScenario.NORMAL);runBlocking{BankDesignStore.get(context).apply(raw())};waitScreen("studio_native_review")
        tap("scene_primary");tap("auth_confirm");waitScreen("transfer_complete")
        Assert.assertEquals(12_808_000L,state.balance);Assert.assertEquals(1,state.receipts.count{!it.seed})
    }
}
