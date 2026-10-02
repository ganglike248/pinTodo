package com.pintodo.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.pintodo.R
import com.pintodo.data.AlertMode
import com.pintodo.data.SettingsStore
import com.pintodo.data.Todo
import com.pintodo.ui.MainActivity
import com.pintodo.ui.SnoozeActivity
import com.pintodo.ui.Format

/**
 * 알림 채널 구성
 * - 처음 뜰 때: 알림 방식(소리/진동/둘 다)에 맞는 HIGH 채널 → 1회 울림 + 팝업
 * - 다시 게시(스와이프 후 복원, 앱 실행 시 복원 등): QUIET 채널 → 울리지 않음
 */
object Notifier {
    private const val CH_QUIET = "quiet"
    private const val CH_SOUND = "alert_sound"
    private const val CH_VIBRATE = "alert_vibrate"
    private const val CH_BOTH = "alert_both"
    private val VIBRATION = longArrayOf(0, 250, 150, 250)

    fun ensureChannels(context: Context) {
        val nm = manager(context)
        nm.deleteNotificationChannel("pinned_todos") // v1 채널 정리

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        nm.createNotificationChannels(listOf(
            NotificationChannel(CH_QUIET, "표시 중인 할 일", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "알림창에 조용히 떠 있는 할 일 (소리·진동 없음)"
                setSound(null, null)
                enableVibration(false)
            },
            NotificationChannel(CH_SOUND, "새 알림 · 소리", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "할 일이 처음 뜰 때 소리로 알림"
                setSound(sound, attrs)
                enableVibration(false)
            },
            NotificationChannel(CH_VIBRATE, "새 알림 · 진동", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "할 일이 처음 뜰 때 진동으로 알림"
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = VIBRATION
            },
            NotificationChannel(CH_BOTH, "새 알림 · 소리+진동", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "할 일이 처음 뜰 때 소리와 진동으로 알림"
                setSound(sound, attrs)
                enableVibration(true)
                vibrationPattern = VIBRATION
            },
        ))
    }

    fun show(context: Context, todo: Todo, alert: Boolean, now: Long) {
        val channel = if (!alert) CH_QUIET else when (todo.alertMode) {
            AlertMode.SILENT -> CH_QUIET
            AlertMode.SOUND -> CH_SOUND
            AlertMode.VIBRATE -> CH_VIBRATE
            AlertMode.BOTH -> CH_BOTH
        }
        val quick = SettingsStore.get(context).quickSnooze
        val schedule = Format.notificationSchedule(todo, now)
        val body = todo.memo.ifBlank { schedule }

        val builder = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_pin)
            .setColor(0xFF4A66E8.toInt())
            .setContentTitle(todo.title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setOngoing(todo.pinned)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp(context, todo.id))
            .setDeleteIntent(action(context, ActionReceiver.ACTION_DISMISSED, todo.id))
            // 알림 버튼은 최대 3개: 완료 / 자주 쓰는 미루기 / 미루기 선택
            .addAction(Notification.Action.Builder(null, if (todo.isRepeat) "오늘 완료" else "완료",
                action(context, ActionReceiver.ACTION_DONE, todo.id)).build())
            .addAction(Notification.Action.Builder(null, quick.buttonLabel,
                action(context, ActionReceiver.ACTION_SNOOZE, todo.id)).build())
            .addAction(Notification.Action.Builder(null, "미루기…", snoozeChooser(context, todo.id)).build())
        if (todo.memo.isNotBlank() && schedule.isNotEmpty()) builder.setSubText(schedule)

        manager(context).notify(todo.id, builder.build())
    }

    fun cancel(context: Context, id: Int) = manager(context).cancel(id)

    // tag가 있는 것은 시스템이 만든 자동 그룹 요약이므로 제외
    fun activeIds(context: Context): Set<Int> =
        manager(context).activeNotifications.filter { it.tag == null }.map { it.id }.toSet()

    fun enabled(context: Context) = manager(context).areNotificationsEnabled()

    private fun openApp(context: Context, id: Int) = PendingIntent.getActivity(
        context, id,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TODO_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun snoozeChooser(context: Context, id: Int) = PendingIntent.getActivity(
        context, id,
        Intent(context, SnoozeActivity::class.java)
            .putExtra(ActionReceiver.EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun action(context: Context, action: String, id: Int): PendingIntent {
        val intent = Intent(context, ActionReceiver::class.java)
            .setAction(action)
            .putExtra(ActionReceiver.EXTRA_ID, id)
        // action이 달라 Intent가 구분되므로 requestCode는 id만으로 충분
        return PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun manager(context: Context) =
        context.getSystemService(NotificationManager::class.java)
}
