package im.miaoli.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import im.miaoli.R
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
    var pollingError by remember(config) { mutableStateOf<ValidationMessage?>(null) }
    var minimalClockQuotes by remember(config) {
        mutableStateOf(config.useMinimalClockQuotes)
    }

    fun save() {
        val error = pollingErrorMessage(pollingSeconds)
        pollingError = error
        if (error == null) {
            onSave(
                config.copy(
                    pollingSeconds = pollingSeconds.toInt(),
                    useMinimalClockQuotes = minimalClockQuotes,
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
                title = { Text(stringResource(R.string.global_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = ::save) {
                        Text(stringResource(R.string.save))
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
                    text = stringResource(R.string.polling),
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
                    label = { Text(stringResource(R.string.refresh_interval_seconds)) },
                    supportingText = errorSupportingText(pollingError),
                    isError = pollingError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.minimal_clock_quotes),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Switch(
                        checked = minimalClockQuotes,
                        onCheckedChange = { minimalClockQuotes = it },
                    )
                }
            }
        }
    }
}

@Composable
fun TribuneConfigurationScreen(
    config: TribuneConfig,
    tribuneId: Long,
    onBack: () -> Unit,
    onSave: (TribuneConfig) -> Unit,
    onLoginCookies: (Long, String) -> Unit,
    isNew: Boolean = false,
) {
    val tribune = if (isNew) {
        Tribune(
            id = tribuneId,
            title = "",
            color = Tribune.DEFAULT_NEW_COLOR,
            backendUrl = "",
            format = FeedFormat.TSV,
            timeZoneId = Tribune.DEFAULT_TIME_ZONE,
            enabled = true,
        )
    } else {
        config.tribunes.firstOrNull { it.id == tribuneId }
    }
    if (tribune == null) {
        MissingTribuneScreen(onBack = onBack)
    } else {
        TribuneConfigurationContent(
            config = config,
            tribune = tribune,
            isNew = isNew,
            onBack = onBack,
            onSave = onSave,
            onLoginCookies = onLoginCookies,
        )
    }
}

@Composable
private fun TribuneConfigurationContent(
    config: TribuneConfig,
    tribune: Tribune,
    isNew: Boolean,
    onBack: () -> Unit,
    onSave: (TribuneConfig) -> Unit,
    onLoginCookies: (Long, String) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var draft by remember(tribune.id) { mutableStateOf(TribuneDraft.from(tribune)) }
    var errors by remember(tribune.id) {
        mutableStateOf<Map<String, ValidationMessage>?>(null)
    }
    var showLogin by remember(tribune.id) { mutableStateOf(false) }

    if (showLogin) {
        TribuneLoginScreen(
            title = stringResource(
                R.string.login_title,
                draft.title.ifBlank { stringResource(R.string.tribune) },
            ),
            loginUrl = draft.loginUrl.trim(),
            cookieUrl = draft.postUrl.trim().takeIf { it.isValidHttpUrl() }
                ?: draft.loginUrl.trim(),
            onCancel = { showLogin = false },
            onCookies = { cookies ->
                draft = draft.copy(cookie = cookies)
                errors = null
                if (!isNew) {
                    onLoginCookies(tribune.id, cookies)
                }
                showLogin = false
                Toast.makeText(
                    context,
                    if (isNew) {
                        resources.getString(R.string.cookies_captured_new)
                    } else {
                        resources.getString(R.string.cookies_saved)
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
        return
    }

    fun updateDraft(transform: (TribuneDraft) -> TribuneDraft) {
        draft = transform(draft)
        errors = null
    }

    fun save() {
        val existingDrafts = config.tribunes.map { existing ->
            if (!isNew && existing.id == tribune.id) {
                draft
            } else {
                TribuneDraft.from(existing)
            }
        }
        val fullDraft = ConfigurationDraft(
            pollingSeconds = config.pollingSeconds.toString(),
            tribunes = if (isNew) existingDrafts + draft else existingDrafts,
        )
        val tribuneErrors = fullDraft.validate().tribuneErrors[tribune.id].orEmpty()
        errors = tribuneErrors
        if (tribuneErrors.isEmpty()) {
            val updated = draft.toTribune()
            onSave(
                config.copy(
                    tribunes = if (isNew) {
                        config.tribunes + updated
                    } else {
                        config.tribunes.map { existing ->
                            if (existing.id == tribune.id) updated else existing
                        }
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
                title = {
                    Text(
                        stringResource(
                            if (isNew) R.string.new_tribune else R.string.tribune_settings,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = ::save) {
                        Text(stringResource(R.string.save))
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
                        text = stringResource(R.string.check_highlighted_fields),
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
                    onLogin = { showLogin = true },
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
                title = { Text(stringResource(R.string.tribune_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
            Text(stringResource(R.string.tribune_not_found))
        }
    }
}

@Composable
private fun TribuneConfigCard(
    draft: TribuneDraft,
    errors: Map<String, ValidationMessage>,
    onChange: ((TribuneDraft) -> TribuneDraft) -> Unit,
    onLogin: () -> Unit,
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            onChange { it.copy(notifyOnAnswers = false) }
        }
    }
    val parsedColor = parseHexColor(draft.color)
    val previewColor = parsedColor?.let { Color(it) }
        ?: MaterialTheme.colorScheme.outlineVariant

    fun setNotificationsEnabled(enabled: Boolean) {
        onChange { it.copy(notifyOnAnswers = enabled) }
        if (enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
                    text = draft.title.ifBlank {
                        stringResource(R.string.untitled_tribune)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = draft.title,
                onValueChange = { value -> onChange { it.copy(title = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.title_label)) },
                supportingText = errorSupportingText(errors["title"]),
                isError = errors["title"] != null,
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.color,
                onValueChange = { value -> onChange { it.copy(color = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.color_label)) },
                placeholder = { Text(stringResource(R.string.color_placeholder)) },
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
                label = { Text(stringResource(R.string.backend_url_label)) },
                supportingText = errorSupportingText(errors["backendUrl"]),
                isError = errors["backendUrl"] != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.format_label),
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
                label = { Text(stringResource(R.string.time_zone_label)) },
                placeholder = { Text(stringResource(R.string.time_zone_placeholder)) },
                supportingText = errorSupportingText(errors["timeZoneId"]),
                isError = errors["timeZoneId"] != null,
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.postUrl,
                onValueChange = { value -> onChange { it.copy(postUrl = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.post_url_label)) },
                placeholder = { Text(stringResource(R.string.url_placeholder)) },
                supportingText = errorSupportingText(errors["postUrl"])
                    ?: { Text(stringResource(R.string.post_url_supporting)) },
                isError = errors["postUrl"] != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = draft.postField,
                onValueChange = { value -> onChange { it.copy(postField = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.post_form_field_label)) },
                supportingText = errorSupportingText(errors["postField"])
                    ?: { Text(stringResource(R.string.post_form_field_supporting)) },
                isError = errors["postField"] != null,
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.loginUrl,
                onValueChange = { value -> onChange { it.copy(loginUrl = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.login_url_label)) },
                placeholder = { Text(stringResource(R.string.url_placeholder)) },
                supportingText = errorSupportingText(errors["loginUrl"])
                    ?: { Text(stringResource(R.string.login_url_supporting)) },
                isError = errors["loginUrl"] != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedButton(
                onClick = onLogin,
                enabled = draft.loginUrl.trim().isValidHttpUrl(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.log_in))
            }

            OutlinedTextField(
                value = draft.userAgent,
                onValueChange = { value -> onChange { it.copy(userAgent = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.user_agent_label)) },
                supportingText = errorSupportingText(errors["userAgent"]),
                isError = errors["userAgent"] != null,
                singleLine = true,
            )

            OutlinedTextField(
                value = draft.username,
                onValueChange = { value -> onChange { it.copy(username = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.username_label)) },
                placeholder = { Text(stringResource(R.string.optional)) },
                supportingText = {
                    Text(stringResource(R.string.username_supporting))
                },
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.send_notification_on_answers),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = draft.notifyOnAnswers,
                    onCheckedChange = ::setNotificationsEnabled,
                )
            }

            OutlinedTextField(
                value = draft.cookie,
                onValueChange = { value -> onChange { it.copy(cookie = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.cookie_label)) },
                placeholder = { Text(stringResource(R.string.optional)) },
                supportingText = errorSupportingText(errors["cookie"])
                    ?: { Text(stringResource(R.string.cookie_supporting)) },
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
                text = stringResource(format.labelRes()),
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
                    text = { Text(stringResource(option.labelRes())) },
                    onClick = {
                        onFormatChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@StringRes
private fun FeedFormat.labelRes(): Int = when (this) {
    FeedFormat.TSV -> R.string.format_tsv
    FeedFormat.XML_HTML_ESCAPED -> R.string.format_xml_html_escaped
    FeedFormat.XML_RAW -> R.string.format_xml_raw
}

private fun pollingErrorMessage(value: String): ValidationMessage? {
    val seconds = value.toIntOrNull()
    return if (
        seconds == null ||
        seconds < TribuneConfig.MIN_POLLING_SECONDS
    ) {
        ValidationMessage(
            resourceId = R.string.polling_interval_error,
            formatArgs = listOf(TribuneConfig.MIN_POLLING_SECONDS),
        )
    } else {
        null
    }
}

@Composable
private fun errorSupportingText(
    message: ValidationMessage?,
): (@Composable () -> Unit)? = message?.let { error ->
    {
        Text(
            stringResource(
                error.resourceId,
                *error.formatArgs.toTypedArray(),
            ),
        )
    }
}
