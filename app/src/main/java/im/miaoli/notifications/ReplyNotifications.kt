package im.miaoli.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.RemoteViews
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import im.miaoli.MainActivity
import im.miaoli.R
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig
import im.miaoli.ui.clockReferences
import im.miaoli.ui.totozReferences

internal const val ACTION_OPEN_REPLY = "im.miaoli.action.OPEN_REPLY"
internal const val EXTRA_TRIBUNE_ID = "im.miaoli.extra.TRIBUNE_ID"
internal const val EXTRA_CLOCK = "im.miaoli.extra.CLOCK"
internal const val EXTRA_POST_KEY = "im.miaoli.extra.POST_KEY"

internal data class NotificationQuote(
    val tribuneId: Long,
    val clock: String,
    val postKey: String?,
)

internal fun notificationQuoteFromIntent(intent: Intent?): NotificationQuote? {
    if (intent?.action != ACTION_OPEN_REPLY) return null
    val tribuneId = intent.getLongExtra(EXTRA_TRIBUNE_ID, -1L)
    val clock = intent.getStringExtra(EXTRA_CLOCK)?.trim().orEmpty()
    val postKey = intent.getStringExtra(EXTRA_POST_KEY)?.trim()?.takeIf { it.isNotEmpty() }
    if (tribuneId <= 0L || clock.isEmpty()) return null
    return NotificationQuote(tribuneId = tribuneId, clock = clock, postKey = postKey)
}

/** Reads the notification quote once and removes it from the intent. */
internal fun consumeNotificationQuote(intent: Intent?): NotificationQuote? {
    val quote = notificationQuoteFromIntent(intent) ?: return null
    intent?.action = null
    intent?.removeExtra(EXTRA_TRIBUNE_ID)
    intent?.removeExtra(EXTRA_CLOCK)
    intent?.removeExtra(EXTRA_POST_KEY)
    return quote
}

/** Uses the login when available; otherwise truncates the feed's raw info field. */
internal fun Post.notificationAuthor(): String {
    val username = login.trim()
    if (username.isNotEmpty()) return username
    val info = authorInfo.trim().ifEmpty { author.trim() }
    return info.take(12)
}

private data class NotificationColors(
    val accent: Int,
    val postClock: Int,
    val foreground: Int,
)

private fun notificationColors(context: Context): NotificationColors {
    val night = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    return if (night) {
        NotificationColors(
            accent = 0xFF90CAF9.toInt(),
            postClock = 0xFFFFB74D.toInt(),
            foreground = 0xFFE6E6E6.toInt(),
        )
    } else {
        NotificationColors(
            accent = 0xFF1976D2.toInt(),
            postClock = 0xFF8A4B00.toInt(),
            foreground = 0xFF1A1A1A.toInt(),
        )
    }
}

private fun buildNotificationText(
    context: Context,
    reply: Post,
    accentColor: Int,
    postClockColor: Int,
): Spanned {
    val message = styledNotificationMessage(reply.message, accentColor)
    val builder = SpannableStringBuilder()
    val clockStart = builder.length
    builder.append(reply.clock)
    builder.setSpan(
        ForegroundColorSpan(postClockColor),
        clockStart,
        builder.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    builder.append(context.getString(R.string.notification_answer_separator))
    builder.append(message)
    return builder
}

private fun styledNotificationMessage(message: String, accentColor: Int): Spanned {
    val parsed = runCatching {
        Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)
    }.getOrElse {
        SpannableStringBuilder(message)
    }
    val styled = SpannableStringBuilder(parsed)
    styled.getSpans(0, styled.length, URLSpan::class.java).forEach { span ->
        val start = styled.getSpanStart(span)
        val end = styled.getSpanEnd(span)
        if (start >= 0 && end > start) {
            styled.setSpan(
                ForegroundColorSpan(accentColor),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
    val visibleText = styled.toString()
    clockReferences(visibleText).forEach { reference ->
        styled.setSpan(
            ForegroundColorSpan(accentColor),
            reference.start,
            reference.end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    totozReferences(visibleText).forEach { reference ->
        styled.setSpan(
            ForegroundColorSpan(accentColor),
            reference.start,
            reference.end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    return styled
}

internal object ReplyNotifications {
    private const val CHANNEL_PREFIX = "tribune_answers_"

    fun syncChannels(context: Context, config: TribuneConfig) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val enabled = config.tribunes.filter { it.enabled }
        val expectedIds = enabled.mapTo(mutableSetOf()) { channelId(it.id) }

        enabled.forEach { tribune ->
            manager.createNotificationChannel(channelFor(tribune))
        }
        manager.notificationChannels
            .filter { channel ->
                channel.id.startsWith(CHANNEL_PREFIX) && channel.id !in expectedIds
            }
            .forEach { channel ->
                manager.deleteNotificationChannel(channel.id)
            }
    }

    fun show(context: Context, notification: ReplyNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notificationChannelId = channelId(notification.reply.tribune.id)
        if (manager.getNotificationChannel(notificationChannelId) == null) {
            manager.createNotificationChannel(channelFor(notification.reply.tribune))
        }

        val reply = notification.reply
        val title = context.getString(
            R.string.notification_answer_title,
            reply.tribune.title,
            reply.notificationAuthor(),
        )
        val colors = notificationColors(context)
        val text = buildNotificationText(
            context = context,
            reply = reply,
            accentColor = colors.accent,
            postClockColor = colors.postClock,
        )
        val customContent = RemoteViews(
            context.packageName,
            R.layout.notification_answer,
        ).apply {
            setTextViewText(R.id.notification_answer_title, title)
            setTextViewText(R.id.notification_answer_body, text)
            setTextColor(R.id.notification_answer_title, colors.foreground)
            setTextColor(R.id.notification_answer_body, colors.foreground)
        }
        val builder = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher),
            )
            .setContentTitle(title)
            .setContentText(text)
            .setCustomContentView(customContent)
            .setCustomBigContentView(customContent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(replyPendingIntent(context, reply))

        NotificationManagerCompat.from(context).notify(
            reply.key.hashCode(),
            builder.build(),
        )
    }

    private fun channelFor(tribune: Tribune): NotificationChannel =
        NotificationChannel(
            channelId(tribune.id),
            tribune.title,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setShowBadge(true)
        }

    private fun channelId(tribuneId: Long): String = "$CHANNEL_PREFIX$tribuneId"

    private fun replyPendingIntent(context: Context, reply: Post): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_REPLY
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TRIBUNE_ID, reply.tribune.id)
            putExtra(EXTRA_CLOCK, reply.clock)
            putExtra(EXTRA_POST_KEY, reply.key)
        }
        return PendingIntent.getActivity(
            context,
            reply.key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
