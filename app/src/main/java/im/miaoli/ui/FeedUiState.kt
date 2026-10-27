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
    val draft: DraftState = DraftState(),
    val selectedTribuneId: Long? = null,
    val isPosting: Boolean = false,
    val postError: PostError? = null,
    /** One-shot request to bring a newly sent post, or the bottom of the feed, into view. */
    val scrollRequest: FeedScrollRequest? = null,
) {
    val feedTribunes: List<Tribune>
        get() = config.feedTribunes

    val postableTribunes: List<Tribune>
        get() = config.postableTribunes
}

data class FeedScrollRequest(
    val id: Long,
    /** Key of the post to reveal, or null to reveal the newest end of the feed. */
    val postKey: String? = null,
)

data class PostError(
    val tribuneTitle: String,
    val detail: String?,
)
