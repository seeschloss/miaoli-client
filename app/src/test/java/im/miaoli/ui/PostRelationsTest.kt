package im.miaoli.ui

import im.miaoli.data.FeedFormat
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostRelationsTest {
    private val tribune = Tribune(
        id = 1L,
        title = "A",
        color = 0xFFB8E0C2,
        backendUrl = "https://example.test/a",
        format = FeedFormat.TSV,
        timeZoneId = "UTC",
        ownPostIds = listOf("1"),
    )

    @Test
    fun marksOwnPostsAndRepliesToThem() = runBlocking {
        val posts = listOf(
            post(id = "3", time = "20260101120200", message = "hello"),
            post(id = "2", time = "20260101120100", message = "12:00:00"),
            post(id = "1", time = "20260101120000", message = "original"),
        )

        val relations = computePostRelations(posts, listOf(tribune))

        assertEquals(PostRelation.Own, relations["1:1"])
        assertEquals(PostRelation.ReplyToOwn, relations["1:2"])
        assertNull(relations["1:3"])
    }

    @Test
    fun matchesOwnPostsByUsernameWhenIdsAreUnavailable() = runBlocking {
        val posts = listOf(
            post(id = "1", time = "20260101120000", message = "original"),
            post(
                id = "2",
                time = "20260101120100",
                message = "12:00:00",
                author = "other",
            ),
        )
        val usernameTribune = tribune.copy(ownPostIds = emptyList(), username = "author")

        val relations = computePostRelations(posts, listOf(usernameTribune))

        assertEquals(PostRelation.Own, relations["1:1"])
        assertEquals(PostRelation.ReplyToOwn, relations["1:2"])
    }

    @Test
    fun ownPostTakesPrecedenceOverReplyMarker() = runBlocking {
        val posts = listOf(
            post(id = "1", time = "20260101120000", message = "original"),
            post(id = "2", time = "20260101120100", message = "12:00:00"),
        )
        val ownTribune = tribune.copy(ownPostIds = listOf("1", "2"))

        val relations = computePostRelations(posts, listOf(ownTribune))

        assertEquals(PostRelation.Own, relations["1:1"])
        assertEquals(PostRelation.Own, relations["1:2"])
    }

    @Test
    fun collectsAnswersWithCrossTribuneSuffix() = runBlocking {
        val otherTribune = tribune.copy(
            id = 2L,
            title = "B",
            backendUrl = "https://example.test/b",
            ownPostIds = emptyList(),
        )
        val posts = listOf(
            Post(
                id = "2",
                time = "20260101120100",
                author = "answerer",
                message = "12:00:00@A",
                tribune = otherTribune,
            ),
            post(id = "1", time = "20260101120000", message = "original"),
        )

        val data = computePostRelationsWithAnswers(
            posts = posts,
            tribunes = listOf(tribune, otherTribune),
        )

        assertEquals(listOf("12:01:00@B"), data.answers["1:1"]?.map { it.displayClock })
    }

    private fun post(
        id: String,
        time: String,
        message: String,
        author: String = "author",
    ) = Post(
        id = id,
        time = time,
        author = author,
        message = message,
        tribune = tribune,
    )
}
