package io.ethan.pushgo.web

import android.os.SystemClock
import android.content.ContentValues
import android.provider.MediaStore
import android.util.Base64
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebActionDeviceTest {
 @Test fun httpsPageSelectsPhotoThroughSystemChooserAndUploadsInsideApp() {
  val url=InstrumentationRegistry.getArguments().getString("webActionFixtureUrl")
  assumeTrue("Action-only isolated HTTPS fixture required",url!=null)
  val context=ApplicationProvider.getApplicationContext<Context>()
  val values=ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME,"web-action-ci.png");put(MediaStore.MediaColumns.MIME_TYPE,"image/png");put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/") }
  val photoUri=context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values)!!
  context.contentResolver.openOutputStream(photoUri)!!.use { stream -> stream.write(Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+Xl1sAAAAASUVORK5CYII=",Base64.DEFAULT)) }
  val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
  try { ActivityScenario.launch<WebActionActivity>(WebActionActivity.intent(context,WebAction(url!!,"任务照片"))).use {
   assertTrue(device.wait(Until.hasObject(By.text("从相册选择")),20000))
   device.findObject(By.text("从相册选择")).click()
   SystemClock.sleep(1000)
   // Cancel the system picker first; the original page must remain usable.
   device.pressBack();assertTrue(device.wait(Until.hasObject(By.text("从相册选择")),10000))
   device.findObject(By.text("从相册选择")).click()
   SystemClock.sleep(1000)
   val drawer=device.findObject(By.descContains("Show roots"))?:device.findObject(By.descContains("Navigation drawer"))
   drawer?.click()
   device.wait(Until.findObject(By.text("Downloads")),10000)?.click()
   val photo=device.wait(Until.findObject(By.text("web-action-ci.png")),15000)
   assertNotNull("System picker did not expose the isolated fixture photo",photo);photo!!.click()
   assertTrue(device.wait(Until.hasObject(By.text("照片已选择")),15000))
   device.findObject(By.text("提交照片")).click()
   assertTrue(device.wait(Until.hasObject(By.text("照片已上传")),15000))
   assertEquals("io.ethan.pushgo",device.currentPackageName)
   device.takeScreenshot(java.io.File(context.getExternalFilesDir(null),"web-action-upload.png"))
  } } finally {context.contentResolver.delete(photoUri,null,null)}
 }
}
