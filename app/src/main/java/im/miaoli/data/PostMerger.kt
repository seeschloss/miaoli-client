package im.miaoli.data

object PostMerger {
    private const val MAX_POSTS = 500

    /**
     * Merge two sets of posts, removing duplicates and returning the newest posts first.
     * Newest-first ordering matches the reverse layout used by the feed.
     */
    fun merge(existing: Collection<Post>, incoming: Collection<Post>): List<Post> {
        val byKey = LinkedHashMap<String, Post>(existing.size + incoming.size)
        existing.forEach { byKey[it.key] = it }
        incoming.forEach { byKey[it.key] = it }

        return byKey.values
            .sortedWith(
                compareByDescending<Post> { it.time }
                    .thenByDescending { it.id.toLongOrNull() ?: 0L },
            )
            .take(MAX_POSTS)
    }
}
