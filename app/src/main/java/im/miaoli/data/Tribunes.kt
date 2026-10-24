package im.miaoli.data

object Tribunes {
    val moules = Tribune(
        id = "moules",
        title = "Moules",
        color = 0xFFFDE096,
        backendUrl = "https://moules.org/board/last.php?backend=tsv",
        format = FeedFormat.TSV,
    )

    val linuxfr = Tribune(
        id = "linuxfr",
        title = "LinuxFR",
        color = 0xFF67C6F2,
        backendUrl = "https://linuxfr.org/board/index.xml",
        format = FeedFormat.XML_HTML_ESCAPED,
    )

    val all = listOf(moules, linuxfr)
}
