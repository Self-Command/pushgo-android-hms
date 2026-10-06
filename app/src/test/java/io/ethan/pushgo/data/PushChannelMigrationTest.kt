package io.ethan.pushgo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PushChannelMigrationTest {
    @Test fun officialFcmInstallationKeepsItsChannel() {
        assertEquals(PushChannelType.FCM, PushChannelType.restore(null, true, false))
    }
    @Test fun officialPrivateInstallationKeepsItsChannel() {
        assertEquals(PushChannelType.PRIVATE, PushChannelType.restore(null, false, false))
    }
    @Test fun legacyHmsInstallationNeverBecomesFcmWithoutAgc() {
        // AGC availability does not affect the stored identity migration.
        assertEquals(PushChannelType.HMS, PushChannelType.restore(null, true, true))
    }
    @Test fun savedChoiceWinsOverInactiveCachedHmsToken() {
        assertEquals(PushChannelType.FCM, PushChannelType.restore("fcm", true, true))
        assertEquals(PushChannelType.PRIVATE, PushChannelType.restore("private", false, true))
    }
}
