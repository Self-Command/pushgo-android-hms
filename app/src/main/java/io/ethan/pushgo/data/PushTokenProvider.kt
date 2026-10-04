package io.ethan.pushgo.data

import android.content.Context
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import io.ethan.pushgo.notifications.HmsTokenSyncWorker
import kotlinx.coroutines.*

interface PushTokenProvider { suspend fun fetchToken(timeoutMs: Long): String? }

/** One independent getToken request survives waiter timeouts. */
class HuaweiPushTokenProvider(context: Context) : PushTokenProvider {
    private val appContext = context.applicationContext
    private val delegate = SingleFlightPushTokenProvider(
        fetch = {
            val appId = AGConnectServicesConfig.fromContext(appContext).getString("client/app_id")
            require(!appId.isNullOrBlank()) { "Huawei app ID missing" }
            HmsInstanceId.getInstance(appContext).getToken(appId, "HCM")
        },
        onToken = { HmsTokenSyncWorker.accept(appContext, it) },
    )
    override suspend fun fetchToken(timeoutMs: Long): String? = delegate.fetchToken(timeoutMs)
}

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
                val token = fetch()?.trim()?.ifEmpty { null }
                token?.let(onToken)
                token
            }.also { inFlight = it }
        }
        return withTimeoutOrNull(timeoutMs) { request.await() }
    }
}
