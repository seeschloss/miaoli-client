package im.miaoli.data

enum class FeedFormat {
    TSV,
    XML_HTML_ESCAPED,
}

data class Tribune(
    val id: String,
    val title: String,
    /** ARGB color used to identify this tribune's messages. */
    val color: Long,
    val backendUrl: String,
    val format: FeedFormat,
)
