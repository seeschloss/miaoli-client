package im.miaoli.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class XmlFeedParser : FeedParser {
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
                    "message" -> if (inPost) message = parser.nextText()
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
}
