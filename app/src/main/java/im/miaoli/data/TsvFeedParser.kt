package im.miaoli.data

class TsvFeedParser : FeedParser {
    override fun parse(body: String, tribune: Tribune): List<Post> =
        body.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .mapNotNull { line -> parseLine(line, tribune) }
            .toList()

    private fun parseLine(line: String, tribune: Tribune): Post? {
        val fields = line.split('\t', limit = 5)
        if (fields.size < 5) return null

        val id = fields[0].trim()
        val time = fields[1].trim()
        if (id.isEmpty() || id.toLongOrNull() == null) return null

        val info = fields[2]
        val login = fields[3]

        return Post(
            id = id,
            time = time,
            author = login.ifBlank { info }.ifBlank { "?" },
            message = fields[4],
            tribune = tribune,
        )
    }
}
