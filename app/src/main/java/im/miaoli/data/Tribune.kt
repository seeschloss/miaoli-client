package im.miaoli.data

enum class FeedFormat {
    TSV,
    XML_HTML_ESCAPED,
}

data class Tribune(
    val id: Long,
    val title: String,
    /** ARGB color used to identify this tribune's messages. */
    val color: Long,
    val backendUrl: String,
    val format: FeedFormat,
    /**
     * IANA time zone used to interpret the server's yyyyMMddHHmmss timestamps,
     * for example "Europe/Paris" or "UTC".
     */
    val timeZoneId: String = DEFAULT_TIME_ZONE,
    /** Form endpoint accepting an application/x-www-form-urlencoded POST, if posting is supported. */
    val postUrl: String? = null,
    /** Name of the form field that carries the message body. */
    val postField: String = "message",
    /** User-Agent sent for this tribune's feed and posting requests. */
    val userAgent: String = DEFAULT_USER_AGENT,
    /** Optional Cookie header value sent for this tribune's requests. */
    val cookie: String = "",
    /** Whether this tribune is included in the merged feed and its side pane. */
    val showInFeed: Boolean = true,
) {
    val canPost: Boolean
        get() = !postUrl.isNullOrBlank()

    companion object {
        const val DEFAULT_USER_AGENT = "Miaoli/0.3 (Android)"
        const val DEFAULT_TIME_ZONE = "UTC"
        const val DEFAULT_NEW_COLOR = 0xFFB8E0C2
    }
}
