package im.miaoli.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import im.miaoli.data.FeedRepository
import im.miaoli.data.PostMerger
import im.miaoli.data.SharedPreferencesTribuneConfigStore
import kotlinx.coroutines.CancellationException

class ReplyCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val config = SharedPreferencesTribuneConfigStore(applicationContext)
            .load()
            .normalized()
        val feedTribunes = config.feedTribunes
        if (feedTribunes.none { it.notifyOnAnswers }) return Result.success()

        return try {
            val result = FeedRepository().fetch(feedTribunes)
            if (result.posts.isEmpty() && result.errors.isNotEmpty()) {
                return Result.retry()
            }

            val posts = PostMerger.merge(emptyList(), result.posts)
            val stateStore = SharedPreferencesNotificationStateStore(applicationContext)
            val scan = scanForReplyNotifications(
                posts = posts,
                tribunes = config.tribunes,
                previousStates = stateStore.load(),
            )
            stateStore.save(scan.states)
            ReplyNotifications.syncChannels(applicationContext, config)
            scan.notifications.forEach { notification ->
                ReplyNotifications.show(applicationContext, notification)
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.retry()
        }
    }
}
