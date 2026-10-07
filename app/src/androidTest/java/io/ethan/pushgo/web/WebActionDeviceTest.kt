package io.ethan.pushgo.web

import android.os.SystemClock
import android.content.ContentValues
import android.provider.MediaStore
import android.graphics.Bitmap
import android.graphics.Color
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebActionDeviceTest {
 private fun clickFresh(device: UiDevice, selector: BySelector, timeout: Long = 15000) {
  val deadline = SystemClock.uptimeMillis() + timeout
  while (SystemClock.uptimeMillis() < deadline) {
   try {
    val view = device.wait(Until.findObject(selector), 1000)
    if (view != null) { view.click(); return }
   } catch (_: StaleObjectException) { /* system picker relayout; locate its current node */ }
  }
  val context=ApplicationProvider.getApplicationContext<Context>()
  val hierarchy=java.io.File(context.filesDir,"web-action-failure.xml")
  device.dumpWindowHierarchy(hierarchy)
  android.util.Log.e("WebActionUiFixture",hierarchy.readText())
  device.takeScreenshot(java.io.File(context.filesDir,"web-action-failure.png"))
  throw AssertionError("UI action did not become available: $selector")
 }

 @Test fun httpsPageSelectsPhotoThroughSystemChooserAndUploadsInsideApp() {
  val url=InstrumentationRegistry.getArguments().getString("webActionFixtureUrl")
  assumeTrue("Action-only isolated HTTPS fixture required",url!=null)
  val context=ApplicationProvider.getApplicationContext<Context>()
  val values=ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME,"web-action-ci.png");put(MediaStore.MediaColumns.MIME_TYPE,"image/png");put(MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/") }
  val photoUri=context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)!!
  context.contentResolver.openOutputStream(photoUri)!!.use { stream -> val bitmap=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) };bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);bitmap.recycle() }
  val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
  try { ActivityScenario.launch<WebActionActivity>(WebActionActivity.intent(context,WebAction(url!!,"任务照片"))).use {
   clickFresh(device, By.text("从相册选择"),60000)
   SystemClock.sleep(1000)
   // Cancel the system picker first; the original page must remain usable.
   device.pressBack();assertTrue(device.wait(Until.hasObject(By.text("从相册选择")),10000))
   clickFresh(device, By.text("从相册选择"))
   SystemClock.sleep(1000)
   val drawer=device.findObject(By.descContains("Show roots"))?:device.findObject(By.descContains("Navigation drawer"))
   drawer?.click()
   clickFresh(device, By.text("Images"))
   if(device.wait(Until.hasObject(By.text("Pictures")),3000))clickFresh(device,By.text("Pictures"))
   clickFresh(device, By.text("web-action-ci.png"))
   assertTrue(device.wait(Until.hasObject(By.text("照片已选择")),15000))
   clickFresh(device, By.text("提交照片"))
   assertTrue(device.wait(Until.hasObject(By.text("照片已上传")),15000))
   assertEquals("io.ethan.pushgo",device.currentPackageName)
   device.takeScreenshot(java.io.File(context.getExternalFilesDir(null),"web-action-upload.png"))
  } } finally {context.contentResolver.delete(photoUri,null,null)}
 }
}
