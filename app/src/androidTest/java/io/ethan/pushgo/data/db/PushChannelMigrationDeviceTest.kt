package io.ethan.pushgo.data.db

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.ethan.pushgo.data.AndroidKeystoreSecretStore
import io.ethan.pushgo.data.PushChannelType
import io.ethan.pushgo.data.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PushChannelMigrationDeviceTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "push-channel-migration-test.db"
    // Exercise the real encrypted store without sharing preferences with the
    // running application's Firebase callbacks during instrumentation.
    private val secrets get() = AndroidKeystoreSecretStore(object : ContextWrapper(context) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences("push-channel-migration-$name", mode)
    })
    private val cache get() = context.getSharedPreferences("push-channel-migration-test", Context.MODE_PRIVATE)

    @Before fun prepare() { context.deleteDatabase(name); secrets.clearAll(); cache.edit().clear().commit() }
    @After fun cleanup() { context.deleteDatabase(name); secrets.clearAll(); cache.edit().clear().commit() }

    private suspend fun seedV30(useFcm: Boolean, legacyHms: Boolean) {
        val database = PushGoDatabase.buildForTest(context, name)
        database.appSettingsDao().upsert(AppSettingsEntity(
            serverAddress = "http://192.0.2.1:6666", token = null,
            notificationKeyUpdatedAt = null, fcmToken = null, useFcmChannel = useFcm,
        ))
        database.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
            // The official v30 schema differs only by the new nullable column.
            it.execSQL("ALTER TABLE app_settings DROP COLUMN push_channel_type")
            it.version = 30
        }
        secrets.setDeviceKey("unchanged-device-identity")
        secrets.setFcmToken("real-fcm-token")
        if (legacyHms) secrets.setHmsToken("legacy-hms-token")
    }

    @Test fun legacyHmsMigratesWithoutCrossRegisteringFcmAndKeepsIdentity() = runBlocking {
        seedV30(useFcm = true, legacyHms = true)
        val database = PushGoDatabase.buildForTest(context, name)
        try {
            val settings = SettingsRepository(database.appSettingsDao(), secrets, cache)
            assertEquals(PushChannelType.HMS, settings.getPushChannelType())
            assertEquals("legacy-hms-token", settings.getProviderToken())
            settings.setFcmToken("late-inactive-fcm-token")
            assertEquals(PushChannelType.HMS, settings.getPushChannelType())
            assertEquals("legacy-hms-token", settings.getProviderToken())
            assertEquals("late-inactive-fcm-token", settings.getFcmToken())
            assertEquals("unchanged-device-identity", settings.getDeviceKey())
            assertEquals("http://192.0.2.1:6666", settings.getServerAddress())
            assertEquals(31, database.openHelper.writableDatabase.version)
        } finally { database.close() }
    }

    @Test fun officialPrivateChoiceSurvivesV30Upgrade() = runBlocking {
        seedV30(useFcm = false, legacyHms = false)
        val database = PushGoDatabase.buildForTest(context, name)
        try {
            val settings = SettingsRepository(database.appSettingsDao(), secrets, cache)
            assertEquals(PushChannelType.PRIVATE, settings.getPushChannelType())
            assertEquals(null, settings.getProviderToken())
            assertEquals("real-fcm-token", settings.getFcmToken())
        } finally { database.close() }
    }
}
