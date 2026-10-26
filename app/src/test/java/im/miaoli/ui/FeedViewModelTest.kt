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
            id = "a",
            title = "A",
            color = 0xFF000000,
            backendUrl = "https://example.test/a",
            format = FeedFormat.TSV,
        ),
        Tribune(
            id = "b",
            title = "B",
            color = 0xFFFFFFFF,
            backendUrl = "https://example.test/b",
            format = FeedFormat.TSV,
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
        assertEquals(setOf("a", "b"), viewModel.state.value.enabledTribuneIds)
    }

    @Test
    fun togglesTribuneVisibility() {
        viewModel.toggleTribune("a")

        assertEquals(setOf("b"), viewModel.state.value.enabledTribuneIds)

        viewModel.toggleTribune("a")

        assertEquals(setOf("a", "b"), viewModel.state.value.enabledTribuneIds)
    }

    @Test
    fun ignoresUnknownTribune() {
        viewModel.toggleTribune("missing")

        assertEquals(setOf("a", "b"), viewModel.state.value.enabledTribuneIds)
    }

    @Test
    fun appendsClockToDraftWithSeparator() {
        viewModel.updateDraft("hello")

        viewModel.appendToDraft("12:34:56 ")

        assertEquals("hello 12:34:56 ", viewModel.state.value.draft)
    }

    @Test
    fun appendsClockWithoutExtraSeparatorAfterWhitespace() {
        viewModel.updateDraft("hello ")

        viewModel.appendToDraft("12:34:56 ")

        assertEquals("hello 12:34:56 ", viewModel.state.value.draft)
    }

    @Test
    fun appendsClockToEmptyDraft() {
        viewModel.appendToDraft("12:34:56 ")

        assertEquals("12:34:56 ", viewModel.state.value.draft)
    }

    @Test
    fun selectsReadOnlyTribuneAsComposerTarget() {
        viewModel.selectTribune("a")

        assertEquals("a", viewModel.state.value.selectedTribuneId)
    }

    @Test
    fun saveConfigAddsNewlyVisibleTribuneToEnabledSet() {
        val third = Tribune(
            id = "c",
            title = "C",
            color = 0xFF123456,
            backendUrl = "https://example.test/c",
            format = FeedFormat.TSV,
        )

        viewModel.saveConfig(
            TribuneConfig(tribunes = tribunes + third, pollingSeconds = 30),
        )

        assertEquals(setOf("a", "b", "c"), viewModel.state.value.enabledTribuneIds)
        assertEquals("c", viewModel.state.value.config.tribunes.last().id)
    }

    @Test
    fun saveConfigPreservesHiddenTribuneAndUpdatesMetadata() {
        viewModel.toggleTribune("a")

        viewModel.saveConfig(
            TribuneConfig(
                tribunes = tribunes.map { tribune ->
                    if (tribune.id == "a") {
                        tribune.copy(title = "Renamed", color = 0xFF123456)
                    } else {
                        tribune
                    }
                },
                pollingSeconds = 45,
            ),
        )

        val state = viewModel.state.value
        assertEquals(setOf("b"), state.enabledTribuneIds)
        assertEquals(45, state.config.pollingSeconds)
        assertEquals("Renamed", state.config.tribunes.first { it.id == "a" }.title)
        assertEquals(0xFF123456, state.config.tribunes.first { it.id == "a" }.color)
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
