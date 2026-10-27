package im.miaoli.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class FetchResult(
    val posts: List<Post>,
    /** Errors keyed by the tribune that failed. */
    val errors: Map<Tribune, String>,
)

data class PostReceipt(
    /** ID returned by the tribune in the `X-Post-Id` response header, if any. */
    val postId: String? = null,
)

interface FeedDataSource {
    suspend fun fetch(tribunes: List<Tribune>): FetchResult
    suspend fun post(tribune: Tribune, message: String): PostReceipt
}

class FeedRepository(
    private val client: OkHttpClient = defaultClient(),
    private val parsers: Map<FeedFormat, FeedParser> = mapOf(
        FeedFormat.TSV to TsvFeedParser(),
        FeedFormat.XML_HTML_ESCAPED to XmlFeedParser(),
    ),
) : FeedDataSource {
    override suspend fun fetch(tribunes: List<Tribune>): FetchResult = coroutineScope {
        val results = tribunes
            .map { tribune ->
                async(Dispatchers.IO) {
                    tribune to runCatching { fetchOne(tribune) }
                }
            }
            .awaitAll()

        FetchResult(
            posts = results.flatMap { (_, result) -> result.getOrDefault(emptyList()) },
            errors = results
                .mapNotNull { (tribune, result) ->
                    result.exceptionOrNull()?.let { error ->
                        tribune to (error.message ?: error::class.simpleName ?: "Unknown error")
                    }
                }
                .toMap(),
        )
    }

    private suspend fun fetchOne(tribune: Tribune): List<Post> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(tribune.backendUrl)
            .tribuneHeaders(tribune)
            .header("Accept", "application/xml, text/xml, text/tab-separated-values, */*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }

            val body = response.body.string()
            parsers.getValue(tribune.format).parse(body, tribune)
        }
    }

    /**
     * Posts [message] to [tribune] using its configured form endpoint.
     *
     * No credentials are required. A configured cookie is sent as an optional
     * header. A non-2xx response is reported as an [IOException]. The tribune
     * may return the new post's ID in the `X-Post-Id` response header.
     */
    override suspend fun post(tribune: Tribune, message: String): PostReceipt =
        withContext(Dispatchers.IO) {
            val url = tribune.postUrl?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("${tribune.title} does not support posting")
            val body = message.trim()
            require(body.isNotEmpty()) { "Message must not be empty" }

            val form = FormBody.Builder()
                .add(tribune.postField, body)
                .build()

            val request = Request.Builder()
                .url(url)
                .tribuneHeaders(tribune)
                .header("Accept", "application/json")
                .header("Referer", tribune.backendUrl)
                .post(form)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }

                PostReceipt(
                    postId = response.header("X-Post-Id")
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() },
                )
            }
        }

    private fun Request.Builder.tribuneHeaders(tribune: Tribune): Request.Builder = apply {
        header("User-Agent", tribune.userAgent.ifBlank { Tribune.DEFAULT_USER_AGENT })
        tribune.cookie.trim()
            .takeIf { it.isNotEmpty() }
            ?.let { header("Cookie", it) }
    }

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
