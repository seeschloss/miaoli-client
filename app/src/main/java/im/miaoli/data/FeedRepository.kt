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

class FeedRepository(
    private val client: OkHttpClient = defaultClient(),
    private val parsers: Map<FeedFormat, FeedParser> = mapOf(
        FeedFormat.TSV to TsvFeedParser(),
        FeedFormat.XML_HTML_ESCAPED to XmlFeedParser(),
    ),
) {
    suspend fun fetch(tribunes: List<Tribune>): FetchResult = coroutineScope {
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
            .header("User-Agent", USER_AGENT)
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
     * No credentials are sent. A non-2xx response is reported as an [IOException].
     */
    suspend fun post(tribune: Tribune, message: String) = withContext(Dispatchers.IO) {
        val url = tribune.postUrl
            ?: throw IllegalArgumentException("${tribune.title} does not support posting")
        val body = message.trim()
        require(body.isNotEmpty()) { "Message must not be empty" }

        val form = FormBody.Builder()
            .add(tribune.postField, body)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Referer", tribune.backendUrl)
            .post(form)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
        }
    }

    companion object {
        private const val USER_AGENT = "Miaoli/0.3 (Android)"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
