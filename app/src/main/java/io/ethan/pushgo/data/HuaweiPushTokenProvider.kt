package io.ethan.pushgo.data

import android.content.Context
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import io.ethan.pushgo.notifications.HmsTokenSyncWorker
import io.ethan.pushgo.util.HmsSupport
import kotlinx.coroutines.*

class HuaweiPushTokenProvider(context: Context) : PushTokenProvider {
    private val appContext = context.applicationContext
    private val delegate = SingleFlightPushTokenProvider(
        fetch = {
            check(HmsSupport.isConfigured()) { "HMS is not configured in this APK" }
            val appId = AGConnectServicesConfig.fromContext(appContext).getString("client/app_id")
            require(!appId.isNullOrBlank()) { "Huawei app ID missing" }
            HmsInstanceId.getInstance(appContext).getToken(appId, "HCM")
        },
        onToken = { HmsTokenSyncWorker.accept(appContext, it) },
    )
    override suspend fun fetchToken(timeoutMs: Long): String? = delegate.fetchToken(timeoutMs)
}

/** A waiter timeout must not abort the SDK operation or discard its late token. */
internal class SingleFlightPushTokenProvider(
    private val fetch: () -> String?,
    private val onToken: (String) -> Unit,
) : PushTokenProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var inFlight: Deferred<String?>? = null
    override suspend fun fetchToken(timeoutMs: Long): String? {
        val request = synchronized(lock) {
            inFlight?.takeIf { !it.isCompleted } ?: scope.async {
                fetch()?.trim()?.ifEmpty { null }?.also(onToken)
            }.also { inFlight = it }
        }
        return withTimeoutOrNull(timeoutMs) { request.await() }
    }
}

class SelectedPushTokenProvider(
    private val settings: SettingsRepository,
    private val firebase: PushTokenProvider,
    private val huawei: PushTokenProvider,
) : PushTokenProvider {
    suspend fun fetchFor(type: PushChannelType, timeoutMs: Long): String? {
        val token = when (type) {
            PushChannelType.FCM -> firebase.fetchToken(timeoutMs)
            PushChannelType.HMS -> huawei.fetchToken(timeoutMs)
            PushChannelType.PRIVATE -> null
        }
        token?.let { settings.setProviderToken(type, it) }
        return token
    }
    override suspend fun fetchToken(timeoutMs: Long): String? {
        val type = settings.getPushChannelType()
        val token = fetchFor(type, timeoutMs)
        check(type == settings.getPushChannelType()) { "Push channel changed during token acquisition" }
        return token
    }
}
