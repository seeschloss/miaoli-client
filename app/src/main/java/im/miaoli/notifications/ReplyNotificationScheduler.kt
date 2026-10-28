package im.miaoli.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import im.miaoli.data.TribuneConfig
import java.util.concurrent.TimeUnit

internal object ReplyNotificationScheduler {
    private const val WORK_NAME = "reply_notifications"
    private const val CHECK_INTERVAL_MINUTES = 15L

    fun sync(
        context: Context,
        config: TribuneConfig,
        newlyEnabledTribuneIds: Set<Long> = emptySet(),
    ) {
        val workManager = WorkManager.getInstance(context)
        ReplyNotifications.syncChannels(context, config)

        val notificationTribuneIds = config.tribunes
            .filter { it.enabled && it.notifyOnAnswers }
            .mapTo(mutableSetOf()) { it.id }
        if (notificationTribuneIds.isEmpty()) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        // Establish the "notifications started at" time right away. This lets
        // the first background scan report answers received between enabling
        // notifications and the first periodic run.
        val stateStore = SharedPreferencesNotificationStateStore(context)
        val states = stateStore.load()
        val idsToStart = newlyEnabledTribuneIds + notificationTribuneIds.filter { it !in states }
        if (idsToStart.isNotEmpty()) {
            stateStore.markEnabled(idsToStart, System.currentTimeMillis())
        }

        val request = PeriodicWorkRequestBuilder<ReplyCheckWorker>(
            CHECK_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
