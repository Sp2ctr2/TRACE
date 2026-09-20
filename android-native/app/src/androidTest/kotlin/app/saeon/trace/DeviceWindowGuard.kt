package app.saeon.trace

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File

/** The API 35 Google APIs image can show a background Pixel Launcher ANR.
 * Record and close only that exact unrelated process dialog. Never dismiss an
 * application ANR or crash; those remain failures. Golden captures must not
 * silently pass while a system dialog obscures the app under test. */
object DeviceWindowGuard {
    fun clearKnownLauncherDialog(output: File) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        if (!device.hasObject(By.text("Pixel Launcher isn't responding"))) return
        val stamp = System.currentTimeMillis()
        output.mkdirs()
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(output, "environment-launcher-anr-$stamp.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
        File(output, "environment-events.txt").appendText("$stamp: unrelated Pixel Launcher ANR intercepted input; closing the launcher dialog before continuing.\n")
        val close = device.findObject(By.res("android", "aerr_close"))
            ?: device.findObject(By.text("Close app"))
        checkNotNull(close) { "Pixel Launcher ANR has no close control; cannot validate obscured UI" }
        close.click()
        check(device.wait(Until.gone(By.text("Pixel Launcher isn't responding")), 5_000)) {
            "Pixel Launcher ANR remained above the application"
        }
        device.waitForIdle(1_000)
    }
    fun assertNoCrashDialog() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        check(!device.hasObject(By.res("android", "aerr_close"))) {
            "An unexpected crash/ANR dialog obscures the application; screenshot is not a valid golden"
        }
    }
}
