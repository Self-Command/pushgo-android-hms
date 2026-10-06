package io.ethan.pushgo.notifications

import java.net.URI

/** Only the configured origin and a UUID task page may receive a check-in action. */
object TaskNotesActionPolicy {
    fun normalizeOrigin(value: String): String? = runCatching {
        val uri = URI(value.trim())
        require(uri.scheme.equals("https", ignoreCase = true))
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null)
        require(uri.rawPath.isNullOrEmpty() || uri.rawPath == "/")
        require(uri.port == -1 || uri.port in 1..65535)
        URI("https", null, uri.host.lowercase(), if (uri.port == 443) -1 else uri.port, null, null, null).toASCIIString()
    }.getOrNull()

    fun checkinUrl(configuredOrigin: String?, url: String?, metadata: Map<String, String>): String? = runCatching {
        require(metadata["tasknotes_action"] == "checkin")
        val kind = metadata["tasknotes_checkpoint"]
        require(kind == "start" || kind == "end")
        val allowed = normalizeOrigin(configuredOrigin.orEmpty()) ?: return null
        val uri = URI(url ?: return null)
        require(uri.rawUserInfo == null && uri.rawFragment == null)
        val candidateOrigin = URI(uri.scheme, null, uri.host, uri.port, null, null, null).toString()
        require(normalizeOrigin(candidateOrigin) == allowed)
        require(Regex("^/tasks/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matches(uri.rawPath.orEmpty()))
        require(uri.rawQuery == "checkpoint=$kind")
        uri.toASCIIString()
    }.getOrNull()
}
