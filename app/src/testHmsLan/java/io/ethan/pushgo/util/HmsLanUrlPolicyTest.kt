package io.ethan.pushgo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HmsLanUrlPolicyTest {
    @Test fun only_configured_lan_host_accepts_http() {
        assertEquals("http://192.168.1.6:6666", UrlValidators.normalizeGatewayBaseUrl("http://192.168.1.6:6666/"))
        assertNull(UrlValidators.normalizeGatewayBaseUrl("http://192.168.1.7:6666"))
        assertNull(UrlValidators.normalizeGatewayBaseUrl("http://example.com"))
        assertNull(UrlValidators.normalizeGatewayBaseUrl("http://192.168.1.6.evil.example"))
        assertEquals("https://example.com", UrlValidators.normalizeGatewayBaseUrl("https://example.com/"))
    }
}
