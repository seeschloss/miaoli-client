package im.miaoli.ui

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import im.miaoli.data.FeedDataSource
import im.miaoli.data.FeedRepository
import im.miaoli.data.NoopTotozUsageStore
import im.miaoli.data.Post
import im.miaoli.data.PostMerger
import im.miaoli.data.TotozUsageStore
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
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val HTML_TAG_PATTERN = Regex("<[^>]*>")

class FeedViewModel(
    private val configStore: TribuneConfigStore,
    private val repository: FeedDataSource = FeedRepository(),
    private val totozUsageStore: TotozUsageStore = NoopTotozUsageStore,
) : ViewModel() {
    private val initialConfig = configStore.load().normalized()
    private val _state = MutableStateFlow(
        FeedUiState(
            config = initialConfig,
            selectedTribuneId = initialConfig.postableTribunes.firstOrNull()?.id,
            totozUsage = totozUsageStore.load(),
        ),
    )
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private val refreshMutex = Mutex()
    private var manualRefresh: Job? = null
    private var nextScrollRequestId = 0L
    private var pendingLoginDetection: PendingLoginDetection? = null

    fun updateDraft(value: TextFieldValue) {
        _state.update { current ->
            current.copy(
                draft = DraftEditor.updateValue(current.draft, value),
                postError = null,
            )
        }
    }

    /** Requests focus for the composer after an external action, e.g. a notification. */
    fun requestComposerFocus() {
        _state.update { current ->
            current.copy(composerFocusRequest = current.composerFocusRequest + 1L)
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

    /**
     * Remembers an ID returned by a tribune after posting, so later feed
     * refreshes can identify this device's messages.
     */
    private fun recordOwnPost(tribuneId: Long, postId: String) {
        val updated = _state.updateAndGet { current ->
            current.copy(
                config = current.config.copy(
                    tribunes = current.config.tribunes.map { tribune ->
                        if (tribune.id == tribuneId) {
                            tribune.copy(
                                ownPostIds = (
                                    tribune.ownPostIds.filterNot { it == postId } + postId
                                ).takeLast(Tribune.MAX_OWN_POST_IDS),
                            )
                        } else {
                            tribune
                        }
                    },
                ),
            )
        }
        configStore.save(updated.config)
    }

    private fun recordTotozUsage(message: String) {
        val counts = totozReferences(message)
            .groupingBy { it.name }
            .eachCount()
        if (counts.isEmpty()) return

        val updated = _state.updateAndGet { current ->
            val usage = current.totozUsage.toMutableMap()
            counts.forEach { (name, count) ->
                usage[name] = (usage[name] ?: 0) + count
            }
            current.copy(totozUsage = usage)
        }
        totozUsageStore.save(updated.totozUsage)
    }

    /** Stores cookies captured by the in-app login browser for [id]. */
    fun updateCookie(id: Long, cookie: String) {
        val current = _state.value
        if (current.config.tribunes.none { it.id == id }) return
        val updatedConfig = current.config.copy(
            tribunes = current.config.tribunes.map {
                if (it.id == id) it.copy(cookie = cookie.trim()) else it
            },
        )
        configStore.save(updatedConfig)
        _state.update { it.copy(config = updatedConfig) }
    }

    fun toggleTribune(id: Long) {
        val current = _state.value
        val tribune = current.config.tribunes.firstOrNull { it.id == id } ?: return
        val enabled = !tribune.enabled
        val updatedConfig = current.config.copy(
            tribunes = current.config.tribunes.map {
                if (it.id == tribune.id) it.copy(enabled = enabled) else it
            },
        )
        configStore.save(updatedConfig)
        _state.update { it.copy(config = updatedConfig) }

        if (enabled) {
            viewModelScope.launch { reloadTribune(tribune.id) }
        }
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
        val baselineId = current.posts
            .asSequence()
            .filter { it.tribune.id == tribune.id }
            .mapNotNull { it.id.toLongOrNull() }
            .maxOrNull() ?: 0L

        viewModelScope.launch {
            _state.update { it.copy(isPosting = true, postError = null) }

            try {
                val receipt = repository.post(tribune, message)
                receipt.postId?.let { postId ->
                    recordOwnPost(tribune.id, postId)
                }
                if (receipt.postId == null || tribune.username.isBlank()) {
                    pendingLoginDetection = PendingLoginDetection(
                        tribuneId = tribune.id,
                        postedId = receipt.postId,
                        normalizedMessage = normalizedLoginText(message),
                        userAgent = tribune.userAgent.trim(),
                        baselineId = baselineId,
                    )
                }
                recordTotozUsage(message)
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
                        postError = PostError(
                            tribuneTitle = tribune.title,
                            detail = error.message,
                        ),
                    )
                }
            }
        }
    }

    /** Requests that the feed reveal [postKey], e.g. from a notification. */
    fun revealPost(postKey: String) {
        val requestId = ++nextScrollRequestId
        _state.update {
            it.copy(
                scrollRequest = FeedScrollRequest(
                    id = requestId,
                    postKey = postKey,
                    revealWhileImeVisible = true,
                    alignToTop = true,
                ),
            )
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

    /**
     * Fetches one just-enabled tribune so its messages appear without waiting
     * for the next scheduled poll.
     */
    private suspend fun reloadTribune(id: Long) {
        refreshMutex.withLock {
            val tribune = _state.value.config.tribunes
                .firstOrNull { it.id == id }
                ?.takeIf { it.enabled }
                ?: return@withLock

            try {
                val result = repository.fetch(listOf(tribune))
                _state.update { current ->
                    val currentTribune = current.config.tribunes
                        .firstOrNull { it.id == id }
                    if (currentTribune == null ||
                        !currentTribune.enabled ||
                        currentTribune.backendUrl != tribune.backendUrl ||
                        currentTribune.format != tribune.format
                    ) {
                        return@update current
                    }

                    val incoming = result.posts.mapNotNull { post ->
                        if (post.tribune.backendUrl == tribune.backendUrl &&
                            post.tribune.format == tribune.format
                        ) {
                            post.withTribune(currentTribune)
                        } else {
                            null
                        }
                    }
                    val errors = current.errors.toMutableMap()
                    errors.remove(currentTribune.title)
                    result.errors[tribune]?.let { message ->
                        errors[currentTribune.title] = message
                    }

                    current.copy(
                        posts = PostMerger.merge(current.posts, incoming),
                        errors = errors,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { current ->
                    val currentTribune = current.config.tribunes
                        .firstOrNull { it.id == id }
                        ?: return@update current
                    current.copy(
                        errors = current.errors + (
                            currentTribune.title to error.message.orEmpty()
                            ),
                    )
                }
            }
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
            maybeCompleteLoginDetection()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errors = mapOf("Feed" to error.message.orEmpty()),
                )
            }
        } finally {
            refreshMutex.unlock()
        }
    }

    private fun maybeCompleteLoginDetection() {
        val pending = pendingLoginDetection ?: return
        val current = _state.value
        val tribune = current.config.tribunes
            .firstOrNull { it.id == pending.tribuneId }
            ?: run {
                pendingLoginDetection = null
                return
            }
        if (!tribune.canPost) {
            pendingLoginDetection = null
            return
        }

        val candidate = if (pending.postedId != null) {
            current.posts.firstOrNull {
                it.tribune.id == pending.tribuneId && it.id == pending.postedId
            }
        } else {
            val newPosts = current.posts.filter { post ->
                post.tribune.id == pending.tribuneId &&
                    (post.id.toLongOrNull() ?: Long.MIN_VALUE) > pending.baselineId
            }
            when {
                newPosts.size == 1 -> newPosts.single()
                newPosts.size > 1 -> {
                    val matches = newPosts.filter { post ->
                        post.authorInfo.trim() == pending.userAgent &&
                            normalizedLoginText(post.message)
                                .contains(pending.normalizedMessage)
                    }
                    if (matches.size == 1) matches.single() else null
                }
                else -> null
            }
        }

        if (candidate != null) {
            recordOwnPost(pending.tribuneId, candidate.id)
            if (tribune.username.isBlank()) {
                val login = candidate.login.trim()
                if (login.isNotEmpty()) {
                    recordOwnLogin(pending.tribuneId, login)
                }
            }
            pendingLoginDetection = null
            return
        }

        pending.remainingChecks -= 1
        if (pending.remainingChecks <= 0) {
            pendingLoginDetection = null
        }
    }

    private fun recordOwnLogin(tribuneId: Long, login: String) {
        val updated = _state.updateAndGet { current ->
            current.copy(
                config = current.config.copy(
                    tribunes = current.config.tribunes.map { tribune ->
                        if (tribune.id == tribuneId && tribune.username.isBlank()) {
                            tribune.copy(username = login)
                        } else {
                            tribune
                        }
                    },
                ),
            )
        }
        configStore.save(updated.config)
    }

    private fun normalizedLoginText(message: String): String =
        parseMessage(message).text
            .replace(HTML_TAG_PATTERN, "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .filterNot { character ->
                character.isWhitespace() || character == '\u00A0'
            }

    private data class PendingLoginDetection(
        val tribuneId: Long,
        val postedId: String?,
        val normalizedMessage: String,
        val userAgent: String,
        val baselineId: Long,
        var remainingChecks: Int = 3,
    )

    companion object {
        fun factory(
            configStore: TribuneConfigStore,
            totozUsageStore: TotozUsageStore = NoopTotozUsageStore,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    FeedViewModel(
                        configStore = configStore,
                        totozUsageStore = totozUsageStore,
                    )
                }
            }
    }
}
