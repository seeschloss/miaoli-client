package im.miaoli.data

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

class FeedRepositoryPostTest {
    private lateinit var server: HttpServer
    private lateinit var repository: FeedRepository

    @Volatile
    private var responseCode = 200

    @Volatile
    private var responsePostId: String? = null

    @Volatile
    private var lastMethod = ""

    @Volatile
    private var lastPath = ""

    @Volatile
    private var lastBody = ""

    @Volatile
    private var lastContentType = ""

    @Volatile
    private var lastAccept = ""

    @Volatile
    private var lastUserAgent = ""

    @Volatile
    private var lastCookie = ""

    @Volatile
    private var lastReferer = ""

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/tribune/test/post") { exchange ->
            lastMethod = exchange.requestMethod
            lastPath = exchange.requestURI.path
            lastBody = exchange.requestBody.bufferedReader().use { it.readText() }
            lastContentType = exchange.requestHeaders.getFirst("Content-Type").orEmpty()
            lastAccept = exchange.requestHeaders.getFirst("Accept").orEmpty()
            lastUserAgent = exchange.requestHeaders.getFirst("User-Agent").orEmpty()
            lastCookie = exchange.requestHeaders.getFirst("Cookie").orEmpty()
            lastReferer = exchange.requestHeaders.getFirst("Referer").orEmpty()

            val response = """{"posts":[]}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            responsePostId?.let { postId ->
                exchange.responseHeaders.add("x-post-id", postId)
            }
            exchange.sendResponseHeaders(responseCode, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()

        repository = FeedRepository(
            client = OkHttpClient.Builder()
                .callTimeout(5, TimeUnit.SECONDS)
                .build(),
        )
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun postsUrlEncodedMessageAsForm() = runBlocking {
        repository.post(tribune(), "  Hello & goodbye  ")

        assertEquals("POST", lastMethod)
        assertEquals("/tribune/test/post", lastPath)
        assertEquals("message=Hello+%26+goodbye", lastBody)
        assertTrue(lastContentType.startsWith("application/x-www-form-urlencoded"))
        assertEquals("application/json", lastAccept)
        assertEquals("Miaoli/0.3 (Android)", lastUserAgent)
        assertEquals("", lastCookie)
        assertEquals(tribune().backendUrl, lastReferer)
    }

    @Test
    fun returnsPostIdHeader() = runBlocking {
        responsePostId = " 62963 "

        val receipt = repository.post(tribune(), "hello")

        assertEquals("62963", receipt.postId)
    }

    @Test
    fun returnsNullPostIdWhenHeaderIsMissing() = runBlocking {
        val receipt = repository.post(tribune(), "hello")

        assertNull(receipt.postId)
    }

    @Test
    fun sendsConfiguredUserAgentAndCookie() = runBlocking {
        repository.post(
            tribune().copy(
                userAgent = "Custom/1.0",
                cookie = "session=abc",
            ),
            "hello",
        )

        assertEquals("Custom/1.0", lastUserAgent)
        assertEquals("session=abc", lastCookie)
    }

    @Test
    fun usesConfiguredFormField() = runBlocking {
        repository.post(tribune().copy(postField = "board[message]"), "hello")

        assertEquals("board%5Bmessage%5D=hello", lastBody)
    }

    @Test(expected = IOException::class)
    fun throwsOnHttpError(): Unit = runBlocking {
        responseCode = 500
        repository.post(tribune(), "hello")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankMessage(): Unit = runBlocking {
        repository.post(tribune(), "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsReadOnlyTribune(): Unit = runBlocking {
        repository.post(tribune().copy(postUrl = null), "hello")
    }

    private fun tribune() = Tribune(
        id = "test",
        title = "Test",
        color = 0,
        backendUrl = "http://127.0.0.1:${server.address.port}/tribune/test/tsv",
        format = FeedFormat.TSV,
        postUrl = "http://127.0.0.1:${server.address.port}/tribune/test/post",
    )
}
