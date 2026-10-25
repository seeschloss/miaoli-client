package im.miaoli.ui

import im.miaoli.data.FeedFormat
import im.miaoli.data.FeedRepository
import im.miaoli.data.Tribune
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

    private val viewModel = FeedViewModel(
        repository = FeedRepository(),
        tribunes = tribunes,
        postableTribunes = emptyList(),
    )

    @Test
    fun startsWithEveryTribuneEnabled() {
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
}
