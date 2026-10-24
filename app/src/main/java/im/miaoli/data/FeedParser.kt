package im.miaoli.data

interface FeedParser {
    fun parse(body: String, tribune: Tribune): List<Post>
}
