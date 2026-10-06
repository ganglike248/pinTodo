package com.pintodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.pintodo.R
import com.pintodo.data.SettingsStore
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
        val now = System.currentTimeMillis()
        val items = items(context, now)
        val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 위젯 크기에 맞춰 런처가 고름: 한 줄·작은 크기는 요약형, 그보다 크면 목록형
            RemoteViews(mapOf(
                SizeF(40f, 40f) to compact(context, items, now),
                SizeF(150f, 130f) to full(context, items, now),
            ))
        } else {
            full(context, items, now)
        }
        manager.updateAppWidget(ids, views)
    }

    private fun items(context: Context, now: Long): List<Todo> {
        val order = listOf(Status.SHOWING, Status.SNOOZED, Status.NO_ALERT, Status.SCHEDULED)
        return TodoStore.all(context)
            .filter { it.status(now) in order }
            .sortedWith(compareBy<Todo>({ order.indexOf(it.status(now)) }, { it.nextStart(now) ?: 0L }))
    }

    private fun subtitle(items: List<Todo>, now: Long) =
        if (items.isEmpty()) "할 일이 없어요" else "알림 중 ${items.count { it.status(now) == Status.SHOWING }}개 · 전체 ${items.size}개"

    /** 요약형: 가장 위의 할 일 하나 + 개수 */
    private fun compact(context: Context, items: List<Todo>, now: Long): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)
        val top = items.firstOrNull()
        views.setOnClickPendingIntent(
            R.id.widget_root,
            activity(context, top?.id ?: REQ_OPEN_APP, Intent(context, MainActivity::class.java).apply {
                if (top != null) putExtra(MainActivity.EXTRA_TODO_ID, top.id)
            }),
        )
        views.setOnClickPendingIntent(R.id.add, activity(context, REQ_QUICK_ADD, Intent(context, QuickAddActivity::class.java)))
        views.setTextViewText(R.id.compact_title, top?.title ?: "할 일이 없어요")
        views.setTextViewText(R.id.compact_sub, if (top == null) "+ 를 눌러 추가해 보세요" else "${Format.status(top, now)} · ${subtitle(items, now)}")
        return views
    }

    /** 목록형 */
    private fun full(context: Context, items: List<Todo>, now: Long): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_todo)
        views.setOnClickPendingIntent(R.id.widget_root, activity(context, REQ_OPEN_APP, Intent(context, MainActivity::class.java)))
        views.setOnClickPendingIntent(R.id.add, activity(context, REQ_QUICK_ADD, Intent(context, QuickAddActivity::class.java)))
        views.setTextViewText(R.id.subtitle, subtitle(items, now))
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
        v.setTextViewText(statusView, Format.status(todo, now) + (Format.progress(todo)?.let { " · ☑ $it" } ?: ""))
        v.setViewVisibility(R.id.row_color, visibleIf(todo.color != null))
        todo.color?.let { v.setInt(R.id.row_color, "setColorFilter", it.argb.toInt()) }

        if (canComplete) {
            v.setOnClickPendingIntent(R.id.row_check, broadcast(context, ActionReceiver.ACTION_DONE, todo.id))
            v.setContentDescription(R.id.row_check, "${if (todo.isRepeat) "오늘 완료" else "완료"}: ${todo.title}")
        }
        // 알림 중인 할 일은 위젯에서 바로 미루기 (설정의 '알림에 바로 보이는 버튼'과 같은 선택지)
        v.setViewVisibility(R.id.row_snooze, visibleIf(status == Status.SHOWING))
        if (status == Status.SHOWING) {
            v.setOnClickPendingIntent(R.id.row_snooze, broadcast(context, ActionReceiver.ACTION_SNOOZE, todo.id))
            v.setContentDescription(R.id.row_snooze, "${SettingsStore.get(context).quickSnooze.buttonLabel} 미루기: ${todo.title}")
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

    // 알림 버튼과 같은 Intent(같은 requestCode·action)라 PendingIntent를 공유함
    private fun broadcast(context: Context, action: String, id: Int) = PendingIntent.getBroadcast(
        context, id,
        Intent(context, ActionReceiver::class.java).setAction(action).putExtra(ActionReceiver.EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun visibleIf(b: Boolean) = if (b) View.VISIBLE else View.GONE
}

/** 위젯 배치·시스템 갱신 요청 시 다시 그림. 클래스 이름은 이미 놓인 위젯이 유지되도록 그대로 둠 */
class TodoWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = TodoWidget.refresh(context)
}
