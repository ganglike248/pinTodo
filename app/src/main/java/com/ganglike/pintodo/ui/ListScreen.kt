@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ganglike.pintodo.ui

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ganglike.pintodo.data.Status
import com.ganglike.pintodo.data.Todo
import com.ganglike.pintodo.notify.Notifier
import com.ganglike.pintodo.notify.Sync
import kotlinx.coroutines.launch

@Composable
fun ListScreen(
    todos: List<Todo>,
    now: Long,
    snackbar: SnackbarHostState,
    onAdd: () -> Unit,
    onEdit: (Todo) -> Unit,
    onComplete: (Todo) -> Unit,
    onRestore: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onClearDone: () -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val active = todos.filter { it.doneAt == null }
    val showing = active.filter { it.status(now) in setOf(Status.SHOWING, Status.SNOOZED) }
    val upcoming = active.filter { it.status(now) in setOf(Status.SCHEDULED, Status.HIDDEN) }
        .sortedBy { it.nextStart(now) ?: Long.MAX_VALUE }
    val ended = active.filter { it.status(now) == Status.ENDED }
    val done = todos.filter { it.doneAt != null }.sortedByDescending { it.doneAt }
    var showDone by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("고정 투두", fontWeight = FontWeight.Bold) },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("할 일 추가") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "banners") { Banners() }

            if (todos.isEmpty()) {
                item(key = "empty") { EmptyState() }
            }

            section("진행 중", showing, now, onEdit, onComplete, onRestore, onDelete)
            section("예정", upcoming, now, onEdit, onComplete, onRestore, onDelete)
            section("기간 종료", ended, now, onEdit, onComplete, onRestore, onDelete)

            if (done.isNotEmpty()) {
                item(key = "done-header") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { showDone = !showDone }) {
                            Text("완료됨 ${done.size}")
                            Icon(if (showDone) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                        }
                        Spacer(Modifier.weight(1f))
                        if (showDone) TextButton(onClick = onClearDone) { Text("모두 지우기") }
                    }
                }
                if (showDone) {
                    items(done, key = { it.id }) { todo ->
                        TodoCard(todo, now, Modifier.animateItem(), onEdit, onComplete, onRestore, onDelete)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.section(
    title: String,
    list: List<Todo>,
    now: Long,
    onEdit: (Todo) -> Unit,
    onComplete: (Todo) -> Unit,
    onRestore: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
) {
    if (list.isEmpty()) return
    item(key = "h-$title") {
        Text(
            "$title  ${list.size}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp).animateItem(),
        )
    }
    items(list, key = { it.id }) { todo ->
        TodoCard(todo, now, Modifier.animateItem(), onEdit, onComplete, onRestore, onDelete)
    }
}

@Composable
private fun TodoCard(
    todo: Todo,
    now: Long,
    modifier: Modifier,
    onEdit: (Todo) -> Unit,
    onComplete: (Todo) -> Unit,
    onRestore: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
) {
    val status = todo.status(now)
    val isDone = status == Status.DONE
    // 반복 할 일은 표시 중일 때만 '이번 회차 완료' 가능
    val canComplete = !isDone && (!todo.isRepeat || status in setOf(Status.SHOWING, Status.SNOOZED))
    val scope = rememberCoroutineScope()
    // rememberSwipeToDismissBoxState는 상태를 저장/복원해서, 실행 취소로 카드가 돌아오면
    // '밀린 상태'로 복원되어 완료가 다시 실행된다. 그래서 매번 새 상태로 시작한다.
    val density = LocalDensity.current
    val swipe = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, density, positionalThreshold = { it * 0.4f }) }

    SwipeToDismissBox(
        state = swipe,
        modifier = modifier,
        enableDismissFromStartToEnd = canComplete,
        enableDismissFromEndToStart = false,
        onDismiss = {
            onComplete(todo)
            scope.launch { swipe.reset() }
        },
        backgroundContent = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (todo.isRepeat) "오늘 완료" else "완료", style = MaterialTheme.typography.titleSmall)
                }
            }
        },
    ) {
        val highlighted = status == Status.SHOWING
        Card(
            onClick = { onEdit(todo) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Row(Modifier.padding(start = 6.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                IconButton(
                    onClick = { if (isDone) onRestore(todo) else if (canComplete) onComplete(todo) },
                    enabled = isDone || canComplete,
                ) {
                    Icon(
                        when {
                            isDone -> Icons.Rounded.CheckCircle
                            canComplete -> Icons.Rounded.RadioButtonUnchecked
                            else -> Icons.Rounded.Repeat
                        },
                        contentDescription = if (isDone) "되돌리기" else "완료",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(Modifier.weight(1f).padding(top = 10.dp)) {
                    Text(
                        todo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isDone) TextDecoration.LineThrough else null,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    if (todo.memo.isNotBlank()) {
                        Text(
                            todo.memo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StatusPill(todo, status, now)
                        if (!isDone) {
                            // 상태 칩과 겹치는 정보(기본값 '완료할 때까지', 종료 없는 예정)는 생략
                            val redundant = !todo.isRepeat && todo.endAt == null &&
                                (todo.startAt == null || status == Status.SCHEDULED)
                            if (!redundant) {
                                Pill(Format.schedule(todo), if (todo.isRepeat) Icons.Rounded.Repeat else Icons.Rounded.Schedule)
                            }
                            if (todo.pinned) Pill(null, Icons.Rounded.PushPin)
                            Pill(null, todo.alertMode.icon())
                        }
                    }
                }
                if (isDone) {
                    IconButton(onClick = { onDelete(todo) }) {
                        Icon(Icons.Rounded.Delete, "삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(todo: Todo, status: Status, now: Long) {
    val c = MaterialTheme.colorScheme
    val (container, content, icon) = when (status) {
        Status.SHOWING -> Triple(c.primary, c.onPrimary, null as ImageVector?)
        Status.SNOOZED -> Triple(c.tertiaryContainer, c.onTertiaryContainer, Icons.Rounded.Snooze)
        Status.SCHEDULED, Status.HIDDEN -> Triple(c.secondaryContainer, c.onSecondaryContainer, null)
        Status.ENDED -> Triple(c.errorContainer, c.onErrorContainer, null)
        Status.DONE -> Triple(c.surfaceContainerHighest, c.onSurfaceVariant, null)
    }
    Pill(Format.status(todo, now), icon, container, content)
}

/** 알림 권한 / 배터리 최적화 안내 */
@Composable
private fun Banners() {
    val ctx = LocalContext.current
    var notifOk by remember { mutableStateOf(Notifier.enabled(ctx)) }
    var batteryOk by remember { mutableStateOf(ignoringBattery(ctx)) }
    var batteryHidden by remember { mutableStateOf(uiPrefs(ctx).getBoolean("battery_banner_hidden", false)) }

    LifecycleResumeEffect(Unit) {
        notifOk = Notifier.enabled(ctx)
        batteryOk = ignoringBattery(ctx)
        onPauseOrDispose { }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOk = Notifier.enabled(ctx)
        Sync.run(ctx)
    }
    LaunchedEffect(Unit) {
        if (!notifOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!notifOk) {
            Banner(
                icon = Icons.Rounded.NotificationsOff,
                text = "알림 권한이 꺼져 있어서 할 일이 알림창에 표시되지 않아요.",
                action = "알림 켜기",
                onAction = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    )
                },
            )
        }
        if (!batteryOk && !batteryHidden) {
            Banner(
                icon = Icons.Rounded.BatteryAlert,
                text = "배터리 절전 때문에 알림 고정이나 예약이 늦어질 수 있어요. 절전 예외로 등록해 두세요.",
                action = "예외 등록",
                onAction = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                    )
                },
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
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
            Row {
                Icon(icon, null, Modifier.padding(top = 2.dp).size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onClose != null) TextButton(onClick = onClose) { Text("닫기") }
                TextButton(onClick = onAction) { Text(action, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.size(88.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PushPin, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("할 일이 없어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "추가한 할 일은 완료할 때까지\n알림창에 고정돼요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private fun ignoringBattery(ctx: Context) =
    ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

private fun uiPrefs(ctx: Context) = ctx.getSharedPreferences("ui", Context.MODE_PRIVATE)
