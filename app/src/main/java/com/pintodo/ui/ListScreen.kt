@file:OptIn(ExperimentalMaterial3Api::class)

package com.pintodo.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pintodo.data.Status
import com.pintodo.data.Todo
import com.pintodo.notify.Notifier
import com.pintodo.notify.Sync
import kotlinx.coroutines.launch

@Composable
fun TodoScreen(
    todos: List<Todo>,
    now: Long,
    contentPadding: PaddingValues,
    onEdit: (Todo) -> Unit,
    onComplete: (Todo) -> Unit,
    onSnooze: (Todo) -> Unit,
    onRestart: (Todo) -> Unit,
) {
    val active = todos.filter { it.doneAt == null }
    val inProgress = active.filter { it.status(now) in setOf(Status.SHOWING, Status.SNOOZED) }
    val upcoming = active.filter { it.status(now) in setOf(Status.SCHEDULED, Status.HIDDEN) }
        .sortedBy { it.nextStart(now) ?: Long.MAX_VALUE }
    val ended = active.filter { it.status(now) == Status.ENDED }
    val noAlert = active.filter { it.status(now) == Status.NO_ALERT }
    val showing = inProgress.count { it.status(now) == Status.SHOWING }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            ScreenHeader("PinTodo", if (active.isEmpty()) "할 일이 없어요" else "알림 중 ${showing}개 · 예정 ${upcoming.size}개")
        }
        item(key = "banners") { Banners() }
        if (active.isEmpty()) item(key = "empty") { EmptyState() }

        section("진행 중", inProgress, now, onEdit, onComplete, onSnooze, onRestart)
        section("예정", upcoming, now, onEdit, onComplete, onSnooze, onRestart)
        section("알림 없는 할 일", noAlert, now, onEdit, onComplete, onSnooze, onRestart)
        section("기간 종료", ended, now, onEdit, onComplete, onSnooze, onRestart)
    }
}

private fun LazyListScope.section(
    title: String,
    list: List<Todo>,
    now: Long,
    onEdit: (Todo) -> Unit,
    onComplete: (Todo) -> Unit,
    onSnooze: (Todo) -> Unit,
    onRestart: (Todo) -> Unit,
) {
    if (list.isEmpty()) return
    item(key = "h-$title") { SectionLabel("$title ${list.size}", Modifier.animateItem()) }
    items(list, key = { it.id }) { todo ->
        TodoCard(todo, now, Modifier.animateItem(), onEdit, onComplete, onSnooze, onRestart)
    }
}

@Composable
private fun TodoCard(
    todo: Todo,
    now: Long,
    modifier: Modifier,
    onEdit: (Todo) -> Unit,
    onComplete: (Todo) -> Unit,
    onSnooze: (Todo) -> Unit,
    onRestart: (Todo) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val status = todo.status(now)
    val completeLabel = if (todo.isRepeat) "오늘 완료" else "완료"
    val visible = status == Status.SHOWING || status == Status.SNOOZED
    // 반복 할 일은 진행 중일 때만 '오늘 완료' 가능
    val canComplete = !todo.isRepeat || visible
    val scope = rememberCoroutineScope()
    // rememberSwipeToDismissBoxState는 상태를 저장/복원해서, 실행 취소로 카드가 돌아오면
    // '밀린 상태'로 복원되어 완료가 다시 실행된다. 그래서 매번 새 상태로 시작한다.
    val density = LocalDensity.current
    val swipe = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, density, positionalThreshold = { it * 0.4f }) }

    SwipeToDismissBox(
        state = swipe,
        modifier = modifier,
        enableDismissFromStartToEnd = canComplete,
        enableDismissFromEndToStart = status == Status.SHOWING,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) onComplete(todo) else onSnooze(todo)
            scope.launch { swipe.reset() }
        },
        backgroundContent = {
            // 밀지 않을 때 배경을 그리면 카드의 둥근 모서리 사이로 색이 비침
            if (swipe.dismissDirection == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val toEnd = swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Surface(
                color = if (toEnd) c.primary else c.tertiary,
                contentColor = c.onPrimary,
                shape = RoundedCornerShape(Dimens.CardRadius),
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (toEnd) Arrangement.Start else Arrangement.End,
                ) {
                    Icon(if (toEnd) Icons.Rounded.Check else Icons.Rounded.Snooze, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            !toEnd -> "미루기"
                            todo.isRepeat -> "오늘 완료"
                            else -> "완료"
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        },
    ) {
        Surface(
            onClick = { onEdit(todo) },
            shape = RoundedCornerShape(Dimens.CardRadius),
            color = c.surface,
            // 스와이프를 못 쓰는 TalkBack 사용자도 완료·미루기를 할 수 있게
            modifier = Modifier.semantics {
                customActions = buildList {
                    if (canComplete) add(CustomAccessibilityAction(completeLabel) { onComplete(todo); true })
                    if (status == Status.SHOWING) add(CustomAccessibilityAction("미루기") { onSnooze(todo); true })
                    if (status == Status.ENDED) add(CustomAccessibilityAction("다시 하기") { onRestart(todo); true })
                }
            },
        ) {
            Row(Modifier.padding(start = 6.dp, end = 18.dp, top = 12.dp, bottom = 14.dp)) {
                CheckCircle(
                    checked = false, active = status == Status.SHOWING,
                    onClick = if (canComplete) ({ onComplete(todo) }) else null,
                    description = "$completeLabel: ${todo.title}",
                )
                Column(Modifier.weight(1f).padding(top = 9.dp, start = 4.dp)) {
                    Text(todo.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (todo.memo.isNotBlank()) {
                        Text(
                            todo.memo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    MetaRow(todo, status, now)
                    if (status == Status.ENDED) {
                        // 기간이 끝난 할 일을 지금부터 다시 띄우기
                        TextButton(
                            onClick = { onRestart(todo) },
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Icon(Icons.Rounded.Replay, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("다시 하기", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

/** '알림 중 · 평일 09:00–18:00   📌 🔔' 한 줄 요약 */
@Composable
private fun MetaRow(todo: Todo, status: Status, now: Long) {
    val c = MaterialTheme.colorScheme
    // 1시간 안에 끝나는 할 일은 빨간색으로
    val urgent = status == Status.SHOWING && todo.windowAt(now)?.end?.let { it - now < 60 * 60_000L } == true
    val statusColor = when (status) {
        Status.SHOWING -> if (urgent) c.error else c.primary
        Status.SNOOZED -> c.tertiary
        Status.ENDED -> c.error
        else -> c.onSurfaceVariant
    }
    // 상태와 겹치는 정보(기본값 '완료할 때까지', 종료 없는 예정)는 생략
    val redundant = !todo.notify ||
        !todo.isRepeat && todo.endAt == null && (todo.startAt == null || status == Status.SCHEDULED)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = statusColor, fontWeight = FontWeight.SemiBold)) { append(Format.status(todo, now)) }
                if (!redundant) {
                    withStyle(SpanStyle(color = c.outline)) { append("  ·  ${Format.schedule(todo)}") }
                }
            },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (todo.notify) {
            Spacer(Modifier.width(8.dp))
            if (todo.pinned) Icon(Icons.Rounded.PushPin, "고정", Modifier.size(14.dp), tint = c.outline)
            Spacer(Modifier.width(4.dp))
            Icon(todo.alertMode.icon(), todo.alertMode.label, Modifier.size(14.dp), tint = c.outline)
        }
    }
}

/** 알림 권한 / 정확한 알람 / 배터리 최적화 안내 */
@Composable
private fun Banners() {
    val ctx = LocalContext.current
    var notifOk by remember { mutableStateOf(Notifier.enabled(ctx)) }
    var exactOk by remember { mutableStateOf(Sync.canExact(ctx)) }
    var exactHidden by remember { mutableStateOf(uiPrefs(ctx).getBoolean("exact_banner_hidden", false)) }
    var batteryOk by remember { mutableStateOf(ignoringBattery(ctx)) }
    var batteryHidden by remember { mutableStateOf(uiPrefs(ctx).getBoolean("battery_banner_hidden", false)) }

    LifecycleResumeEffect(Unit) {
        notifOk = Notifier.enabled(ctx)
        exactOk = Sync.canExact(ctx)
        batteryOk = ignoringBattery(ctx)
        onPauseOrDispose { }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!notifOk) {
            Banner(
                icon = Icons.Rounded.NotificationsOff,
                text = "알림 권한이 꺼져 있어서 할 일이 알림창에 표시되지 않아요",
                action = "알림 켜기",
                onAction = { ctx.startActivity(notificationSettings(ctx)) },
            )
        }
        if (!exactOk && !exactHidden) {
            Banner(
                icon = Icons.Rounded.Alarm,
                text = "'알람 및 리마인더' 권한이 꺼져 있어서 정한 시각보다 알림이 몇 분 늦게 뜰 수 있어요",
                action = "허용하기",
                onAction = { ctx.startActivity(exactAlarmSettings(ctx)) },
                onClose = {
                    exactHidden = true
                    uiPrefs(ctx).edit().putBoolean("exact_banner_hidden", true).apply()
                },
            )
        }
        if (!batteryOk && !batteryHidden) {
            Banner(
                icon = Icons.Rounded.BatteryAlert,
                text = "배터리 절전 때문에 알림 고정이나 예약이 늦어질 수 있어요. 앱 정보 > 배터리에서 '제한 없음'으로 바꿔 주세요",
                action = "설정 열기",
                onAction = { ctx.startActivity(batteryExemption(ctx)) },
                onClose = {
                    batteryHidden = true
                    uiPrefs(ctx).edit().putBoolean("battery_banner_hidden", true).apply()
                },
            )
        }
    }
}

@Composable
private fun Banner(icon: ImageVector, text: String, action: String, onAction: () -> Unit, onClose: (() -> Unit)? = null) {
    GroupCard {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.End) {
            if (onClose != null) {
                TextButton(onClick = onClose) { Text("닫기", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            TextButton(onClick = onAction) { Text(action, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun EmptyState(title: String = "할 일이 없어요", body: String = "추가한 할 일은 완료할 때까지\n알림창에 고정돼요", icon: ImageVector = Icons.Rounded.PushPin) {
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxSize()) {}
            Icon(icon, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

fun ignoringBattery(ctx: Context) =
    ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

/** 앱 정보 화면 (배터리 > 제한 없음). Play 정책상 절전 예외 요청 대화상자 대신 설정 화면으로 안내 */
fun batteryExemption(ctx: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))

/** '알람 및 리마인더' 허용 화면 (Android 12+에서만 권한이 꺼질 수 있음) */
fun exactAlarmSettings(ctx: Context) =
    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))

fun notificationSettings(ctx: Context) =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)

fun uiPrefs(ctx: Context) = ctx.getSharedPreferences("ui", Context.MODE_PRIVATE)
