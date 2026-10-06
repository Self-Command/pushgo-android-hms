package io.ethan.pushgo.notifications

import android.content.Context
import androidx.work.*
import io.ethan.pushgo.PushGoApp
import io.ethan.pushgo.data.AndroidKeystoreSecretStore
import io.ethan.pushgo.data.PushChannelType
import io.ethan.pushgo.util.HmsSupport
import java.util.concurrent.TimeUnit

class HmsTokenSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? PushGoApp ?: return Result.retry()
        val container = app.containerOrNull() ?: return Result.retry()
        if (!HmsSupport.isConfigured() || container.settingsRepository.getPushChannelType() != PushChannelType.HMS) return Result.success()
        val token = container.settingsRepository.getHmsToken() ?: return Result.retry()
        return try {
            if (app.syncProviderToken(PushChannelType.HMS, token)) Result.success() else Result.retry()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { Result.retry() }
    }
    companion object {
        fun scheduleAcquisition(context: Context) {
            if (!HmsSupport.isConfigured()) return
            val work = OneTimeWorkRequestBuilder<HmsTokenAcquireWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork("pushgo-hms-token-acquire", ExistingWorkPolicy.KEEP, work)
        }
        fun accept(context: Context, token: String) {
            if (!HmsSupport.isConfigured()) return
            val normalized = token.trim().ifEmpty { return }
            AndroidKeystoreSecretStore(context.applicationContext).setHmsToken(normalized)
            val work = OneTimeWorkRequestBuilder<HmsTokenSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork("pushgo-hms-token-sync", ExistingWorkPolicy.REPLACE, work)
            (context.applicationContext as? PushGoApp)?.handleProviderTokenUpdate(PushChannelType.HMS, normalized)
        }
    }
}

class HmsTokenAcquireWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as? PushGoApp)?.containerOrNull() ?: return Result.retry()
        if (!HmsSupport.isConfigured() || container.settingsRepository.getPushChannelType() != PushChannelType.HMS) return Result.success()
        return try {
            if (container.pushTokenProvider.fetchToken(10_000) == null) Result.retry() else Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { Result.retry() }
    }
}
