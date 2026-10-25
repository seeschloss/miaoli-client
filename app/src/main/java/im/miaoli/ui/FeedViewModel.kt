package im.miaoli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.miaoli.data.FeedRepository
import im.miaoli.data.PostMerger
import im.miaoli.data.Tribune
import im.miaoli.data.Tribunes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class FeedViewModel(
    private val repository: FeedRepository = FeedRepository(),
    private val tribunes: List<Tribune> = Tribunes.all,
    private val postableTribunes: List<Tribune> = Tribunes.postable,
) : ViewModel() {
    private val _state = MutableStateFlow(
        FeedUiState(selectedTribuneId = postableTribunes.firstOrNull()?.id.orEmpty()),
    )
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private val refreshMutex = Mutex()
    private var manualRefresh: Job? = null

    fun updateDraft(text: String) {
        _state.update { it.copy(draft = text, postError = null) }
    }

    fun selectTribune(id: String) {
        if (postableTribunes.any { it.id == id }) {
            _state.update { it.copy(selectedTribuneId = id) }
        }
    }

    fun post() {
        val current = _state.value
        if (current.isPosting) return

        val message = current.draft.trim()
        if (message.isEmpty()) return

        val tribune = postableTribunes.firstOrNull { it.id == current.selectedTribuneId }
            ?: postableTribunes.firstOrNull()
            ?: return

        viewModelScope.launch {
            _state.update { it.copy(isPosting = true, postError = null) }

            try {
                repository.post(tribune, message)
                _state.update {
                    it.copy(
                        isPosting = false,
                        draft = "",
                        postError = null,
                        sentCount = it.sentCount + 1,
                    )
                }
                refresh(initial = false, showSpinner = false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isPosting = false,
                        postError = "Could not send to ${tribune.title}: " +
                            (error.message ?: "unknown error"),
                    )
                }
            }
        }
    }

    /**
     * Polls while the caller's lifecycle is started. The caller cancels this
     * coroutine when the screen goes to the background.
     */
    suspend fun poll() {
        while (currentCoroutineContext().isActive) {
            refresh(initial = _state.value.posts.isEmpty(), showSpinner = false)
            delay(POLL_INTERVAL_MS)
        }
    }

    fun refresh() {
        if (manualRefresh?.isActive == true) return
        manualRefresh = viewModelScope.launch {
            refresh(initial = false, showSpinner = true)
        }
    }

    private suspend fun refresh(initial: Boolean, showSpinner: Boolean) {
        if (!refreshMutex.tryLock()) return

        try {
            _state.update {
                it.copy(
                    isLoading = initial && it.posts.isEmpty(),
                    isRefreshing = showSpinner,
                )
            }

            val result = repository.fetch(tribunes)

            _state.update { current ->
                current.copy(
                    posts = PostMerger.merge(current.posts, result.posts),
                    isLoading = false,
                    isRefreshing = false,
                    errors = result.errors.mapKeys { (tribune, _) -> tribune.title },
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errors = mapOf("Feed" to (error.message ?: "Unknown error")),
                )
            }
        } finally {
            refreshMutex.unlock()
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L
    }
}
