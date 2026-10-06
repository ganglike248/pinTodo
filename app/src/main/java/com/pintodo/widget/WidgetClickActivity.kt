package com.pintodo.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pintodo.data.SettingsStore
import com.pintodo.data.TodoStore
import com.pintodo.notify.ActionReceiver
import com.pintodo.notify.Actions
import com.pintodo.notify.Sync
import com.pintodo.ui.MainActivity

/**
 * 위젯 스크롤 목록의 클릭을 받는 보이지 않는 화면.
 * 목록 안 항목은 PendingIntent 템플릿 하나만 쓸 수 있어서, 완료·미루기·열기를 여기서 나눠 처리하고 바로 닫는다.
 */
class WidgetClickActivity : Activity() {
    companion object {
        const val DONE = "done"
        const val SNOOZE = "snooze"
        const val OPEN = "open"
        private const val EXTRA_ACTION = "widget_action"

        fun fillIn(action: String, id: Int): Intent =
            Intent().putExtra(EXTRA_ACTION, action).putExtra(ActionReceiver.EXTRA_ID, id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(ActionReceiver.EXTRA_ID, -1)
        when (intent.getStringExtra(EXTRA_ACTION)) {
            DONE -> {
                TodoStore.modify(this, id) { Actions.complete(it, System.currentTimeMillis()) }
                Sync.run(this)
            }
            SNOOZE -> Actions.snooze(this, id, SettingsStore.get(this).quickSnooze)
            OPEN -> startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_TODO_ID, id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
