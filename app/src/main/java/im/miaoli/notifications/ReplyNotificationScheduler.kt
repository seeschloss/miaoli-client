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

    fun sync(context: Context, config: TribuneConfig) {
        val workManager = WorkManager.getInstance(context)
        ReplyNotifications.syncChannels(context, config)

        if (config.tribunes.none { it.enabled && it.notifyOnAnswers }) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
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
