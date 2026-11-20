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
 *   무음도 HIGH(소리·진동 없음) 채널로 팝업만 → 알림이 여러 개 겹쳐 있어도 새로 뜬 걸 알아챔
 * - 다시 게시(스와이프 후 복원, 앱 실행 시 복원 등): QUIET 채널 → 울리지 않음
 */
object Notifier {
    private const val CH_QUIET = "quiet"
    // 할 일 알림은 앱이 직접 묶음: 최근에 뜬 것이 맨 위, 머리글에 '할 일 3개 · 신규 1'
    private const val GROUP = "com.pintodo.todos"
    private const val SUMMARY_TAG = "summary"
    private const val SUMMARY_ID = 0
    private const val CH_SOUND = "alert_sound"
    private const val CH_VIBRATE = "alert_vibrate"
    private const val CH_BOTH = "alert_both"
    private const val CH_POPUP = "alert_popup"
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
            NotificationChannel(CH_POPUP, "새 알림 · 무음", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "무음으로 정한 할 일이 처음 뜰 때 소리·진동 없이 화면 위에 잠깐 보여 줌"
                setSound(null, null)
                enableVibration(false)
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
            // 무음: 처음 뜰 때(회차 시작·미루기 끝)만 소리 없이 팝업, 다시 울리기 간격에는 조용히 (편집 화면 안내와 같게)
            AlertMode.SILENT -> if (todo.isReminder(now)) CH_QUIET else CH_POPUP
            AlertMode.SOUND -> CH_SOUND
            AlertMode.VIBRATE -> CH_VIBRATE
            AlertMode.BOTH -> CH_BOTH
        }
        val settings = SettingsStore.get(context)
        val fresh = todo.isNew(now)
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
            // 뜬 지 30분 안이면 '신규' (겹쳐 있는 알림 중 방금 뜬 것을 알아보게, 시간이 지나면 Sync가 다시 그려 지움)
            .setContentTitle(if (fresh) "신규 · ${todo.title}" else todo.title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(bigText))
            .setOngoing(todo.pinned && !settings.wearable)
            .setLocalOnly(!toWear)
            // 잠금화면: 설정을 켜면 '민감한 내용 숨기기'여도 내용을 보여 주고, 아니면 내용 없이 '할 일 알림'만
            .setVisibility(if (settings.lockScreenContent) Notification.VISIBILITY_PUBLIC else Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(
                Notification.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_pin)
                    .setColor((todo.color?.argb ?: 0xFF4A66E8).toInt())
                    .setContentTitle("할 일 알림")
                    .setContentText("잠금을 풀면 내용을 볼 수 있어요")
                    .build()
            )
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            // 묶음 안에서 최근에 뜬 것이 위로 (sortKey는 글자 순 오름차순이라 뜬 시각을 거꾸로 셈)
            .setGroup(GROUP)
            .setSortKey(sortKey(todo.shownAt(now) ?: now))
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
        // 이번에 뜬 시각 (회차 시작 또는 미루기 종료). 너무 오래된 시각이면 넣지 않음 —
        // Android 16은 앱이 지정한 when이 2주보다 오래된 알림의 게시를 무시해서, 오래 고정한 할 일이 재게시 때 사라졌음
        else todo.shownSince(now)?.let { builder.setWhen(it) } ?: builder.setShowWhen(false)
        val snoozedBack = todo.snoozeUntil != null && todo.alertKey(now) == todo.snoozeUntil
        listOfNotNull("미룬 할 일".takeIf { snoozedBack }, "종료까지".takeIf { countdown })
            .takeIf { it.isNotEmpty() }?.let { builder.setSubText(it.joinToString(" · ")) }

        manager(context).notify(todo.id, builder.build())
    }

    fun cancel(context: Context, id: Int) = manager(context).cancel(id)

    /** 주의: 요약을 지우면 묶음 안 알림도 모두 지워짐 → 할 일 알림을 게시하기 전에 부를 것 ([Sync.run]) */
    fun cancelSummary(context: Context) = manager(context).cancel(SUMMARY_TAG, SUMMARY_ID)

    private fun sortKey(shownAt: Long) = "%019d".format(Long.MAX_VALUE - shownAt)

    /**
     * 할 일 알림 묶음의 요약. 2개 이상 떠 있을 때만 (1개면 그대로 한 장, 그때 지우는 건 [cancelSummary]).
     * 머리글에 '할 일 3개 · 신규 1', 접었을 때도 최근 것부터 제목을 보여 줌.
     * 소리는 내지 않고(자식 알림만 울림), 밀어서 지우면 [ActionReceiver.ACTION_SUMMARY_DISMISSED]로 다시 게시
     */
    fun showSummary(context: Context, shown: List<Todo>, now: Long) {
        val settings = SettingsStore.get(context)
        val recent = shown.sortedByDescending { it.shownAt(now) ?: 0L }
        val fresh = recent.count { it.isNew(now) }
        val header = "할 일 ${shown.size}개" + if (fresh > 0) " · 신규 $fresh" else ""
        val lines = recent.map { (if (it.isNew(now)) "신규 · " else "") + it.title }
        val style = Notification.InboxStyle().setSummaryText(header)
        lines.take(6).forEach { style.addLine(it) }
        val n = Notification.Builder(context, CH_QUIET)
            .setSmallIcon(R.drawable.ic_pin)
            .setColor(0xFF4A66E8.toInt())
            .setContentTitle(header)
            .setContentText(lines.joinToString(", "))
            .setSubText(header)
            .setStyle(style)
            .setGroup(GROUP)
            .setGroupSummary(true)
            .setGroupAlertBehavior(Notification.GROUP_ALERT_CHILDREN)
            .setOnlyAlertOnce(true)
            // 고정(워치로 안 보냄) 할 일이 하나라도 있으면 요약도 고정
            .setOngoing(!settings.wearable && shown.any { it.pinned })
            .setLocalOnly(!settings.wearable)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setWhen(recent.first().shownSince(now) ?: now)
            .setShowWhen(true)
            .setContentIntent(PendingIntent.getActivity(
                context, SUMMARY_ID, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ))
            .setDeleteIntent(PendingIntent.getBroadcast(
                context, SUMMARY_ID, Intent(context, ActionReceiver::class.java).setAction(ActionReceiver.ACTION_SUMMARY_DISMISSED),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ))
            .build()
        manager(context).notify(SUMMARY_TAG, SUMMARY_ID, n)
    }


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
