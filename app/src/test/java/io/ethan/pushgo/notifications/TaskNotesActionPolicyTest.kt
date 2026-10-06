package io.ethan.pushgo.notifications

import org.junit.Assert.*
import org.junit.Test

class TaskNotesActionPolicyTest {
    private val path = "/tasks/123e4567-e89b-12d3-a456-426614174000?checkpoint=start"
    private val metadata = mapOf("tasknotes_action" to "checkin", "tasknotes_checkpoint" to "start")
    @Test fun acceptsOnlyConfiguredOriginAndTask() {
        assertEquals("https://tasks.example.com$path", TaskNotesActionPolicy.checkinUrl("https://TASKS.example.com:443/", "https://tasks.example.com$path", metadata))
        for (url in listOf("https://evil.example$path", "http://tasks.example.com$path", "https://u:p@tasks.example.com$path", "https://tasks.example.com$path#x", "https://tasks.example.com/tasks/x?checkpoint=start", "https://tasks.example.com$path&redirect=https://evil.example"))
            assertNull(TaskNotesActionPolicy.checkinUrl("https://tasks.example.com", url, metadata))
    }
    @Test fun rejectsUnconfiguredOrUnmarkedMessages() {
        assertNull(TaskNotesActionPolicy.checkinUrl(null, "https://tasks.example.com$path", metadata))
        assertNull(TaskNotesActionPolicy.checkinUrl("https://tasks.example.com", "https://tasks.example.com$path", emptyMap()))
        assertNull(TaskNotesActionPolicy.checkinUrl("https://tasks.example.com", "https://tasks.example.com$path", metadata + ("tasknotes_checkpoint" to "end")))
    }
    @Test fun validatesOriginConfiguration() {
        assertNull(TaskNotesActionPolicy.normalizeOrigin("https://tasks.example.com/tasks"))
        assertNull(TaskNotesActionPolicy.normalizeOrigin("javascript:alert(1)"))
        assertEquals("https://tasks.example.com:8443", TaskNotesActionPolicy.normalizeOrigin("https://tasks.example.com:8443/"))
    }
}
