package io.ethan.pushgo.data

import io.ethan.pushgo.util.UrlValidators

internal object GatewayAddressResolver {
    fun resolve(savedAddress: String?, defaultAddress: String): String {
        val address = savedAddress?.trim()?.takeIf { it.isNotEmpty() } ?: defaultAddress
        return UrlValidators.normalizeGatewayBaseUrl(address)
            ?: throw ChannelSubscriptionException.local(
                message = "Invalid gateway address",
                code = "invalid_server_address",
                category = GatewayErrorCategory.VALIDATION,
            )
    }
}
