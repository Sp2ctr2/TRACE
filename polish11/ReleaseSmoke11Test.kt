package app.saeon.trace

import android.content.Intent
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReleaseSmoke11Test {
 @Test fun actualReleaseHome(){
  val inst=InstrumentationRegistry.getInstrumentation()
  val context=inst.targetContext
  val device=UiDevice.getInstance(inst)
  val intent=context.packageManager.getLaunchIntentForPackage(context.packageName)!!
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
  context.startActivity(intent)
  assertTrue("TRACE release home did not appear",device.wait(Until.hasObject(By.text("TRACE")),20000))
  assertTrue(device.wait(Until.hasObject(By.text("송금하기")),10000))
  Thread.sleep(1400)
  val out=File(context.getExternalFilesDir(null),"verification/release11").apply{mkdirs()}
  val bmp=requireNotNull(inst.uiAutomation.takeScreenshot())
  File(out,"release_home.png").outputStream().use{bmp.compress(Bitmap.CompressFormat.PNG,100,it)};bmp.recycle()
  device.findObject(By.text("자산")).click()
  assertTrue(device.wait(Until.hasObject(By.text("내가 보유한 자산")),10000))
  Thread.sleep(500)
  val second=requireNotNull(inst.uiAutomation.takeScreenshot())
  File(out,"release_assets.png").outputStream().use{second.compress(Bitmap.CompressFormat.PNG,100,it)};second.recycle()
  File(out,"release-smoke.txt").writeText("PASS: optimized APK launched; TRACE home and Assets interacted via UI automation.\n")
 }
}
