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
    /** Form endpoint accepting an application/x-www-form-urlencoded POST, if posting is supported. */
    val postUrl: String? = null,
    /** Name of the form field that carries the message body. */
    val postField: String = "message",
) {
    val canPost: Boolean
        get() = postUrl != null
}
