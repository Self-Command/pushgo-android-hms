package io.ethan.pushgo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlValidatorsTest {
    @Test
    fun savedLanGateway_remainsUsableAfterChangingBuildVariant() {
        assertEquals(
            "http://192.168.1.6:6666",
            UrlValidators.normalizeGatewayBaseUrl("http://192.168.1.6:6666"),
        )
        assertEquals(
            "http://10.20.30.40:6666/pushgo",
            UrlValidators.normalizeGatewayBaseUrl(" http://10.20.30.40:6666/pushgo/ "),
        )
    }

    @Test
    fun selfHostedGateways_preserveHostPortAndPrefix() {
        assertEquals("http://gateway.local:6666", UrlValidators.normalizeGatewayBaseUrl("http://gateway.local:6666/"))
        assertEquals("https://gateway.example/api", UrlValidators.normalizeGatewayBaseUrl("HTTPS://Gateway.Example:443/api/"))
        assertEquals("http://[fd00::1]:6666", UrlValidators.normalizeGatewayBaseUrl("http://[fd00::1]:6666"))
    }

    @Test
    fun invalidGatewayUrls_areRejected() {
        for (raw in listOf(
            "", "192.168.1.6:6666", "ftp://gateway.local", "http://user:secret@gateway.local",
            "http://gateway.local?token=value", "http://gateway.local#fragment",
            "http://gateway.local:0", "http://gateway.local:65536", "http://gateway.local:-1",
        )) {
            assertNull(raw, UrlValidators.normalizeGatewayBaseUrl(raw))
        }
    }

    @Test
    fun tasknotesAndUpdateHttpsValidation_remainsSeparate() {
        assertNull(UrlValidators.normalizeHttpsUrl("http://gateway.local/checkin"))
        assertEquals("https://tasks.example", UrlValidators.normalizeHttpsUrl(" https://tasks.example "))
    }
}
