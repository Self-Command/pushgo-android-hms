package io.ethan.pushgo.notifications

import android.content.Context
import androidx.work.*
import io.ethan.pushgo.PushGoApp
import io.ethan.pushgo.data.AndroidKeystoreSecretStore
import java.util.concurrent.TimeUnit

class HmsTokenSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as? PushGoApp)?.containerOrNull() ?: return Result.retry()
        val token = container.settingsRepository.getFcmToken() ?: return Result.retry()
        return try {
            container.settingsRepository.setFcmToken(token)
            container.channelRepository.syncProviderDeviceToken(token)
            container.channelRepository.syncSubscriptionsIfNeeded(token)
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { Result.retry() }
    }
    companion object {
        fun scheduleAcquisition(context: Context) {
            val work = OneTimeWorkRequestBuilder<HmsTokenAcquireWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork("pushgo-hms-token-acquire", ExistingWorkPolicy.KEEP, work)
        }
        fun accept(context: Context, token: String) {
            val normalized = token.trim().ifEmpty { return }
            AndroidKeystoreSecretStore(context.applicationContext).setHmsToken(normalized)
            val work = OneTimeWorkRequestBuilder<HmsTokenSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork("pushgo-hms-token-sync", ExistingWorkPolicy.REPLACE, work)
            (context.applicationContext as? PushGoApp)?.handlePushTokenUpdate(normalized)
        }
    }
}

class HmsTokenAcquireWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as? PushGoApp)?.containerOrNull() ?: return Result.retry()
        return try {
            if (container.pushTokenProvider.fetchToken(10_000) == null) Result.retry() else Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { Result.retry() }
    }
}
