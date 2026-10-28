package im.miaoli.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import im.miaoli.R
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import im.miaoli.ui.theme.LocalMiaoliPostColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val SCROLL_TARGET_TIMEOUT_MS = 2_000L

@Composable
fun FeedScreen(
    state: FeedUiState,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onDraftChange: (TextFieldValue) -> Unit,
    onInsertClock: (String, Long) -> Unit,
    onSelectTribune: (Long) -> Unit,
    onToggleTribune: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onConfigureTribune: (Long) -> Unit,
    onAddTribune: () -> Unit,
    onPost: () -> Unit,
    onScrollRequestHandled: (Long) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val keyboard = LocalSoftwareKeyboardController.current
    val composerFocusRequester = remember { FocusRequester() }
    var composerFocusRequest by remember { mutableIntStateOf(0) }
    var selectedPostKey by remember { mutableStateOf<String?>(null) }
    var filteredPostKeys by remember { mutableStateOf<Set<String>?>(null) }
    var postRelations by remember { mutableStateOf<Map<String, PostRelation>>(emptyMap()) }
    var otherTotozUsage by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var activeTotoz by remember { mutableStateOf<TotozPopup?>(null) }
    val enabledTribunes = state.feedTribunes
    val enabledTribuneIds = remember(enabledTribunes) {
        enabledTribunes.mapTo(mutableSetOf()) { it.id }
    }
    val visiblePosts = remember(state.posts, enabledTribuneIds) {
        state.posts.filter { it.tribune.id in enabledTribuneIds }
    }
    val displayedPosts = remember(visiblePosts, filteredPostKeys) {
        filteredPostKeys?.let { keys ->
            visiblePosts.filter { it.key in keys }
        } ?: visiblePosts
    }
    val ownPostKeys = remember(visiblePosts, state.config.tribunes) {
        val tribunesById = state.config.tribunes.associateBy { it.id }
        visiblePosts
            .filter { post ->
                tribunesById[post.tribune.id]?.matchesOwnPost(post) == true
            }
            .mapTo(mutableSetOf()) { it.key }
    }
    val quoteClocks = remember(visiblePosts, state.config.useMinimalClockQuotes) {
        if (state.config.useMinimalClockQuotes) {
            minimalQuoteClocks(visiblePosts)
        } else {
            emptyMap()
        }
    }
    val enabledTitles = remember(enabledTribunes) {
        enabledTribunes.mapTo(mutableSetOf()) { it.title }
    }
    val visibleErrors = remember(state.errors, enabledTitles) {
        state.errors.filterKeys { it == "Feed" || it in enabledTitles }
    }
    val newestVisibleKey = visiblePosts.firstOrNull()?.key
    val latestDisplayedPosts by rememberUpdatedState(displayedPosts)
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val isAtBottom by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
        }
    }

    BackHandler(enabled = filteredPostKeys != null || selectedPostKey != null) {
        filteredPostKeys = null
        selectedPostKey = null
    }

    LaunchedEffect(displayedPosts) {
        prefetchRenderedMessages(displayedPosts)
    }

    LaunchedEffect(visiblePosts) {
        val names = collectTotozNames(visiblePosts)
        if (names.isNotEmpty()) {
            TotozImageCache.prefetch(context, names)
        }
    }

    LaunchedEffect(visiblePosts, state.config.tribunes) {
        otherTotozUsage = collectOtherTotozCounts(
            posts = visiblePosts,
            tribunes = state.config.tribunes,
        )
        postRelations = computePostRelations(visiblePosts, state.config.tribunes)
    }

    LaunchedEffect(newestVisibleKey) {
        if (filteredPostKeys == null &&
            newestVisibleKey != null &&
            listState.firstVisibleItemIndex <= 1
        ) {
            listState.animateScrollToItem(0)
        }
    }

    // A post can arrive while the keyboard is still hiding. Wait for the IME
    // inset animation to finish so the final scroll uses the settled viewport.
    LaunchedEffect(state.scrollRequest?.id, imeVisible) {
        val request = state.scrollRequest ?: return@LaunchedEffect
        if (imeVisible) return@LaunchedEffect

        withFrameNanos { }

        val targetIndex = if (request.postKey == null) {
            0
        } else {
            withTimeoutOrNull(SCROLL_TARGET_TIMEOUT_MS) {
                snapshotFlow {
                    latestDisplayedPosts.indexOfFirst { it.key == request.postKey }
                }.first { it >= 0 }
            } ?: 0
        }

        if (latestDisplayedPosts.isNotEmpty()) {
            listState.animateScrollToItem(targetIndex)
        }
        onScrollRequestHandled(request.id)
    }

    LaunchedEffect(composerFocusRequest) {
        if (composerFocusRequest > 0 && state.postableTribunes.isNotEmpty()) {
            composerFocusRequester.requestFocus()
            withFrameNanos { }
            keyboard?.show()
        }
    }

    fun openClockReference(origin: Post, reference: ClockReference) {
        selectedPostKey = null
        when (
            val result = resolveClockTarget(
                origin = origin,
                reference = reference,
                tribunes = state.config.tribunes,
                posts = displayedPosts,
            )
        ) {
            is ClockTargetResult.Found -> {
                val index = displayedPosts.indexOfFirst { it.key == result.post.key }
                if (index >= 0) {
                    selectedPostKey = result.post.key
                    scope.launch { listState.animateItemToTop(index) }
                } else {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.clock_not_found, reference.text),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }

            is ClockTargetResult.NoTribune -> Toast.makeText(
                context,
                resources.getString(R.string.clock_tribune_not_found, result.title),
                Toast.LENGTH_LONG,
            ).show()

            is ClockTargetResult.NoMessage -> Toast.makeText(
                context,
                resources.getString(R.string.clock_not_found, result.reference.text),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun filterToClock(origin: Post, reference: ClockReference) {
        selectedPostKey = null
        filteredPostKeys = reachableClockPostKeys(
            origin = origin,
            reference = reference,
            tribunes = state.config.tribunes,
            posts = visiblePosts,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
            TribunesDrawer(
                tribunes = state.config.tribunes,
                onToggleTribune = onToggleTribune,
                onConfigureTribune = { id ->
                    scope.launch {
                        drawerState.close()
                        onConfigureTribune(id)
                    }
                },
                onAddTribune = {
                    scope.launch {
                        drawerState.close()
                        onAddTribune()
                    }
                },
                onClose = { scope.launch { drawerState.close() } },
                onOpenSettings = {
                    scope.launch {
                        drawerState.close()
                        onOpenSettings()
                    }
                },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                MiaoliTopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.show_tribunes),
                            )
                        }
                    },
                    actions = {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh),
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (state.postableTribunes.isNotEmpty()) {
                    PostComposer(
                        state = state,
                        tribunes = state.postableTribunes,
                        focusRequester = composerFocusRequester,
                        onDraftChange = onDraftChange,
                        onSelectTribune = onSelectTribune,
                        onPost = onPost,
                        totozUsage = state.totozUsage,
                        otherTotozUsage = otherTotozUsage,
                    )
                }
            },
            floatingActionButton = {
                if (displayedPosts.isNotEmpty() && !isAtBottom) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.scroll_to_bottom),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (visibleErrors.isNotEmpty()) {
                    ErrorBanner(errors = visibleErrors, onRefresh = onRefresh)
                }

                when {
                    state.isLoading && state.posts.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    enabledTribunes.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.no_tribunes_enabled),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    visiblePosts.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (state.posts.isEmpty()) {
                                    stringResource(R.string.no_messages_yet)
                                } else {
                                    stringResource(R.string.no_messages_from_enabled_tribunes)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            reverseLayout = true,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(
                                items = displayedPosts,
                                key = { post -> post.key },
                            ) { post ->
                                PostRow(
                                    post = post,
                                    relation = when {
                                        post.key in ownPostKeys -> PostRelation.Own
                                        else -> postRelations[post.key] ?: PostRelation.None
                                    },
                                    selected = post.key == selectedPostKey,
                                    onClick = {
                                        if (!state.isPosting) {
                                            onInsertClock(
                                                quoteClocks[post.key] ?: post.clock,
                                                post.tribune.id,
                                            )
                                            composerFocusRequest++
                                        }
                                    },
                                    onClockClick = { reference ->
                                        openClockReference(post, reference)
                                    },
                                    onClockLongClick = { reference ->
                                        filterToClock(post, reference)
                                    },
                                    onTotozPress = { name, anchor ->
                                        activeTotoz = TotozPopup(name, anchor)
                                    },
                                    onTotozRelease = {
                                        activeTotoz = null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    activeTotoz?.let { popup ->
        TotozOverlay(popup)
    }
    }
}

@Composable
private fun TribunesDrawer(
    tribunes: List<Tribune>,
    onToggleTribune: (Long) -> Unit,
    onConfigureTribune: (Long) -> Unit,
    onAddTribune: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    ModalDrawerSheet(
        modifier = Modifier
            .width(300.dp)
            .padding(top = statusBarTop),
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tribunes),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            tribunes.forEach { tribune ->
                TribuneToggleRow(
                    tribune = tribune,
                    onToggle = { onToggleTribune(tribune.id) },
                    onConfigure = { onConfigureTribune(tribune.id) },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddTribune)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.new_tribune_ellipsis),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun TribuneToggleRow(
    tribune: Tribune,
    onToggle: () -> Unit,
    onConfigure: () -> Unit,
) {
    val active = tribune.enabled

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .toggleable(
                    value = tribune.enabled,
                    role = Role.Switch,
                    onValueChange = { onToggle() },
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            Color(tribune.color)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = tribune.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (active) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Switch(
                checked = tribune.enabled,
                onCheckedChange = null,
            )
        }

        IconButton(onClick = onConfigure) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(
                    R.string.configure_tribune,
                    tribune.title,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    errors: Map<String, String>,
    onRefresh: () -> Unit,
) {
    val resources = LocalResources.current
    val errorText = errors.entries.joinToString("\n") { (tribune, error) ->
        val label = if (tribune == "Feed") {
            resources.getString(R.string.feed)
        } else {
            tribune
        }
        resources.getString(
            R.string.error_entry,
            label,
            error.ifBlank { resources.getString(R.string.unknown_error) },
        )
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = errorText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRefresh) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun PostComposer(
    state: FeedUiState,
    tribunes: List<Tribune>,
    focusRequester: FocusRequester,
    onDraftChange: (TextFieldValue) -> Unit,
    onSelectTribune: (Long) -> Unit,
    onPost: () -> Unit,
    totozUsage: Map<String, Int>,
    otherTotozUsage: Map<String, Int>,
) {
    val selected = state.config.tribunes
        .firstOrNull { it.id == state.selectedTribuneId }
        ?: tribunes.first()
    val keyboard = LocalSoftwareKeyboardController.current
    var expanded by remember { mutableStateOf(false) }
    var isComposerFocused by remember { mutableStateOf(false) }
    val totozQuery = remember(state.draft.value) {
        incompleteTotozQuery(state.draft.value)
    }
    val suggestions = remember(totozQuery, totozUsage, otherTotozUsage) {
        totozQuery?.let { query ->
            buildTotozSuggestions(
                ownUsage = totozUsage,
                otherUsage = otherTotozUsage,
                query = query.query,
            )
        }.orEmpty()
    }
    val linkColor = MaterialTheme.colorScheme.primary
    val draftVisualTransformation = remember(linkColor) {
        DraftVisualTransformation(
            clockColor = linkColor,
            linkColor = linkColor,
        )
    }

    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            if (state.postError != null) {
                val detail = state.postError.detail
                    ?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.unknown_error)
                Text(
                    text = stringResource(
                        R.string.post_failed,
                        state.postError.tribuneTitle,
                        detail,
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }

            if (isComposerFocused &&
                totozQuery != null &&
                suggestions.isNotEmpty()
            ) {
                TotozSuggestionsRow(
                    suggestions = suggestions,
                    onSelect = { name ->
                        onDraftChange(
                            completeTotoz(
                                value = state.draft.value,
                                start = totozQuery.start,
                                name = name,
                            ),
                        )
                        focusRequester.requestFocus()
                    },
                )
            } else {
                AnimatedVisibility(
                    visible = isComposerFocused,
                    enter = fadeIn(tween(100)) +
                        expandVertically(
                            animationSpec = tween(120),
                            expandFrom = Alignment.Top,
                        ),
                    exit = fadeOut(tween(80)) +
                        shrinkVertically(
                            animationSpec = tween(100),
                            shrinkTowards = Alignment.Top,
                        ),
                ) {
                    FormattingBar(
                        enabled = !state.isPosting,
                        onInsert = { opening, closing ->
                            onDraftChange(
                                insertPair(state.draft.value, opening, closing),
                            )
                            focusRequester.requestFocus()
                        },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    TextButton(
                        onClick = { expanded = true },
                        enabled = !state.isPosting,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(selected.color)),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = selected.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        tribunes.forEach { tribune ->
                            DropdownMenuItem(
                                text = { Text(tribune.title) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(tribune.color)),
                                    )
                                },
                                onClick = {
                                    onSelectTribune(tribune.id)
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.draft.value,
                    onValueChange = onDraftChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isComposerFocused = it.isFocused },
                    placeholder = { Text(stringResource(R.string.message_placeholder)) },
                    maxLines = 4,
                    enabled = !state.isPosting,
                    shape = RoundedCornerShape(24.dp),
                    visualTransformation = draftVisualTransformation,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            keyboard?.hide()
                            onPost()
                        },
                    ),
                )

                Spacer(Modifier.width(4.dp))

                FilledIconButton(
                    onClick = {
                        keyboard?.hide()
                        onPost()
                    },
                    enabled = state.draft.text.isNotBlank() && !state.isPosting,
                ) {
                    if (state.isPosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotozSuggestionsRow(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        suggestions.forEach { name ->
            TextButton(
                onClick = { onSelect(name) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(
                    text = "[:$name]",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FormattingBar(
    enabled: Boolean,
    onInsert: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        FormattingButton(
            label = "B",
            enabled = enabled,
            textStyle = TextStyle(fontWeight = FontWeight.Bold),
            onClick = { onInsert("<b>", "</b>") },
        )
        FormattingButton(
            label = "I",
            enabled = enabled,
            textStyle = TextStyle(fontStyle = FontStyle.Italic),
            onClick = { onInsert("<i>", "</i>") },
        )
        FormattingButton(
            label = "U",
            enabled = enabled,
            textStyle = TextStyle(textDecoration = TextDecoration.Underline),
            onClick = { onInsert("<u>", "</u>") },
        )
        FormattingButton(
            label = "S",
            enabled = enabled,
            textStyle = TextStyle(textDecoration = TextDecoration.LineThrough),
            onClick = { onInsert("<s>", "</s>") },
        )
        FormattingButton(
            label = "tt",
            enabled = enabled,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            onClick = { onInsert("<tt>", "</tt>") },
        )
        FormattingButton(
            label = "[:]",
            enabled = enabled,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            onClick = { onInsert("[:", "]") },
        )
    }
}

@Composable
private fun FormattingButton(
    label: String,
    enabled: Boolean,
    textStyle: TextStyle,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.merge(textStyle),
        )
    }
}

private fun LazyListState.isItemFullyVisible(index: Int): Boolean {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    return item.offset >= info.viewportStartOffset &&
        item.offset + item.size <= info.viewportEndOffset
}

/**
 * Brings [index] to the visual top unless it is already fully visible. The
 * first scroll gets an off-screen item composed; the second aligns it after
 * its measured offset is available.
 */
private suspend fun LazyListState.animateItemToTop(index: Int) {
    if (index !in 0 until layoutInfo.totalItemsCount) return
    if (isItemFullyVisible(index)) return

    animateScrollToItem(index)
    withFrameNanos { }

    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    // The list is reversed, so moving an item upward requires scrolling
    // backward in the list's coordinate system. Depending on the layout
    // phase, an item at the visual bottom can report a zero offset, so use
    // the remaining viewport span as a fallback.
    val delta = item.offset - info.viewportStartOffset
    val atScrollStart = firstVisibleItemIndex == index &&
        firstVisibleItemScrollOffset == 0
    if (delta != 0) {
        animateScrollBy(-delta.toFloat())
    } else if (atScrollStart) {
        val span = info.viewportEndOffset - info.viewportStartOffset
        val distance = (span - item.size).coerceAtLeast(0)
        if (distance > 0) {
            animateScrollBy(-distance.toFloat())
        }
    }
}

private fun totozAt(
    layout: TextLayoutResult,
    position: Offset,
    references: List<TotozReference>,
): TotozReference? {
    if (position.x < 0f || position.y < 0f ||
        position.x > layout.size.width.toFloat() ||
        position.y > layout.size.height.toFloat()
    ) {
        return null
    }
    val offset = layout.getOffsetForPosition(position)
    return references.firstOrNull { offset in it.start until it.end }
}

private fun totozAnchor(
    layout: TextLayoutResult,
    coordinates: LayoutCoordinates?,
    reference: TotozReference,
): Rect? {
    val position = coordinates?.positionInRoot() ?: return null
    val start = layout.getBoundingBox(reference.start.coerceIn(0, layout.layoutInput.text.length - 1))
    val end = layout.getBoundingBox(
        (reference.end - 1).coerceIn(0, layout.layoutInput.text.length - 1),
    )
    return Rect(
        left = position.x + minOf(start.left, end.left),
        top = position.y + minOf(start.top, end.top),
        right = position.x + maxOf(start.right, end.right),
        bottom = position.y + maxOf(start.bottom, end.bottom),
    )
}

private fun clockAt(
    layout: TextLayoutResult,
    position: Offset,
    references: List<ClockReference>,
): ClockReference? {
    if (position.x < 0f || position.y < 0f ||
        position.x > layout.size.width.toFloat() ||
        position.y > layout.size.height.toFloat()
    ) {
        return null
    }
    val offset = layout.getOffsetForPosition(position)
    return references.firstOrNull { offset in it.start until it.end }
}

@Composable
private fun PostRow(
    post: Post,
    relation: PostRelation,
    selected: Boolean,
    onClick: () -> Unit,
    onClockClick: (ClockReference) -> Unit,
    onClockLongClick: (ClockReference) -> Unit,
    onTotozPress: (String, Rect) -> Unit,
    onTotozRelease: () -> Unit,
) {
    val tribuneColor = Color(post.tribune.color)
    val messageColor = readableTextColor(tribuneColor)
    val linkColor = readableLinkColor(tribuneColor)
    val postColors = LocalMiaoliPostColors.current
    val leftColumnBackground = when (relation) {
        PostRelation.Own -> postColors.ownContainer
        PostRelation.ReplyToOwn -> postColors.replyContainer
        PostRelation.None -> MaterialTheme.colorScheme.surface
    }
    val leftColumnContent = when (relation) {
        PostRelation.Own -> postColors.ownContent
        PostRelation.ReplyToOwn -> postColors.replyContent
        PostRelation.None -> MaterialTheme.colorScheme.onSurface
    }
    val authorColor = if (relation == PostRelation.None) {
        MaterialTheme.colorScheme.primary
    } else {
        leftColumnContent
    }
    val selectedOverlay = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val haptic = LocalHapticFeedback.current
    val latestOnClockClick by rememberUpdatedState(onClockClick)
    val latestOnClockLongClick by rememberUpdatedState(onClockLongClick)
    val latestOnTotozPress by rememberUpdatedState(onTotozPress)
    val latestOnTotozRelease by rememberUpdatedState(onTotozRelease)
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val latestTextCoordinates by rememberUpdatedState(textCoordinates)
    val rendered = remember(post.message, linkColor) {
        renderedMessage(post.message, linkColor)
    }
    val message = rendered.text
    val clockRefs = rendered.clockReferences
    val totozRefs = rendered.totozReferences
    val insertClockLabel = stringResource(
        R.string.insert_clock_and_reply,
        post.clock,
        post.tribune.title,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(tribuneColor)
            .drawWithContent {
                drawContent()
                if (selected) {
                    drawRect(selectedOverlay)
                }
            }
            .clickable(
                onClickLabel = insertClockLabel,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 76.dp)
                .fillMaxHeight()
                .background(leftColumnBackground)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = post.clock,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = leftColumnContent,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = post.author,
                modifier = Modifier.widthIn(max = 76.dp),
                style = MaterialTheme.typography.labelSmall,
                color = authorColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = messageColor,
                onTextLayout = { textLayout = it },
                modifier = Modifier
                    .onGloballyPositioned { textCoordinates = it }
                    .pointerInput(message) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val layout = textLayout ?: return@awaitEachGesture

                            val totoz = totozAt(layout, down.position, totozRefs)
                            if (totoz != null) {
                                // Claim the gesture so the row's click handler
                                // cannot turn a totoz press into a reply.
                                down.consume()
                                try {
                                    totozAnchor(
                                        layout = layout,
                                        coordinates = latestTextCoordinates,
                                        reference = totoz,
                                    )?.let { anchor ->
                                        latestOnTotozPress(totoz.name, anchor)
                                    }
                                    waitForUpOrCancellation()?.consume()
                                } finally {
                                    latestOnTotozRelease()
                                }
                                return@awaitEachGesture
                            }

                            val reference = clockAt(layout, down.position, clockRefs)
                                ?: return@awaitEachGesture
                            // Claim the gesture so the row's click handler cannot also
                            // treat a clock tap or long press as a reply action.
                            down.consume()

                            var canceled = false
                            val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                waitForUpOrCancellation().also { change ->
                                    if (change == null) {
                                        canceled = true
                                    }
                                }
                            }

                            if (up != null) {
                                if (!up.isConsumed &&
                                    clockAt(layout, up.position, clockRefs) == reference
                                ) {
                                    up.consume()
                                    latestOnClockClick(reference)
                                }
                            } else if (!canceled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                latestOnClockLongClick(reference)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                        }
                    },
            )
        }
    }
}
