package io.ethan.pushgo.web

import io.ethan.pushgo.util.JsonCompat
import java.net.URI

/** Opt-in message action: ordinary messages, events and things retain their original routes. */
data class WebAction(val url: String, val label: String) {
    companion object {
        val reservedKeys = setOf("action_version", "action_kind", "action_url", "action_label", "task_card_version", "task_card")
        fun from(metadata: Map<String, String>): WebAction? {
            if (metadata["action_version"] != "1" || metadata["action_kind"] != "web") return null
            val url = metadata["action_url"] ?: return null
            if (WebOrigin.parse(url) == null) return null
            val label = metadata["action_label"]?.trim()?.takeIf { it.isNotBlank() }?.take(32) ?: "打开页面"
            return WebAction(url, label)
        }
    }
}

data class WebOrigin(val host: String, val port: Int) {
    fun permits(url: String): Boolean = parse(url) == this
    companion object {
        fun parse(url: String): WebOrigin? = runCatching {
            if (url.length > 4096 || url.any { it.isISOControl() } || '\\' in url) return null
            val uri = URI(url)
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() || uri.rawUserInfo != null || uri.port < -1 || uri.port == 0 || uri.port > 65535) return null
            WebOrigin(uri.host.lowercase(), if (uri.port == -1) 443 else uri.port)
        }.getOrNull()
        /** External navigation never carries a check-in fragment or query credential. */
        fun external(url: String): String? = runCatching {
            if (parse(url) == null) return null
            val uri = URI(url)
            URI("https", null, uri.host, uri.port, uri.path, null, null).toASCIIString()
        }.getOrNull()
    }
}

data class NativeTaskCard(val title: String, val content: String, val start: String?, val due: String?, val priority: String, val status: String, val tags: List<String>, val source: String, val timezone: String) {
    companion object {
        fun from(metadata: Map<String, String>): NativeTaskCard? = runCatching {
            if (metadata["task_card_version"] != "1") return null
            val raw = metadata["task_card"] ?: return null
            if (raw.length > 1_048_576) return null
            val card = JsonCompat.parseObject(raw) ?: return null
            fun text(key: String, fallback: String = "") = (card[key] as? String)?.take(262144) ?: fallback
            val title = text("title").trim().takeIf { it.isNotEmpty() } ?: return null
            val tags = (card["tags"] as? List<*>)?.mapNotNull { (it as? String)?.take(256) }?.take(100).orEmpty()
            NativeTaskCard(title, text("content"), card["start"] as? String, card["due"] as? String, text("priority", "未设置"), text("status", "未设置"), tags, text("source", "任务中心"), text("timezone", "Asia/Shanghai"))
        }.getOrNull()
    }
}
