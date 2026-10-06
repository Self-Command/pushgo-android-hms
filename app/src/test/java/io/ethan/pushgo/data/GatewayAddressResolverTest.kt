package io.ethan.pushgo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class GatewayAddressResolverTest {
    private val defaultAddress = "https://gateway.pushgo.cn"

    @Test
    fun upgrade_preservesSavedLanGatewayInsteadOfRegisteringWithOfficialGateway() {
        assertEquals("http://192.168.1.6:6666", GatewayAddressResolver.resolve("http://192.168.1.6:6666/", defaultAddress))
    }

    @Test
    fun explicitInvalidAddress_doesNotUseDefaultGateway() {
        try {
            GatewayAddressResolver.resolve("192.168.1.6:6666", defaultAddress)
            fail("An explicit invalid address must stop the request")
        } catch (error: ChannelSubscriptionException) {
            assertEquals("invalid_server_address", error.code)
            assertNull(error.httpStatus)
        }
    }

    @Test
    fun onlyAbsentSavedAddress_usesDefaultGateway() {
        for (savedAddress in listOf(null, "", "  ")) {
            assertEquals(defaultAddress, GatewayAddressResolver.resolve(savedAddress, defaultAddress))
        }
    }
}
