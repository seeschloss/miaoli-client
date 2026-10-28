package im.miaoli.ui

import im.miaoli.data.FeedDataSource
import im.miaoli.data.FeedFormat
import im.miaoli.data.FetchResult
import im.miaoli.data.Post
import im.miaoli.data.PostReceipt
import im.miaoli.data.TotozUsageStore
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig
import im.miaoli.data.TribuneConfigStore
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private val tribunes = listOf(
        Tribune(
            id = 1L,
            title = "A",
            color = 0xFF000000,
            backendUrl = "https://example.test/a",
            format = FeedFormat.TSV,
            postUrl = "https://example.test/a/post",
        ),
        Tribune(
            id = 2L,
            title = "B",
            color = 0xFFFFFFFF,
            backendUrl = "https://example.test/b",
            format = FeedFormat.TSV,
            postUrl = "https://example.test/b/post",
        ),
    )

    private val store = FakeConfigStore(
        TribuneConfig(tribunes = tribunes, pollingSeconds = 30),
    )

    private val repository = FakeFeedDataSource()

    private val viewModel = FeedViewModel(
        configStore = store,
        repository = repository,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsWithEveryFeedTribuneEnabled() {
        assertEquals(setOf(1L, 2L), enabledFeedIds(viewModel))
    }

    @Test
    fun togglesTribuneVisibility() {
        viewModel.toggleTribune(1L)

        assertEquals(setOf(2L), enabledFeedIds(viewModel))
        assertFalse(store.config.tribunes.first { it.id == 1L }.enabled)

        viewModel.toggleTribune(1L)

        assertEquals(setOf(1L, 2L), enabledFeedIds(viewModel))
        assertTrue(store.config.tribunes.first { it.id == 1L }.enabled)
    }

    @Test
    fun storesLoginCookieForPosting() {
        viewModel.updateCookie(1L, "  session=abc  ")

        val cookie = viewModel.state.value.config.tribunes
            .first { it.id == 1L }
            .cookie
        assertEquals("session=abc", cookie)
        assertEquals("session=abc", store.config.tribunes.first { it.id == 1L }.cookie)
    }

    @Test
    fun recordsPostIdReturnedByTribune() {
        val localRepository = FakeFeedDataSource(postReceipt = PostReceipt("42"))
        val localViewModel = FeedViewModel(configStore = store, repository = localRepository)

        localViewModel.updateDraft(TextFieldValue("hello"))
        localViewModel.post()

        assertEquals(
            listOf("42"),
            localViewModel.state.value.config.tribunes
                .first { it.id == 1L }
                .ownPostIds,
        )
        assertEquals(
            listOf("42"),
            store.config.tribunes.first { it.id == 1L }.ownPostIds,
        )
    }

    @Test
    fun recordsTotozUsageFromSuccessfulPost() {
        val usageStore = FakeTotozUsageStore()
        val localViewModel = FeedViewModel(
            configStore = store,
            repository = FakeFeedDataSource(),
            totozUsageStore = usageStore,
        )

        localViewModel.updateDraft(
            TextFieldValue("hello [:foo] and [:foo] [:bar]"),
        )
        localViewModel.post()

        val expected = mapOf("foo" to 2, "bar" to 1)
        assertEquals(expected, localViewModel.state.value.totozUsage)
        assertEquals(expected, usageStore.usage)
    }

    @Test
    fun enablingTribuneReloadsOnlyThatTribune() {
        viewModel.toggleTribune(1L)

        assertTrue(repository.fetchedTribuneIds.isEmpty())

        viewModel.toggleTribune(1L)

        assertEquals(listOf(listOf(1L)), repository.fetchedTribuneIds)
    }

    @Test
    fun enablingTribuneMergesItsFetchedPosts() {
        val post = Post(
            id = "new",
            time = "20260101120000",
            author = "author",
            message = "message",
            tribune = tribunes.first(),
        )
        val localRepository = FakeFeedDataSource(posts = listOf(post))
        val localViewModel = FeedViewModel(
            configStore = store,
            repository = localRepository,
        )

        localViewModel.toggleTribune(1L)
        localViewModel.toggleTribune(1L)

        assertEquals(listOf("new"), localViewModel.state.value.posts.map { it.id })
        assertEquals(listOf(listOf(1L)), localRepository.fetchedTribuneIds)
    }

    @Test
    fun persistsDisabledTribuneAcrossRuns() {
        viewModel.toggleTribune(1L)

        val reloaded = FeedViewModel(configStore = store, repository = FakeFeedDataSource())

        assertEquals(setOf(2L), enabledFeedIds(reloaded))
    }

    @Test
    fun ignoresUnknownTribune() {
        viewModel.toggleTribune(99L)

        assertEquals(setOf(1L, 2L), enabledFeedIds(viewModel))
    }

    @Test
    fun insertClockSelectsPostableOrigin() {
        viewModel.insertClock("12:34:56", 2L)

        val state = viewModel.state.value
        assertEquals(2L, state.selectedTribuneId)
        assertEquals("12:34:56 ", state.draft.text)
    }

    @Test
    fun insertClockFromReadOnlyOriginKeepsDestination() {
        val readOnly = Tribune(
            id = 9L,
            title = "Readonly",
            color = 0xFF888888,
            backendUrl = "https://example.test/readonly",
            format = FeedFormat.TSV,
        )
        val localViewModel = FeedViewModel(
            configStore = FakeConfigStore(
                TribuneConfig(tribunes = tribunes + readOnly, pollingSeconds = 30),
            ),
            repository = FakeFeedDataSource(),
        )

        localViewModel.insertClock("12:34:56", 9L)

        val state = localViewModel.state.value
        assertEquals(1L, state.selectedTribuneId)
        assertEquals("12:34:56@Readonly ", state.draft.text)
    }

    @Test
    fun selectReadOnlyTribuneDoesNotChangeDestination() {
        val readOnly = Tribune(
            id = 9L,
            title = "Readonly",
            color = 0xFF888888,
            backendUrl = "https://example.test/readonly",
            format = FeedFormat.TSV,
        )
        val localViewModel = FeedViewModel(
            configStore = FakeConfigStore(
                TribuneConfig(tribunes = tribunes + readOnly, pollingSeconds = 30),
            ),
            repository = FakeFeedDataSource(),
        )

        localViewModel.selectTribune(9L)

        assertEquals(1L, localViewModel.state.value.selectedTribuneId)
    }

    @Test
    fun switchingDestinationRewritesExistingClockSuffix() {
        viewModel.insertClock("12:34:56", 1L)

        viewModel.selectTribune(2L)

        val state = viewModel.state.value
        assertEquals(2L, state.selectedTribuneId)
        assertEquals("12:34:56@A ", state.draft.text)
    }

    @Test
    fun saveConfigRewritesClocksWhenSelectedDestinationChanges() {
        viewModel.insertClock("12:34:56", 1L)

        viewModel.saveConfig(
            TribuneConfig(
                tribunes = tribunes.map { tribune ->
                    if (tribune.id == 1L) tribune.copy(postUrl = null) else tribune
                },
                pollingSeconds = 30,
            ),
        )

        val state = viewModel.state.value
        assertEquals(2L, state.selectedTribuneId)
        assertEquals("12:34:56@A ", state.draft.text)
    }

    @Test
    fun saveConfigAddsEnabledTribuneToFeed() {
        val third = Tribune(
            id = 3L,
            title = "C",
            color = 0xFF123456,
            backendUrl = "https://example.test/c",
            format = FeedFormat.TSV,
        )

        viewModel.saveConfig(
            TribuneConfig(tribunes = tribunes + third, pollingSeconds = 30),
        )

        assertEquals(setOf(1L, 2L, 3L), enabledFeedIds(viewModel))
        assertEquals(3L, viewModel.state.value.config.tribunes.last().id)
        assertTrue(viewModel.state.value.config.tribunes.last().enabled)
    }

    @Test
    fun saveConfigPreservesDisabledTribuneAndUpdatesMetadata() {
        viewModel.toggleTribune(1L)

        viewModel.saveConfig(
            TribuneConfig(
                tribunes = viewModel.state.value.config.tribunes.map { tribune ->
                    if (tribune.id == 1L) {
                        tribune.copy(title = "Renamed", color = 0xFF123456)
                    } else {
                        tribune
                    }
                },
                pollingSeconds = 45,
            ),
        )

        val state = viewModel.state.value
        assertEquals(setOf(2L), enabledFeedIds(viewModel))
        assertEquals(45, state.config.pollingSeconds)
        assertEquals("Renamed", state.config.tribunes.first { it.id == 1L }.title)
        assertEquals(0xFF123456, state.config.tribunes.first { it.id == 1L }.color)
        assertFalse(state.config.tribunes.first { it.id == 1L }.enabled)
        assertEquals(state.config, store.config)
    }

    @Test
    fun saveConfigClampsPollingInterval() {
        viewModel.saveConfig(
            TribuneConfig(tribunes = tribunes, pollingSeconds = 1),
        )

        assertEquals(
            TribuneConfig.MIN_POLLING_SECONDS,
            viewModel.state.value.config.pollingSeconds,
        )
    }

    private fun enabledFeedIds(viewModel: FeedViewModel): Set<Long> =
        viewModel.state.value.feedTribunes.mapTo(mutableSetOf()) { it.id }

    private class FakeFeedDataSource(
        private val posts: List<Post> = emptyList(),
        private val postReceipt: PostReceipt = PostReceipt(),
    ) : FeedDataSource {
        val fetchedTribuneIds = mutableListOf<List<Long>>()

        override suspend fun fetch(tribunes: List<Tribune>): FetchResult {
            val ids = tribunes.mapTo(mutableSetOf()) { it.id }
            fetchedTribuneIds += tribunes.map { it.id }
            return FetchResult(
                posts = posts.filter { it.tribune.id in ids },
                errors = emptyMap(),
            )
        }

        override suspend fun post(tribune: Tribune, message: String): PostReceipt =
            postReceipt
    }

    private class FakeConfigStore(var config: TribuneConfig) : TribuneConfigStore {
        override fun load(): TribuneConfig = config

        override fun save(config: TribuneConfig) {
            this.config = config
        }
    }

    private class FakeTotozUsageStore(
        var usage: Map<String, Int> = emptyMap(),
    ) : TotozUsageStore {
        override fun load(): Map<String, Int> = usage

        override fun save(usage: Map<String, Int>) {
            this.usage = usage
        }
    }
}
