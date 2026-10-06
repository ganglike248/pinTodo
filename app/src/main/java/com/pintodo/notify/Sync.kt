package com.pintodo.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pintodo.data.Status
import com.pintodo.data.TodoStore
import com.pintodo.tile.QuickAddTileService
import com.pintodo.widget.TodoWidget

/**
 * 저장된 할 일과 실제 알림/알람 상태를 맞춘다.
 * 데이터가 바뀌거나 예약 시각이 되거나 알림이 지워질 때마다 이 함수 하나만 호출하면 된다.
 */
object Sync {
    @Synchronized
    fun run(context: Context) {
        val now = System.currentTimeMillis()
        Notifier.ensureChannels(context)

        val todos = TodoStore.all(context)
        val alerted = mutableMapOf<Int, Long>()

        for (todo in todos) {
            if (todo.status(now) == Status.SHOWING) {
                val key = todo.alertKey(now)
                val alert = key != null && key != todo.alertedKey
                // 이미 떠 있는 알림의 갱신은 setOnlyAlertOnce 때문에 울리지 않으므로 새로 게시
                if (alert) Notifier.cancel(context, todo.id)
                Notifier.show(context, todo, alert, now)
                if (alert) alerted[todo.id] = key!!
            } else {
                Notifier.cancel(context, todo.id)
            }
            scheduleAlarm(context, todo.id, todo.nextChange(now))
        }

        // 삭제된 할 일의 잔여 알림 정리
        val ids = todos.map { it.id }.toSet()
        Notifier.activeIds(context).filter { it !in ids }.forEach { Notifier.cancel(context, it) }

        if (alerted.isNotEmpty()) {
            TodoStore.update(context) { list ->
                list.map { t -> alerted[t.id]?.let { t.copy(alertedKey = it) } ?: t }
            }
        }

        // 홈 화면 위젯과 빠른 설정 타일도 최신 상태로
        TodoWidget.refresh(context)
        QuickAddTileService.refresh(context)
    }

    fun cancelAlarm(context: Context, id: Int) = scheduleAlarm(context, id, null)

    private fun scheduleAlarm(context: Context, id: Int, at: Long?) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, id,
            Intent(context, AlarmReceiver::class.java).putExtra(ActionReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        when {
            at == null -> am.cancel(pi)
            canExact(am) -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else -> am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun canExact(am: AlarmManager) =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    /** '알람 및 리마인더' 권한. 없으면 알람이 몇 분 늦게 울릴 수 있음 */
    fun canExact(context: Context) = canExact(context.getSystemService(AlarmManager::class.java))
}
