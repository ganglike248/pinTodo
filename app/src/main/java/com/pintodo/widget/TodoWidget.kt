package com.pintodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.pintodo.R
import com.pintodo.data.Status
import com.pintodo.data.Todo
import com.pintodo.data.TodoStore
import com.pintodo.notify.ActionReceiver
import com.pintodo.ui.Format
import com.pintodo.ui.MainActivity
import com.pintodo.ui.QuickAddActivity

/**
 * 홈 화면 위젯: 진행 중·알림 없는·예정인 할 일 목록.
 *
 * RemoteViews로 직접 그려서 AppWidgetManager에 바로 반영한다.
 * (Glance는 WorkManager를 거쳐 그려서 삼성 기기에서 갱신이 늦거나 누락되는 문제가 있었음)
 */
object TodoWidget {
    private const val MAX_ROWS = 8
    private const val REQ_OPEN_APP = 100_000
    private const val REQ_QUICK_ADD = 100_001

    fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TodoWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, build(context))
    }

    private fun build(context: Context): RemoteViews {
        val now = System.currentTimeMillis()
        val order = listOf(Status.SHOWING, Status.SNOOZED, Status.NO_ALERT, Status.SCHEDULED)
        val items = TodoStore.all(context)
            .filter { it.status(now) in order }
            .sortedWith(compareBy<Todo>({ order.indexOf(it.status(now)) }, { it.nextStart(now) ?: 0L }))
        val showing = items.count { it.status(now) == Status.SHOWING }

        val views = RemoteViews(context.packageName, R.layout.widget_todo)
        views.setOnClickPendingIntent(R.id.widget_root, activity(context, REQ_OPEN_APP, Intent(context, MainActivity::class.java)))
        views.setOnClickPendingIntent(R.id.add, activity(context, REQ_QUICK_ADD, Intent(context, QuickAddActivity::class.java)))
        views.setTextViewText(
            R.id.subtitle,
            if (items.isEmpty()) "할 일이 없어요" else "알림 중 ${showing}개 · 전체 ${items.size}개",
        )
        views.setViewVisibility(R.id.empty, if (items.isEmpty()) View.VISIBLE else View.GONE)

        views.removeAllViews(R.id.rows)
        items.take(MAX_ROWS).forEach { views.addView(R.id.rows, row(context, it, now)) }
        val rest = items.size - MAX_ROWS
        views.setViewVisibility(R.id.more, if (rest > 0) View.VISIBLE else View.GONE)
        if (rest > 0) views.setTextViewText(R.id.more, "외 ${rest}개 더")
        return views
    }

    private fun row(context: Context, todo: Todo, now: Long): RemoteViews {
        val status = todo.status(now)
        val canComplete = status != Status.SCHEDULED
        val v = RemoteViews(context.packageName, R.layout.widget_row)
        v.setTextViewText(R.id.row_title, todo.title)

        // 아이콘: 알림 중=파란 원, 미룸·알림 없음=회색 원, 예정=시계
        v.setViewVisibility(R.id.row_check_on, visibleIf(status == Status.SHOWING))
        v.setViewVisibility(R.id.row_check_off, visibleIf(status == Status.SNOOZED || status == Status.NO_ALERT))
        v.setViewVisibility(R.id.row_sched, visibleIf(status == Status.SCHEDULED))

        // 상태 문구: 색별로 준비된 TextView 중 하나만 보이게 (야간 모드 색은 리소스가 처리)
        val statusView = when (status) {
            Status.SHOWING -> R.id.row_status_primary
            Status.SNOOZED -> R.id.row_status_orange
            else -> R.id.row_status_grey
        }
        listOf(R.id.row_status_primary, R.id.row_status_orange, R.id.row_status_grey).forEach {
            v.setViewVisibility(it, visibleIf(it == statusView))
        }
        v.setTextViewText(statusView, Format.status(todo, now))

        if (canComplete) {
            val done = Intent(context, ActionReceiver::class.java)
                .setAction(ActionReceiver.ACTION_DONE)
                .putExtra(ActionReceiver.EXTRA_ID, todo.id)
            v.setOnClickPendingIntent(
                R.id.row_check,
                PendingIntent.getBroadcast(context, todo.id, done, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }
        v.setOnClickPendingIntent(
            R.id.row_body,
            activity(context, todo.id, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TODO_ID, todo.id)),
        )
        return v
    }

    private fun activity(context: Context, requestCode: Int, intent: Intent) = PendingIntent.getActivity(
        context, requestCode,
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun visibleIf(b: Boolean) = if (b) View.VISIBLE else View.GONE
}

/** 위젯 배치·시스템 갱신 요청 시 다시 그림. 클래스 이름은 이미 놓인 위젯이 유지되도록 그대로 둠 */
class TodoWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = TodoWidget.refresh(context)
}
