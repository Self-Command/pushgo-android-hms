package io.ethan.pushgo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GatewayHttpCompatibilityTest {
    @Test fun savedLanOriginIsNotReplacedByTheOfficialGateway() {
        assertEquals("http://192.0.2.1:6666", UrlValidators.normalizeGatewayBaseUrl("http://192.0.2.1:6666/"))
    }
    @Test fun httpsReverseProxyKeepsItsPathAndDefaultPort() {
        assertEquals("https://example.com/gateway", UrlValidators.normalizeGatewayBaseUrl("https://example.com:443/gateway/"))
    }
    @Test fun credentialsAndQueryCannotBecomeGatewayIdentity() {
        assertNull(UrlValidators.normalizeGatewayBaseUrl("http://user:password@example.com"))
        assertNull(UrlValidators.normalizeGatewayBaseUrl("https://example.com?token=unexpected"))
    }
}
