package com.ganglike.pintodo.notify

import android.content.Context
import com.ganglike.pintodo.data.SnoozeOption
import com.ganglike.pintodo.data.Todo
import com.ganglike.pintodo.data.TodoStore

/** 화면과 알림 버튼이 함께 쓰는 동작 */
object Actions {
    /** 한 번짜리는 완료 처리, 반복은 이번 회차만 숨김 */
    fun complete(todo: Todo, now: Long): Todo =
        if (todo.isRepeat) todo.copy(hiddenKey = todo.windowAt(now)?.start, snoozeUntil = null)
        else todo.copy(doneAt = now, snoozeUntil = null)

    /** 미루기. 선택지가 이미 지난 시각(예: 저녁 6시 이후의 '오늘 저녁')이면 무시 */
    fun snooze(context: Context, id: Int, option: SnoozeOption) {
        val until = option.until(System.currentTimeMillis()) ?: return
        TodoStore.modify(context, id) { it.copy(snoozeUntil = until) }
        Sync.run(context)
    }

    fun save(context: Context, todo: Todo) {
        TodoStore.upsert(context, todo)
        Sync.run(context)
    }

    fun delete(context: Context, id: Int) {
        TodoStore.remove(context, id)
        Notifier.cancel(context, id)
        Sync.cancelAlarm(context, id)
        Sync.run(context)
    }
}
