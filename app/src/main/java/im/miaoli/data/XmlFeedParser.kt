package im.miaoli.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class XmlFeedParser(
    private val preserveMessageTags: Boolean = false,
) : FeedParser {
    override fun parse(body: String, tribune: Tribune): List<Post> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(body))

        val posts = mutableListOf<Post>()
        var inPost = false
        var id: String? = null
        var time: String? = null
        var info = ""
        var login = ""
        var message = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "post" -> {
                        inPost = true
                        id = parser.getAttributeValue(null, "id")
                        time = parser.getAttributeValue(null, "time")
                        info = ""
                        login = ""
                        message = ""
                    }

                    "info" -> if (inPost) info = parser.nextText()
                    "login" -> if (inPost) login = parser.nextText()
                    "message" -> if (inPost) {
                        message = if (preserveMessageTags) {
                            readMessageMarkup(parser)
                        } else {
                            parser.nextText()
                        }
                    }
                }

                XmlPullParser.END_TAG -> if (parser.name == "post") {
                    inPost = false
                    val postId = id
                    val postTime = time
                    if (!postId.isNullOrBlank() && !postTime.isNullOrBlank()) {
                        posts += Post(
                            id = postId,
                            time = postTime,
                            author = login.ifBlank { info }.ifBlank { "?" },
                            message = message,
                            tribune = tribune,
                        )
                    }
                }
            }
            event = parser.next()
        }

        return posts
    }

    /**
     * Rebuilds a message containing nested XML elements as HTML markup.
     * Text and attribute values are escaped again because the pull parser
     * has already decoded the XML entities.
     */
    private fun readMessageMarkup(parser: XmlPullParser): String {
        val markup = StringBuilder()
        var depth = 0
        var event = parser.next()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    markup.append('<').append(parser.name)
                    for (index in 0 until parser.attributeCount) {
                        markup.append(' ')
                            .append(parser.getAttributeName(index))
                            .append("=\"")
                            .append(escapeAttribute(parser.getAttributeValue(index)))
                            .append('"')
                    }
                    markup.append('>')
                    depth++
                }

                XmlPullParser.END_TAG -> {
                    if (depth == 0) {
                        return markup.toString()
                    }
                    markup.append("</").append(parser.name).append('>')
                    depth--
                }

                XmlPullParser.TEXT,
                XmlPullParser.CDSECT,
                -> markup.append(escapeText(parser.text.orEmpty()))

                XmlPullParser.ENTITY_REF -> {
                    val text = parser.text
                    if (text != null) {
                        markup.append(escapeText(text))
                    } else {
                        markup.append('&').append(parser.name).append(';')
                    }
                }
            }
            event = parser.next()
        }

        return markup.toString()
    }

    private fun escapeText(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                else -> append(character)
            }
        }
    }

    private fun escapeAttribute(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                else -> append(character)
            }
        }
    }
}
