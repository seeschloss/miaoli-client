package im.miaoli.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import im.miaoli.data.FeedFormat
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig

@Composable
fun FeedSettingsScreen(
    config: TribuneConfig,
    onBack: () -> Unit,
    onSave: (TribuneConfig) -> Unit,
) {
    var pollingSeconds by remember(config) { mutableStateOf(config.pollingSeconds.toString()) }
    var pollingError by remember(config) { mutableStateOf<String?>(null) }

    fun save() {
        val error = pollingErrorMessage(pollingSeconds)
        pollingError = error
        if (error == null) {
            onSave(config.copy(pollingSeconds = pollingSeconds.toInt()))
        }
    }

    BackHandler { onBack() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            MiaoliTopAppBar(
                title = { Text("Feed settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = ::save) {
                        Text("Save")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Polling",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            item {
                OutlinedTextField(
                    value = pollingSeconds,
                    onValueChange = { value ->
                        pollingSeconds = value.filter(Char::isDigit)
                        pollingError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Refresh interval (seconds)") },
                    supportingText = errorSupportingText(pollingError),
                    isError = pollingError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }
    }
}

@Composable
fun TribuneConfigurationScreen(
    config: TribuneConfig,
    tribuneId: String,
    onBack: () -> Unit,
    onSave: (TribuneConfig) -> Unit,
) {
    val tribune = config.tribunes.firstOrNull { it.id == tribuneId }
    if (tribune == null) {
        MissingTribuneScreen(onBack = onBack)
    } else {
        TribuneConfigurationContent(
            config = config,
            tribune = tribune,
            onBack = onBack,
            onSave = onSave,
        )
    }
}

@Composable
private fun TribuneConfigurationContent(
    config: TribuneConfig,
    tribune: Tribune,
    onBack: () -> Unit,
    onSave: (TribuneConfig) -> Unit,
) {
    var draft by remember(config, tribune.id) { mutableStateOf(TribuneDraft.from(tribune)) }
    var errors by remember(config, tribune.id) { mutableStateOf<Map<String, String>?>(null) }

    fun updateDraft(transform: (TribuneDraft) -> TribuneDraft) {
        draft = transform(draft)
        errors = null
    }

    fun save() {
        val fullDraft = ConfigurationDraft(
            pollingSeconds = config.pollingSeconds.toString(),
            tribunes = config.tribunes.map { existing ->
                if (existing.id == tribune.id) draft else TribuneDraft.from(existing)
            },
        )
        val tribuneErrors = fullDraft.validate().tribuneErrors[tribune.id].orEmpty()
        errors = tribuneErrors
        if (tribuneErrors.isEmpty()) {
            val updated = draft.toTribune()
            onSave(
                config.copy(
                    tribunes = config.tribunes.map { existing ->
                        if (existing.id == tribune.id) updated else existing
                    },
                ),
            )
        }
    }

    BackHandler { onBack() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            MiaoliTopAppBar(
                title = { Text("Tribune settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = ::save) {
                        Text("Save")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (errors?.isNotEmpty() == true) {
                item {
                    Text(
                        text = "Check the highlighted fields.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item {
                TribuneConfigCard(
                    draft = draft,
                    errors = errors.orEmpty(),
                    onChange = ::updateDraft,
                )
            }
        }
    }
}

@Composable
private fun MissingTribuneScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MiaoliTopAppBar(
                title = { Text("Tribune settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text("Tribune not found.")
        }
    }
}

@Composable
private fun TribuneConfigCard(
    draft: TribuneDraft,
    errors: Map<String, String>,
    onChange: ((TribuneDraft) -> TribuneDraft) -> Unit,
) {
    val parsedColor = parseHexColor(draft.color)
    val previewColor = parsedColor?.let { Color(it) }
        ?: MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        ),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = draft.title.ifBlank { "Untitled tribune" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = draft.title,
                onValueChange = { value -> onChange { it.copy(title = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                supportingText = errorSupportingText(errors["title"]),
                isError = errors["title"] != null,
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.color,
                onValueChange = { value -> onChange { it.copy(color = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Color") },
                placeholder = { Text("#RRGGBB") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            ),
                    )
                },
                supportingText = errorSupportingText(errors["color"]),
                isError = errors["color"] != null,
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.backendUrl,
                onValueChange = { value -> onChange { it.copy(backendUrl = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Backend URL") },
                supportingText = errorSupportingText(errors["backendUrl"]),
                isError = errors["backendUrl"] != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FormatSelector(
                    format = draft.format,
                    onFormatChange = { value -> onChange { it.copy(format = value) } },
                )
            }

            OutlinedTextField(
                value = draft.timeZoneId,
                onValueChange = { value -> onChange { it.copy(timeZoneId = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Time zone") },
                placeholder = { Text("Europe/Paris") },
                supportingText = errorSupportingText(errors["timeZoneId"]),
                isError = errors["timeZoneId"] != null,
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.postUrl,
                onValueChange = { value -> onChange { it.copy(postUrl = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Post URL") },
                placeholder = { Text("https://...") },
                supportingText = errorSupportingText(errors["postUrl"])
                    ?: { Text("Leave blank if read-only") },
                isError = errors["postUrl"] != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = draft.postField,
                onValueChange = { value -> onChange { it.copy(postField = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Post form field") },
                supportingText = errorSupportingText(errors["postField"])
                    ?: { Text("Used when a post URL is set.") },
                isError = errors["postField"] != null,
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.userAgent,
                onValueChange = { value -> onChange { it.copy(userAgent = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("User agent") },
                supportingText = errorSupportingText(errors["userAgent"]),
                isError = errors["userAgent"] != null,
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.cookie,
                onValueChange = { value -> onChange { it.copy(cookie = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cookie") },
                placeholder = { Text("Optional") },
                supportingText = errorSupportingText(errors["cookie"])
                    ?: { Text("Optional, will only be sent to the post URL") },
                isError = errors["cookie"] != null,
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun FormatSelector(
    format: FeedFormat,
    onFormatChange: (FeedFormat) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = format.label,
                modifier = Modifier.weight(1f),
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
            FeedFormat.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onFormatChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val FeedFormat.label: String
    get() = when (this) {
        FeedFormat.TSV -> "TSV"
        FeedFormat.XML_HTML_ESCAPED -> "XML (HTML-escaped)"
    }

private fun pollingErrorMessage(value: String): String? {
    val seconds = value.toIntOrNull()
    return if (
        seconds == null ||
        seconds < TribuneConfig.MIN_POLLING_SECONDS
    ) {
        "Please do not hammer a tribune with an interval less than ${TribuneConfig.MIN_POLLING_SECONDS} seconds!"
    } else {
        null
    }
}

private fun errorSupportingText(message: String?): (@Composable () -> Unit)? =
    message?.let { text -> { Text(text) } }
