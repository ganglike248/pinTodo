package com.ganglike.pintodo.notify

import android.content.Context
import com.ganglike.pintodo.data.Todo
import com.ganglike.pintodo.data.TodoStore

/** 화면과 알림 버튼이 함께 쓰는 동작 */
object Actions {
    /** 한 번짜리는 완료 처리, 반복은 이번 회차만 숨김 */
    fun complete(todo: Todo, now: Long): Todo =
        if (todo.isRepeat) todo.copy(hiddenKey = todo.windowAt(now)?.start, snoozeUntil = null)
        else todo.copy(doneAt = now, snoozeUntil = null)

    fun save(context: Context, todo: Todo) {
        TodoStore.upsert(context, todo)
        Sync.run(context)
    }

    fun delete(context: Context, id: Int) {
        TodoStore.remove(context, id)
        Notifier.cancel(context, id)
        Sync.cancelAlarm(context, id)
    }
}
