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
    /** Counts of totoz used in posts sent from this app. */
    val totozUsage: Map<String, Int> = emptyMap(),
    /** One-shot request to bring a newly sent post, or the bottom of the feed, into view. */
    val scrollRequest: FeedScrollRequest? = null,
    /** Incremented when an external action should focus the composer. */
    val composerFocusRequest: Long = 0L,
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
    /** Whether to scroll even while the keyboard is open. */
    val revealWhileImeVisible: Boolean = false,
    /** Whether to align the target at the top instead of its default edge. */
    val alignToTop: Boolean = false,
)

data class PostError(
    val tribuneTitle: String,
    val detail: String?,
)
