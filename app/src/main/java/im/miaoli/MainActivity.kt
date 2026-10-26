package im.miaoli

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
import im.miaoli.data.SharedPreferencesTribuneConfigStore
import im.miaoli.ui.FeedScreen
import im.miaoli.ui.FeedSettingsScreen
import im.miaoli.ui.FeedViewModel
import im.miaoli.ui.TribuneConfigurationScreen
import im.miaoli.ui.theme.MiaoliTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MiaoliTheme {
                val context = LocalContext.current
                val configStore = remember(context) {
                    SharedPreferencesTribuneConfigStore(context.applicationContext)
                }
                val factory = remember(configStore) {
                    FeedViewModel.factory(configStore)
                }
                val viewModel: FeedViewModel = viewModel(factory = factory)
                val state by viewModel.state.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current
                val feedListState = rememberLazyListState()
                var showFeedSettings by rememberSaveable { mutableStateOf(false) }
                var configuredTribuneId by rememberSaveable { mutableStateOf<String?>(null) }

                LaunchedEffect(lifecycleOwner, state.config.pollingSeconds) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.poll()
                    }
                }

                val editingTribuneId = configuredTribuneId
                when {
                    editingTribuneId != null -> TribuneConfigurationScreen(
                        config = state.config,
                        tribuneId = editingTribuneId,
                        onBack = { configuredTribuneId = null },
                        onSave = { config ->
                            viewModel.saveConfig(config)
                            viewModel.refresh()
                            configuredTribuneId = null
                        },
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
                        onSelectTribune = viewModel::selectTribune,
                        onToggleTribune = viewModel::toggleTribune,
                        onOpenSettings = { showFeedSettings = true },
                        onConfigureTribune = { configuredTribuneId = it },
                        onPost = viewModel::post,
                    )
                }
            }
        }
    }
}
