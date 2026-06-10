package com.itsjeel01.remotevcsmanager.ui

/**
 * In-memory LRU cache for VCS API responses and derived data.
 *
 * Keyed by content hash (for markdown → HTML) or by API endpoint
 * signature (for API responses). Evicts least-recently-used entries
 * when capacity is exceeded.
 */
object VcsCache {

    private const val MARKDOWN_CAPACITY = 200
    private const val API_CAPACITY = 100

    private val markdownCache = object : LinkedHashMap<String, String>(MARKDOWN_CAPACITY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
            size > MARKDOWN_CAPACITY
    }

    private val apiCache = object : LinkedHashMap<String, Any>(API_CAPACITY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any>): Boolean =
            size > API_CAPACITY
    }

    // ── Markdown render cache ──────────────────────────────────

    fun getMarkdown(key: String): String? = synchronized(markdownCache) { markdownCache[key] }

    fun putMarkdown(key: String, html: String) {
        synchronized(markdownCache) { markdownCache[key] = html }
    }

    // ── Generic API response cache ─────────────────────────────

    fun <T> getApi(key: String): T? = synchronized(apiCache) {
        @Suppress("UNCHECKED_CAST")
        apiCache[key] as? T
    }

    fun putApi(key: String, value: Any) {
        synchronized(apiCache) { apiCache[key] = value }
    }

    fun invalidateApi(key: String) {
        synchronized(apiCache) { apiCache.remove(key) }
    }

    // ── Clear all caches ───────────────────────────────────────

    fun clear() {
        synchronized(markdownCache) { markdownCache.clear() }
        synchronized(apiCache) { apiCache.clear() }
    }
}
