package io.ethan.pushgo.data

/** App identity and wire routing are separate from the token value. */
enum class PushChannelType(val wireName: String) {
    FCM("fcm"), HMS("huawei"), PRIVATE("private");

    companion object {
        fun restore(value: String?, legacyUseFcm: Boolean, hasLegacyHmsToken: Boolean): PushChannelType =
            entries.firstOrNull { it.wireName == value }
                ?: if (hasLegacyHmsToken) HMS else if (legacyUseFcm) FCM else PRIVATE
    }
}
