package im.miaoli.data

object Tribunes {
    val moules = Tribune(
        id = 1L,
        title = "Moules",
        color = 0xFFFDE096,
        backendUrl = "https://moules.org/board/last.php?backend=tsv",
        format = FeedFormat.TSV,
        timeZoneId = "Europe/Paris",
    )

    val linuxfr = Tribune(
        id = 2L,
        title = "LinuxFR",
        color = 0xFF67C6F2,
        backendUrl = "https://linuxfr.org/board/index.xml",
        format = FeedFormat.XML_HTML_ESCAPED,
        timeZoneId = "Europe/Paris",
    )

    val miaoliandroid = Tribune(
        id = 3L,
        title = "Miaoli Android",
        color = 0xFFB8E0C2,
        backendUrl = "https://miaoli.im/@See/miaoli-android/posts.tsv",
        format = FeedFormat.TSV,
        timeZoneId = "UTC",
        postUrl = "https://miaoli.im/@See/miaoli-android",
        postField = "message",
        showInFeed = false,
    )

    val defaults = listOf(moules, linuxfr, miaoliandroid)
}
