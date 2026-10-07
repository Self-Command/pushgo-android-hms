package io.ethan.pushgo.web
import org.junit.Assert.*
import org.junit.Test
class WebActionTest {
    @Test fun ordinaryMessagesRemainUnchanged() { assertNull(WebAction.from(emptyMap())); assertNull(NativeTaskCard.from(emptyMap())) }
    @Test fun explicitActionRequiresHttpsAndSafeAuthority() {
        val m = mapOf("action_version" to "1", "action_kind" to "web", "action_label" to "去打卡")
        for (url in listOf("http://example.org", "file:///private/photo", "javascript:alert(1)", "https://user:pass@example.org", "https://example.org\n", "https://example.org:0")) assertNull(WebAction.from(m + ("action_url" to url)))
        assertNotNull(WebAction.from(m + ("action_url" to "https://task.example.org/checkin/one/start#token=secret")))
    }
    @Test fun crossOriginNeverCarriesAuthorization() {
        val origin = WebOrigin.parse("https://task.example.org/checkin/card/start#token=secret")!!
        assertTrue(origin.permits("https://task.example.org/checkin-api/v1/photos"))
        assertFalse(origin.permits("https://task.example.org:8443/checkin"))
        assertFalse(origin.permits("https://evil.example/checkin"))
        assertEquals("https://evil.example/path", WebOrigin.external("https://evil.example/path?token=secret#credential=secret"))
    }
    @Test fun completeStructuredCardRemainsReadable() {
        val raw = """{"title":"阅读第五章","content":"整理三个要点；开始和结束拍照。","start":"2026-10-07T01:00:00Z","due":"2026-10-07T02:00:00Z","status":"进行中","priority":"高","tags":["学习","打卡"],"source":"Obsidian 任务","timezone":"Asia/Shanghai"}"""
        val c = NativeTaskCard.from(mapOf("task_card_version" to "1", "task_card" to raw))!!
        assertEquals("阅读第五章", c.title); assertTrue(c.content.contains("三个要点")); assertEquals(listOf("学习","打卡"),c.tags);assertEquals("高",c.priority)
    }
}
