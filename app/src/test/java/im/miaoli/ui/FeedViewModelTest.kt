package im.miaoli.ui

import im.miaoli.data.FeedFormat
import im.miaoli.data.FeedRepository
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig
import im.miaoli.data.TribuneConfigStore
import org.junit.Assert.assertEquals
import org.junit.Test

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

    private val viewModel = FeedViewModel(
        configStore = store,
        repository = FeedRepository(),
    )

    @Test
    fun startsWithEveryFeedTribuneEnabled() {
        assertEquals(setOf(1L, 2L), viewModel.state.value.enabledTribuneIds)
    }

    @Test
    fun togglesTribuneVisibility() {
        viewModel.toggleTribune(1L)

        assertEquals(setOf(2L), viewModel.state.value.enabledTribuneIds)

        viewModel.toggleTribune(1L)

        assertEquals(setOf(1L, 2L), viewModel.state.value.enabledTribuneIds)
    }

    @Test
    fun ignoresUnknownTribune() {
        viewModel.toggleTribune(99L)

        assertEquals(setOf(1L, 2L), viewModel.state.value.enabledTribuneIds)
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
            repository = FeedRepository(),
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
            repository = FeedRepository(),
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
    fun saveConfigAddsNewlyVisibleTribuneToEnabledSet() {
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

        assertEquals(setOf(1L, 2L, 3L), viewModel.state.value.enabledTribuneIds)
        assertEquals(3L, viewModel.state.value.config.tribunes.last().id)
    }

    @Test
    fun saveConfigPreservesHiddenTribuneAndUpdatesMetadata() {
        viewModel.toggleTribune(1L)

        viewModel.saveConfig(
            TribuneConfig(
                tribunes = tribunes.map { tribune ->
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
        assertEquals(setOf(2L), state.enabledTribuneIds)
        assertEquals(45, state.config.pollingSeconds)
        assertEquals("Renamed", state.config.tribunes.first { it.id == 1L }.title)
        assertEquals(0xFF123456, state.config.tribunes.first { it.id == 1L }.color)
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

    private class FakeConfigStore(var config: TribuneConfig) : TribuneConfigStore {
        override fun load(): TribuneConfig = config

        override fun save(config: TribuneConfig) {
            this.config = config
        }
    }
}
