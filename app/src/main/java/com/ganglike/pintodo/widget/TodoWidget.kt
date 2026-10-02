package com.ganglike.pintodo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ganglike.pintodo.R
import com.ganglike.pintodo.data.Status
import com.ganglike.pintodo.data.Todo
import com.ganglike.pintodo.data.TodoStore
import com.ganglike.pintodo.notify.Actions
import com.ganglike.pintodo.ui.Format
import com.ganglike.pintodo.ui.MainActivity
import com.ganglike.pintodo.ui.QuickAddActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 홈 화면 위젯: 진행 중·예정인 할 일 목록 */
class TodoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent(context) }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun refresh(context: Context) {
            val app = context.applicationContext
            scope.launch { runCatching { TodoWidget().updateAll(app) } }
        }
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodoWidget()
}

/** 위젯의 체크 버튼 */
class CompleteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[KEY_ID] ?: return
        val todo = TodoStore.get(context, id) ?: return
        Actions.save(context, Actions.complete(todo, System.currentTimeMillis()))
    }
}

private val KEY_ID = ActionParameters.Key<Int>("id")
private val KEY_OPEN = ActionParameters.Key<Int>(MainActivity.EXTRA_TODO_ID)

private object WColors {
    val bg = ColorProvider(Color(0xFFFFFFFF), Color(0xFF202027))
    val text = ColorProvider(Color(0xFF191F28), Color(0xFFE4E4E5))
    val sub = ColorProvider(Color(0xFF8B95A1), Color(0xFF8E8E98))
    val primary = ColorProvider(Color(0xFF3182F6), Color(0xFF4B96FF))
    val orange = ColorProvider(Color(0xFFF08A00), Color(0xFFFFA733))
    val white = ColorProvider(Color.White, Color.White)
    val line = ColorProvider(Color(0xFFB0B8C1), Color(0xFF5A5A66))
}

@Composable
private fun WidgetContent(context: Context) {
    val todos by TodoStore.flow(context).collectAsState()
    val now = System.currentTimeMillis()
    val visible = setOf(Status.SHOWING, Status.SNOOZED)
    val items = todos
        .filter { it.status(now) in visible + Status.SCHEDULED }
        .sortedWith(compareBy<Todo>({ it.status(now) !in visible }, { it.nextStart(now) ?: 0L }))
    val showing = items.count { it.status(now) == Status.SHOWING }

    Column(
        GlanceModifier.fillMaxSize().background(WColors.bg).cornerRadius(24.dp)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                Text("고정 투두", style = TextStyle(color = WColors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Text(
                    if (items.isEmpty()) "할 일이 없어요" else "알림 중 ${showing}개 · 전체 ${items.size}개",
                    style = TextStyle(color = WColors.sub, fontSize = 12.sp),
                )
            }
            Box(
                GlanceModifier.size(36.dp).cornerRadius(18.dp).background(WColors.primary)
                    .clickable(actionStartActivity<QuickAddActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Image(ImageProvider(R.drawable.ic_add), "할 일 추가", GlanceModifier.size(20.dp), colorFilter = ColorFilter.tint(WColors.white))
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        LazyColumn(GlanceModifier.fillMaxSize()) {
            items(items, itemId = { it.id.toLong() }) { todo -> TodoRow(todo, now) }
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, now: Long) {
    val status = todo.status(now)
    val canComplete = status == Status.SHOWING || status == Status.SNOOZED
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            GlanceModifier.size(32.dp).then(
                if (canComplete) GlanceModifier.clickable(actionRunCallback<CompleteAction>(actionParametersOf(KEY_ID to todo.id)))
                else GlanceModifier
            ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                ImageProvider(if (canComplete) R.drawable.ic_circle else R.drawable.ic_schedule),
                if (canComplete) "완료" else "예정",
                GlanceModifier.size(22.dp),
                colorFilter = ColorFilter.tint(if (status == Status.SHOWING) WColors.primary else WColors.line),
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(
            GlanceModifier.defaultWeight()
                .clickable(actionStartActivity<MainActivity>(actionParametersOf(KEY_OPEN to todo.id))),
        ) {
            Text(todo.title, maxLines = 1, style = TextStyle(color = WColors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium))
            Text(
                Format.status(todo, now),
                maxLines = 1,
                style = TextStyle(
                    color = when (status) {
                        Status.SHOWING -> WColors.primary
                        Status.SNOOZED -> WColors.orange
                        else -> WColors.sub
                    },
                    fontSize = 12.sp,
                ),
            )
        }
    }
}
