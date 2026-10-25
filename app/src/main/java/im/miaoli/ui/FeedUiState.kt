package im.miaoli.ui

import im.miaoli.data.Post

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val draft: String = "",
    val selectedTribuneId: String = "",
    val isPosting: Boolean = false,
    val postError: String? = null,
)
