package io.ethan.pushgo.notifications

import android.os.Bundle
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

class HuaweiMessagingService : HmsMessageService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.dataOfMap.takeIf { it.isNotEmpty() }
            ?: InboundMessagePayloadCodec.decode(message.data.orEmpty()) ?: return
        InboundMessageWorker.enqueue(applicationContext, data, message.messageId)
    }
    override fun onNewToken(token: String) { HmsTokenSyncWorker.accept(applicationContext, token) }
    override fun onNewToken(token: String, bundle: Bundle) { HmsTokenSyncWorker.accept(applicationContext, token) }
    override fun onTokenError(error: Exception) { HmsTokenSyncWorker.scheduleAcquisition(applicationContext) }
    override fun onTokenError(error: Exception, bundle: Bundle) { onTokenError(error) }
}
