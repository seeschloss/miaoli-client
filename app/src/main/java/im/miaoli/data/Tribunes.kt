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

    /**
     * Temporary tribune created on miaoli.im for testing posting.
     *
     * It is intentionally not part of [all], so the read-only Moules and
     * LinuxFR feeds stay unchanged. Replace it with a real tribune once
     * posting is wired into the final configuration.
     */
    val nativeTest = Tribune(
        id = "native-test",
        title = "Native Test",
        color = 0xFFB8E0C2,
        backendUrl = "https://miaoli.im/tribune/native-client-test-20261025/tsv",
        format = FeedFormat.TSV,
        postUrl = "https://miaoli.im/tribune/native-client-test-20261025/post",
        postField = "message",
    )

    val all = listOf(moules, linuxfr)

    /** Tribunes that can receive new messages, used by the composer's picker. */
    val postable = listOf(nativeTest)
}
