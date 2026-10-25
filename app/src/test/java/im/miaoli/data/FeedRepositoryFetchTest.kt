package im.miaoli.data

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

class FeedRepositoryFetchTest {
    private lateinit var server: HttpServer
    private lateinit var repository: FeedRepository

    @Volatile
    private var lastUserAgent = ""

    @Volatile
    private var lastCookie = ""

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/feed") { exchange ->
            lastUserAgent = exchange.requestHeaders.getFirst("User-Agent").orEmpty()
            lastCookie = exchange.requestHeaders.getFirst("Cookie").orEmpty()

            val body = "1\t20260101120000\tAlice\talice\thello\n".toByteArray()
            exchange.responseHeaders.add("Content-Type", "text/tab-separated-values")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
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
    fun fetchesWithConfiguredUserAgentAndCookie() = runBlocking {
        val result = repository.fetch(
            listOf(
                tribune().copy(
                    userAgent = "Custom/1.0",
                    cookie = "session=abc",
                ),
            ),
        )

        assertEquals(1, result.posts.size)
        assertEquals("hello", result.posts.single().message)
        assertEquals("Custom/1.0", lastUserAgent)
        assertEquals("session=abc", lastCookie)
    }

    @Test
    fun omitsCookieHeaderWhenCookieIsEmpty() = runBlocking {
        repository.fetch(listOf(tribune()))

        assertEquals(Tribune.DEFAULT_USER_AGENT, lastUserAgent)
        assertEquals("", lastCookie)
    }

    private fun tribune() = Tribune(
        id = "test",
        title = "Test",
        color = 0,
        backendUrl = "http://127.0.0.1:${server.address.port}/feed",
        format = FeedFormat.TSV,
    )
}
