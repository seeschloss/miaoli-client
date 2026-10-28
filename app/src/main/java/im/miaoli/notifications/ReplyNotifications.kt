package im.miaoli.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import im.miaoli.MainActivity
import im.miaoli.R
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig
import im.miaoli.ui.parseMessage

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
        val text = context.getString(
            R.string.notification_answer_text,
            reply.clock,
            parseMessage(reply.message).text.trim(),
        )
        val builder = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher),
            )
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
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
