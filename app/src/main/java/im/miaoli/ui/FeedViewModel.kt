package im.miaoli.ui

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import im.miaoli.data.FeedRepository
import im.miaoli.data.PostMerger
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig
import im.miaoli.data.TribuneConfigStore
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
    private val configStore: TribuneConfigStore,
    private val repository: FeedRepository = FeedRepository(),
) : ViewModel() {
    private val initialConfig = configStore.load().normalized()
    private val _state = MutableStateFlow(
        FeedUiState(
            config = initialConfig,
            selectedTribuneId = initialConfig.postableTribunes.firstOrNull()?.id,
        ),
    )
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private val refreshMutex = Mutex()
    private var manualRefresh: Job? = null
    private var nextScrollRequestId = 0L

    fun updateDraft(value: TextFieldValue) {
        _state.update { current ->
            current.copy(
                draft = DraftEditor.updateValue(current.draft, value),
                postError = null,
            )
        }
    }

    /**
     * Inserts a quoted clock at the current cursor/selection. A postable
     * origin becomes the new destination; a read-only origin leaves the
     * destination unchanged.
     */
    fun insertClock(clock: String, originTribuneId: Long) {
        _state.update { current ->
            val origin = current.config.tribunes
                .firstOrNull { it.id == originTribuneId }
                ?: return@update current
            val destinationId = if (origin.canPost) {
                origin.id
            } else {
                current.selectedTribuneId
            }

            current.copy(
                selectedTribuneId = destinationId,
                draft = DraftEditor.insertClock(
                    current = current.draft,
                    clock = clock,
                    origin = origin,
                    destinationId = destinationId,
                    config = current.config,
                ),
                postError = null,
            )
        }
    }

    /**
     * Switches the composer destination. Only postable tribunes can be
     * selected. Existing clocks are rewritten relative to the new destination.
     */
    fun selectTribune(id: Long) {
        _state.update { current ->
            if (id == current.selectedTribuneId) {
                current.copy(postError = null)
            } else if (current.config.postableTribunes.any { it.id == id }) {
                current.copy(
                    selectedTribuneId = id,
                    draft = DraftEditor.rewriteClocks(current.draft, id, current.config),
                    postError = null,
                )
            } else {
                current
            }
        }
    }

    fun toggleTribune(id: Long) {
        val current = _state.value
        val tribune = current.config.tribunes.firstOrNull { it.id == id } ?: return
        val updatedConfig = current.config.copy(
            tribunes = current.config.tribunes.map {
                if (it.id == tribune.id) it.copy(enabled = !it.enabled) else it
            },
        )
        configStore.save(updatedConfig)
        _state.update { it.copy(config = updatedConfig) }
    }

    /**
     * Applies a validated configuration, updates existing rows to the new
     * tribune metadata, and persists it. The caller is responsible for
     * triggering [refresh] if an immediate reload is wanted.
     */
    fun saveConfig(newConfig: TribuneConfig) {
        val normalized = newConfig.normalized()
        configStore.save(normalized)

        _state.update { current ->
            val changedSources = normalized.tribunes
                .filter { new ->
                    val old = current.config.tribunes.firstOrNull { it.id == new.id }
                    old != null &&
                        (old.backendUrl != new.backendUrl || old.format != new.format)
                }
                .mapTo(mutableSetOf()) { it.id }
            val tribunesById = normalized.tribunes.associateBy { it.id }
            val newSelectedId = normalized.postableTribunes
                .firstOrNull { it.id == current.selectedTribuneId }
                ?.id
                ?: normalized.postableTribunes.firstOrNull()?.id

            current.copy(
                config = normalized,
                posts = PostMerger.merge(
                    existing = current.posts
                        .filterNot { it.tribune.id in changedSources }
                        .mapNotNull { post ->
                            tribunesById[post.tribune.id]?.let { post.withTribune(it) }
                        },
                    incoming = emptyList(),
                ),
                selectedTribuneId = newSelectedId,
                draft = if (newSelectedId != current.selectedTribuneId) {
                    DraftEditor.rewriteClocks(current.draft, newSelectedId, normalized)
                } else {
                    current.draft
                },
                errors = emptyMap(),
                postError = null,
            )
        }
    }

    fun post() {
        val current = _state.value
        if (current.isPosting) return

        val message = current.draft.text.trim()
        if (message.isEmpty()) return

        val tribune = current.postableTribunes
            .firstOrNull { it.id == current.selectedTribuneId }
            ?: return

        viewModelScope.launch {
            _state.update { it.copy(isPosting = true, postError = null) }

            try {
                val receipt = repository.post(tribune, message)
                _state.update {
                    it.copy(
                        isPosting = false,
                        draft = DraftState(),
                        postError = null,
                    )
                }
                refresh(initial = false, showSpinner = false)

                val postKey = if (
                    _state.value.config.tribunes
                        .firstOrNull { it.id == tribune.id }
                        ?.enabled == true
                ) {
                    receipt.postId?.let { postId -> "${tribune.id}:$postId" }
                } else {
                    null
                }
                val requestId = ++nextScrollRequestId
                _state.update {
                    it.copy(
                        scrollRequest = FeedScrollRequest(
                            id = requestId,
                            postKey = postKey,
                        ),
                    )
                }
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

    /** Clears a scroll request after the feed has handled it. */
    fun consumeScrollRequest(id: Long) {
        _state.update { current ->
            if (current.scrollRequest?.id == id) {
                current.copy(scrollRequest = null)
            } else {
                current
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
            val seconds = _state.value.config.pollingSeconds
                .coerceAtLeast(TribuneConfig.MIN_POLLING_SECONDS)
            delay(seconds * 1000L)
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

            val feedTribunes = _state.value.config.feedTribunes
            val result = repository.fetch(feedTribunes)

            _state.update { current ->
                fun currentTribuneFor(fetched: Tribune): Tribune? {
                    val currentTribune = current.config.tribunes
                        .firstOrNull { it.id == fetched.id }
                        ?: return null
                    if (currentTribune.backendUrl != fetched.backendUrl ||
                        currentTribune.format != fetched.format
                    ) {
                        return null
                    }
                    return currentTribune
                }

                val incoming = result.posts.mapNotNull { post ->
                    currentTribuneFor(post.tribune)?.let { post.withTribune(it) }
                }
                val errors = result.errors.mapNotNull { (fetched, message) ->
                    currentTribuneFor(fetched)?.let { it.title to message }
                }.toMap()

                current.copy(
                    posts = PostMerger.merge(current.posts, incoming),
                    isLoading = false,
                    isRefreshing = false,
                    errors = errors,
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
        fun factory(configStore: TribuneConfigStore): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { FeedViewModel(configStore = configStore) }
            }
    }
}
