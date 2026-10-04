package io.ethan.pushgo.util
import android.content.Context
import com.huawei.hms.api.HuaweiApiAvailability
/** Legacy UI symbol; this checks HMS only. Provider mode remains selected when unavailable. */
object FcmSupport {
    fun isAvailable(context: Context): Boolean = runCatching {
        HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context) == 0
    }.getOrDefault(false)
}
