package im.miaoli.data

object Tribunes {
    val miaoliandroid = Tribune(
        id = 1L,
        title = "Miaoli Android",
        color = 0xFFB8E0C2,
        backendUrl = "https://miaoli.im/@See/miaoli-android/posts.tsv",
        format = FeedFormat.TSV,
        timeZoneId = "UTC",
        postUrl = "https://miaoli.im/@See/miaoli-android",
        loginUrl = "https://miaoli.im",
        postField = "message",
    )

    val linuxfr = Tribune(
        id = 2L,
        title = "LinuxFR",
        color = 0xFF67C6F2,
        backendUrl = "https://linuxfr.org/board/index.xml",
        format = FeedFormat.XML_HTML_ESCAPED,
        timeZoneId = "Europe/Paris",
        loginUrl = "https://linuxfr.org",
    )

    val defaults = listOf(miaoliandroid, linuxfr)
}
