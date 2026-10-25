package im.miaoli.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import im.miaoli.data.Post
import im.miaoli.data.Tribune

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedUiState,
    tribunes: List<Tribune>,
    postableTribunes: List<Tribune>,
    onRefresh: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSelectTribune: (String) -> Unit,
    onPost: () -> Unit,
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val newestKey = state.posts.firstOrNull()?.key

    LaunchedEffect(newestKey) {
        if (newestKey != null && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(state.sentCount) {
        if (state.sentCount > 0) {
            snackbarHostState.showSnackbar("Message sent")
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Miaoli") },
                actions = {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (postableTribunes.isNotEmpty()) {
                PostComposer(
                    state = state,
                    tribunes = postableTribunes,
                    onDraftChange = onDraftChange,
                    onSelectTribune = onSelectTribune,
                    onPost = onPost,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TribuneLegend(tribunes)

            if (state.errors.isNotEmpty()) {
                ErrorBanner(errors = state.errors, onRefresh = onRefresh)
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

                state.posts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No messages yet.",
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
                            items = state.posts,
                            key = { post -> post.key },
                        ) { post ->
                            PostRow(post)
                        }
                    }
                }
            }
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
    onDraftChange: (String) -> Unit,
    onSelectTribune: (String) -> Unit,
    onPost: () -> Unit,
) {
    val selected = tribunes.firstOrNull { it.id == state.selectedTribuneId } ?: tribunes.first()
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
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    maxLines = 4,
                    enabled = !state.isPosting,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
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
                    enabled = state.draft.isNotBlank() && !state.isPosting,
                ) {
                    if (state.isPosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
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
private fun PostRow(post: Post) {
    val tribuneColor = Color(post.tribune.color)
    val message = remember(post.message) { renderMessage(post.message) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(tribuneColor),
    ) {
        Column(
            modifier = Modifier
                .width(76.dp)
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
            )
            Text(
                text = post.author,
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

private fun renderMessage(message: String): AnnotatedString =
    runCatching { AnnotatedString.fromHtml(message) }
        .getOrElse { AnnotatedString(message) }
