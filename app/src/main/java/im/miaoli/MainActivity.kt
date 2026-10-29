package im.miaoli

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.miaoli.data.SharedPreferencesTotozUsageStore
import im.miaoli.notifications.SharedPreferencesNotificationStateStore
import im.miaoli.data.SharedPreferencesTribuneConfigStore
import im.miaoli.notifications.NotificationQuote
import im.miaoli.notifications.ReplyNotificationScheduler
import im.miaoli.notifications.consumeNotificationQuote
import im.miaoli.ui.FeedScreen
import im.miaoli.ui.FeedSettingsScreen
import im.miaoli.ui.FeedViewModel
import im.miaoli.ui.TribuneConfigurationScreen
import im.miaoli.ui.theme.MiaoliTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val pendingNotificationQuote = MutableStateFlow<NotificationQuote?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNotificationQuote.value = consumeNotificationQuote(intent)
        enableEdgeToEdge()

        setContent {
            MiaoliTheme {
                val context = LocalContext.current
                val configStore = remember(context) {
                    SharedPreferencesTribuneConfigStore(context.applicationContext)
                }
                val totozUsageStore = remember(context) {
                    SharedPreferencesTotozUsageStore(context.applicationContext)
                }
                val notificationStateStore = remember(context) {
                    SharedPreferencesNotificationStateStore(context.applicationContext)
                }
                val factory = remember(configStore, totozUsageStore) {
                    FeedViewModel.factory(configStore, totozUsageStore)
                }
                val viewModel: FeedViewModel = viewModel(factory = factory)
                val state by viewModel.state.collectAsStateWithLifecycle()
                val pendingQuote by pendingNotificationQuote.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current
                val feedListState = rememberLazyListState()
                var showFeedSettings by rememberSaveable { mutableStateOf(false) }
                var configuredTribuneId by rememberSaveable { mutableStateOf<Long?>(null) }
                var newTribuneId by rememberSaveable { mutableStateOf<Long?>(null) }
                var previousNotificationTribuneIds by remember {
                    mutableStateOf<Set<Long>?>(null)
                }

                LaunchedEffect(lifecycleOwner, state.config.pollingSeconds) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.poll()
                    }
                }

                LaunchedEffect(state.config) {
                    val notificationTribuneIds = state.config.tribunes
                        .filter { it.enabled && it.notifyOnAnswers }
                        .mapTo(mutableSetOf()) { it.id }
                    val newlyEnabled = previousNotificationTribuneIds
                        ?.let { previous -> notificationTribuneIds - previous }
                        .orEmpty()
                    ReplyNotificationScheduler.sync(
                        context = context.applicationContext,
                        config = state.config,
                        newlyEnabledTribuneIds = newlyEnabled,
                    )
                    previousNotificationTribuneIds = notificationTribuneIds
                }

                LaunchedEffect(pendingQuote) {
                    val quote = pendingQuote ?: return@LaunchedEffect
                    showFeedSettings = false
                    configuredTribuneId = null
                    newTribuneId = null
                    viewModel.insertClock(quote.clock, quote.tribuneId)
                    quote.postKey?.let(viewModel::revealPost)
                    viewModel.requestComposerFocus()
                    pendingNotificationQuote.value = null
                }

                val addingTribuneId = newTribuneId
                val editingTribuneId = configuredTribuneId
                when {
                    addingTribuneId != null -> TribuneConfigurationScreen(
                        config = state.config,
                        tribuneId = addingTribuneId,
                        isNew = true,
                        onBack = { newTribuneId = null },
                        onSave = { config ->
                            viewModel.saveConfig(config)
                            viewModel.refresh()
                            newTribuneId = null
                        },
                        onLoginCookies = viewModel::updateCookie,
                    )

                    editingTribuneId != null -> TribuneConfigurationScreen(
                        config = state.config,
                        tribuneId = editingTribuneId,
                        onBack = { configuredTribuneId = null },
                        onSave = { config ->
                            viewModel.saveConfig(config)
                            viewModel.refresh()
                            configuredTribuneId = null
                        },
                        onLoginCookies = viewModel::updateCookie,
                    )

                    showFeedSettings -> FeedSettingsScreen(
                        config = state.config,
                        onBack = { showFeedSettings = false },
                        onSave = { config ->
                            viewModel.saveConfig(config)
                            viewModel.refresh()
                            showFeedSettings = false
                        },
                    )

                    else -> FeedScreen(
                        state = state,
                        listState = feedListState,
                        onRefresh = viewModel::refresh,
                        onDraftChange = viewModel::updateDraft,
                        onInsertClock = viewModel::insertClock,
                        onSelectTribune = viewModel::selectTribune,
                        onToggleTribune = viewModel::toggleTribune,
                        onOpenSettings = { showFeedSettings = true },
                        onConfigureTribune = { id ->
                            newTribuneId = null
                            configuredTribuneId = id
                        },
                        onAddTribune = {
                            configuredTribuneId = null
                            newTribuneId = state.config.nextTribuneId()
                        },
                        onPost = viewModel::post,
                        onScrollRequestHandled = viewModel::consumeScrollRequest,
                        onPostsSeen = { posts -> notificationStateStore.markSeen(posts) },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNotificationQuote(intent)?.let { quote ->
            pendingNotificationQuote.value = quote
        }
    }
}
