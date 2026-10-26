package im.miaoli.ui

import im.miaoli.data.Post
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val config: TribuneConfig = TribuneConfig(),
    val enabledTribuneIds: Set<String> = config.feedTribunes.map { it.id }.toSet(),
    val draft: DraftState = DraftState(),
    val selectedTribuneId: String = "",
    val isPosting: Boolean = false,
    val postError: String? = null,
) {
    val feedTribunes: List<Tribune>
        get() = config.feedTribunes

    val postableTribunes: List<Tribune>
        get() = config.postableTribunes
}
