package im.miaoli.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedUiState,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onDraftChange: (TextFieldValue) -> Unit,
    onInsertClock: (String, String) -> Unit,
    onSelectTribune: (String) -> Unit,
    onToggleTribune: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onConfigureTribune: (String) -> Unit,
    onPost: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val composerFocusRequester = remember { FocusRequester() }
    var composerFocusRequest by remember { mutableIntStateOf(0) }
    val enabledTribunes = state.feedTribunes.filter { it.id in state.enabledTribuneIds }
    val visiblePosts = state.posts.filter { it.tribune.id in state.enabledTribuneIds }
    val enabledTitles = enabledTribunes.mapTo(mutableSetOf()) { it.title }
    val visibleErrors = state.errors.filterKeys { it == "Feed" || it in enabledTitles }
    val newestKey = visiblePosts.firstOrNull()?.key

    LaunchedEffect(newestKey) {
        if (newestKey != null && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(composerFocusRequest) {
        if (composerFocusRequest > 0 && state.postableTribunes.isNotEmpty()) {
            composerFocusRequester.requestFocus()
            withFrameNanos { }
            keyboard?.show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TribunesDrawer(
                tribunes = state.config.tribunes,
                enabledTribuneIds = state.enabledTribuneIds,
                onToggleTribune = onToggleTribune,
                onConfigureTribune = { id ->
                    scope.launch {
                        drawerState.close()
                        onConfigureTribune(id)
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
                TopAppBar(
                    title = { Text("Miaoli") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Show tribunes")
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
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Feed settings",
                            )
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
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (enabledTribunes.isNotEmpty()) {
                    TribuneLegend(enabledTribunes)
                }

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

                    state.enabledTribuneIds.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No tribunes selected.",
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
                                    "No messages yet."
                                } else {
                                    "No messages from selected tribunes."
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
                                items = visiblePosts,
                                key = { post -> post.key },
                            ) { post ->
                                PostRow(
                                    post = post,
                                    onClick = {
                                        if (!state.isPosting) {
                                            onInsertClock(post.clock, post.tribune.id)
                                            composerFocusRequest++
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TribunesDrawer(
    tribunes: List<Tribune>,
    enabledTribuneIds: Set<String>,
    onToggleTribune: (String) -> Unit,
    onConfigureTribune: (String) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
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
                    text = "Tribunes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Use the switch to show or hide a tribune in this session. " +
                    "Tap the pencil to configure it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            tribunes.forEach { tribune ->
                TribuneToggleRow(
                    tribune = tribune,
                    enabled = tribune.id in enabledTribuneIds,
                    onToggle = { onToggleTribune(tribune.id) },
                    onConfigure = { onConfigureTribune(tribune.id) },
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
                    text = "Feed settings",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun TribuneToggleRow(
    tribune: Tribune,
    enabled: Boolean,
    onToggle: () -> Unit,
    onConfigure: () -> Unit,
) {
    val active = tribune.showInFeed && enabled

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
                    value = enabled,
                    role = Role.Switch,
                    onValueChange = { onToggle() },
                    enabled = tribune.showInFeed,
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

            if (!tribune.showInFeed) {
                Text(
                    text = "Hidden",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = null,
                enabled = tribune.showInFeed,
            )
        }

        IconButton(onClick = onConfigure) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Configure ${tribune.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TribuneLegend(tribunes: List<Tribune>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tribunes.forEach { tribune ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(tribune.color)),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = tribune.title,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    errors: Map<String, String>,
    onRefresh: () -> Unit,
) {
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
                text = errors.entries.joinToString("\n") { (tribune, error) ->
                    "$tribune: $error"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRefresh) {
                Text("Retry")
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
    onSelectTribune: (String) -> Unit,
    onPost: () -> Unit,
) {
    val selected = state.config.tribunes
        .firstOrNull { it.id == state.selectedTribuneId }
        ?: tribunes.first()
    val keyboard = LocalSoftwareKeyboardController.current
    var expanded by remember { mutableStateOf(false) }

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
                Text(
                    text = state.postError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
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
                        .focusRequester(focusRequester),
                    placeholder = { Text("Message") },
                    maxLines = 4,
                    enabled = !state.isPosting,
                    shape = RoundedCornerShape(24.dp),
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
                            contentDescription = "Send",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostRow(
    post: Post,
    onClick: () -> Unit,
) {
    val tribuneColor = Color(post.tribune.color)
    val linkColor = MaterialTheme.colorScheme.primary
    val message = remember(post.message, linkColor) {
        renderMessage(post.message, clockColor = linkColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(tribuneColor)
            .clickable(
                onClickLabel = "Insert ${post.clock} and reply to ${post.tribune.title}",
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 76.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = post.clock,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = post.author,
                modifier = Modifier.widthIn(max = 76.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
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
                color = Color(0xFF1A1A1A),
            )
        }
    }
}
