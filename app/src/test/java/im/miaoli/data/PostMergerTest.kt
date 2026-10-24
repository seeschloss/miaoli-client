package im.miaoli.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PostMergerTest {
    private fun post(
        id: String,
        time: String,
        tribune: Tribune = Tribunes.moules,
    ) = Post(
        id = id,
        time = time,
        author = "author",
        message = "message",
        tribune = tribune,
    )

    @Test
    fun removesDuplicatesByTribuneAndId() {
        val existing = listOf(post("1", "20260101120000"))
        val incoming = listOf(
            post("1", "20260101120000"),
            post("2", "20260101120001"),
        )

        val merged = PostMerger.merge(existing, incoming)

        assertEquals(2, merged.size)
        assertEquals(listOf("2", "1"), merged.map { it.id })
    }

    @Test
    fun sameIdInDifferentTribunesIsNotADuplicate() {
        val merged = PostMerger.merge(
            existing = listOf(post("1", "20260101120000", Tribunes.moules)),
            incoming = listOf(post("1", "20260101120000", Tribunes.linuxfr)),
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun sortsNewestFirst() {
        val merged = PostMerger.merge(
            existing = listOf(post("1", "20260101120000")),
            incoming = listOf(
                post("2", "20260101120005"),
                post("3", "20260101120002"),
            ),
        )

        assertEquals(listOf("2", "3", "1"), merged.map { it.id })
    }
}
