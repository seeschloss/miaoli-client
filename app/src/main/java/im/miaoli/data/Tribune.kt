package im.miaoli.data

enum class FeedFormat {
    TSV,
    XML_HTML_ESCAPED,
    XML_RAW,
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
    /** Optional page where the user can log in; cookies are captured from an in-app browser. */
    val loginUrl: String? = null,
    /** Name of the form field that carries the message body. */
    val postField: String = "message",
    /** User-Agent sent for this tribune's feed and posting requests. */
    val userAgent: String = DEFAULT_USER_AGENT,
    /** Optional Cookie header value sent only to this tribune's post URL. */
    val cookie: String = "",
    /** Whether this tribune is enabled in the merged feed. */
    val enabled: Boolean = true,
    /** IDs returned by this tribune for posts sent by this device, oldest first. */
    val ownPostIds: List<String> = emptyList(),
    /** Optional author name used when X-Post-Id is unavailable. */
    val username: String = "",
    /** Whether background checks should notify about answers to this device's posts. */
    val notifyOnAnswers: Boolean = false,
) {
    val canPost: Boolean
        get() = !postUrl.isNullOrBlank()

    companion object {
        const val DEFAULT_USER_AGENT = "Miaoli/0.3 (Android)"
        const val DEFAULT_TIME_ZONE = "UTC"
        const val DEFAULT_NEW_COLOR = 0xFFB8E0C2
        const val MAX_OWN_POST_IDS = 500
    }
}
