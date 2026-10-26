package im.miaoli.data

object Tribunes {
    val moules = Tribune(
        id = "moules",
        title = "Moules",
        color = 0xFFFDE096,
        backendUrl = "https://moules.org/board/last.php?backend=tsv",
        format = FeedFormat.TSV,
        timeZoneId = "Europe/Paris",
    )

    val linuxfr = Tribune(
        id = "linuxfr",
        title = "LinuxFR",
        color = 0xFF67C6F2,
        backendUrl = "https://linuxfr.org/board/index.xml",
        format = FeedFormat.XML_HTML_ESCAPED,
        timeZoneId = "Europe/Paris",
    )

    /**
     * Tribune provided on miaoli.im for testing posting.
     *
     * It is intentionally hidden from the feed by default, so the read-only
     * Moules and LinuxFR feeds stay unchanged. It can be edited in the
     * configuration screen like any other tribune.
     */
    val nativeTest = Tribune(
        id = "sikfart",
        title = "Sikfart",
        color = 0xFFB8E0C2,
        backendUrl = "https://miaoli.im/@See/+6QbxZZANv8/posts.tsv",
        format = FeedFormat.TSV,
        timeZoneId = "UTC",
        postUrl = "https://miaoli.im/@See/+6QbxZZANv8",
        postField = "message",
        showInFeed = false,
    )

    /** All built-in tribunes, including post-only tribunes. */
    val defaults = listOf(moules, linuxfr, nativeTest)

    /**
     * Time zone to use when loading a saved configuration from before
     * per-tribune time zones existed. Unknown tribunes default to UTC.
     */
    fun defaultTimeZoneFor(id: String): String =
        defaults.firstOrNull { it.id == id }?.timeZoneId ?: Tribune.DEFAULT_TIME_ZONE
}
