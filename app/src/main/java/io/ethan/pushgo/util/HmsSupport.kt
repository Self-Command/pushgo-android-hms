package io.ethan.pushgo.util

import android.content.Context
import com.huawei.hms.api.HuaweiApiAvailability
import io.ethan.pushgo.BuildConfig

object HmsSupport {
    fun isConfigured(): Boolean = BuildConfig.HMS_CONFIGURED
    fun isAvailable(context: Context): Boolean = isConfigured() && runCatching {
        HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context) == 0
    }.getOrDefault(false)
}
