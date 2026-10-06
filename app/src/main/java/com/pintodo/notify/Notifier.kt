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

    private var channelsReady = false

    fun ensureChannels(context: Context) {
        if (channelsReady) return
        channelsReady = true
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
        val settings = SettingsStore.get(context)
        // 내용이 없어도 언제로 정한 할 일인지 보이도록 일정은 항상 표시
        val schedule = Format.notificationSchedule(todo)
        // 펼치면: 메모 / 체크리스트(최대 8개) / 일정
        val progress = Format.progress(todo)?.let { "체크리스트 $it" }
        val text = todo.memo.ifBlank { progress ?: schedule }
        val checklist = todo.items.take(8).joinToString("\n") { (if (it.done) "☑ " else "☐ ") + it.text } +
            if (todo.items.size > 8) "\n외 ${todo.items.size - 8}개" else ""
        val bigText = listOf(todo.memo, checklist, schedule).filter { it.isNotBlank() }.joinToString("\n")

        // 워치(Wear OS·갤럭시 워치·밴드 앱)는 ongoing 알림을 넘겨받지 않음 →
        // 워치로 보낼 때는 ongoing 없이 deleteIntent 재게시만으로 고정하고, 지운 회차는 휴대폰에만 남김
        val toWear = settings.wearable && todo.wearDismissedKey != todo.alertKey(now)

        val done = Notification.Action.Builder(null, if (todo.isRepeat) "오늘 완료" else "완료",
            action(context, ActionReceiver.ACTION_DONE, todo.id)).build()
        val quickSnooze = Notification.Action.Builder(null, settings.quickSnooze.buttonLabel,
            action(context, ActionReceiver.ACTION_SNOOZE, todo.id)).build()

        val builder = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_pin)
            .setColor((todo.color?.argb ?: 0xFF4A66E8).toInt())
            .setContentTitle(todo.title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(bigText))
            .setOngoing(todo.pinned && !settings.wearable)
            .setLocalOnly(!toWear)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp(context, todo.id))
            .setDeleteIntent(action(context, ActionReceiver.ACTION_DISMISSED, todo.id))
            // 알림 버튼은 최대 3개: 완료 / 자주 쓰는 미루기 / 미루기 선택
            .addAction(done)
            .addAction(quickSnooze)
            .addAction(Notification.Action.Builder(null, "미루기…", snoozeChooser(context, todo.id)).build())
            // 워치에서는 휴대폰 화면을 여는 '미루기…' 없이 바로 처리되는 버튼만
            .extend(Notification.WearableExtender().addAction(done).addAction(quickSnooze))

        // 하루 안에 끝나는 할 일은 머리글에 종료까지 남은 시간을 실시간으로 (예: 종료까지 · 1:59:30)
        val end = todo.windowAt(now)?.end
        val countdown = end != null && end - now < 24 * 60 * 60_000L
        if (countdown) builder.setWhen(end!!).setUsesChronometer(true).setChronometerCountDown(true)
        else builder.setWhen(todo.alertKey(now) ?: now)   // 이번에 뜬 시각 (회차 시작 또는 미루기 종료)
        val snoozedBack = todo.snoozeUntil != null && todo.alertKey(now) == todo.snoozeUntil
        listOfNotNull("미룬 할 일".takeIf { snoozedBack }, "종료까지".takeIf { countdown })
            .takeIf { it.isNotEmpty() }?.let { builder.setSubText(it.joinToString(" · ")) }

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
