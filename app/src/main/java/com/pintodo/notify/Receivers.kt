package com.pintodo.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pintodo.data.SettingsStore
import com.pintodo.data.Status
import com.pintodo.data.TodoStore

/** 알림의 버튼과 스와이프 삭제 처리 */
class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DONE = "com.pintodo.DONE"
        const val ACTION_SNOOZE = "com.pintodo.SNOOZE"
        const val ACTION_DISMISSED = "com.pintodo.DISMISSED"
        const val EXTRA_ID = "id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        val now = System.currentTimeMillis()
        val todo = TodoStore.get(context, id) ?: run { Notifier.cancel(context, id); return }

        when (intent.action) {
            ACTION_DONE -> TodoStore.modify(context, id) { Actions.complete(it, now) }
            ACTION_SNOOZE -> Actions.snooze(context, id, SettingsStore.get(context).quickSnooze)
            ACTION_DISMISSED -> {
                if (todo.pinned) {
                    // 고정: 이 알림만 조용히 다시 게시 (그룹째 지우면 알림마다 호출되므로 전체 Sync는 생략)
                    if (todo.status(now) == Status.SHOWING) Notifier.show(context, todo, alert = false, now = now)
                    return
                }
                // 고정 안 함: 이번 회차는 닫은 것으로 처리
                val key = todo.windowAt(now)?.start
                TodoStore.modify(context, id) { it.copy(hiddenKey = key) }
            }
        }
        Sync.run(context)
    }
}

/** 예약 시각(시작/종료/미루기 끝) 도달 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Sync.run(context)
}

/** 재부팅, 앱 업데이트, 시간/시간대 변경 */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Sync.run(context)
}
