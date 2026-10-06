package com.pintodo.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pintodo.data.Todo
import com.pintodo.data.TodoStore
import com.pintodo.notify.Actions
import com.pintodo.notify.Sync
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 바깥(알림·위젯·빠른 추가)에서 들어온 요청 */
sealed interface OpenRequest {
    data class Edit(val id: Int) : OpenRequest
    data class New(val title: String, val memo: String = "") : OpenRequest
}

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_NEW_TITLE = "new_title"
        const val EXTRA_NEW_MEMO = "new_memo"
    }

    private val openRequest = mutableStateOf<OpenRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) openRequest.value = intent.toRequest()
        setContent {
            PinTodoTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App(openRequest)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openRequest.value = intent.toRequest()
    }

    override fun onResume() {
        super.onResume()
        // 강제 종료 등으로 사라진 알림/알람 복원
        Sync.run(this)
    }

    private fun Intent.toRequest(): OpenRequest? = when {
        hasExtra(EXTRA_NEW_TITLE) -> OpenRequest.New(getStringExtra(EXTRA_NEW_TITLE).orEmpty(), getStringExtra(EXTRA_NEW_MEMO).orEmpty())
        getIntExtra(EXTRA_TODO_ID, -1) >= 0 -> OpenRequest.Edit(getIntExtra(EXTRA_TODO_ID, -1))
        else -> null
    }
}

private data class EditTarget(val todo: Todo, val isNew: Boolean)

private enum class Tab(val label: String, val icon: ImageVector) {
    TODO("할 일", Icons.Rounded.CheckCircle),
    HISTORY("기록", Icons.Rounded.History),
    SETTINGS("설정", Icons.Rounded.Settings),
}

@Composable
private fun App(openRequest: MutableState<OpenRequest?>) {
    val ctx = LocalContext.current
    val todos by TodoStore.flow(ctx).collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var snoozing by remember { mutableStateOf<Todo?>(null) }
    var editDirty by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<OpenRequest?>(null) }
    var onboarding by remember { mutableStateOf(needsOnboarding(ctx)) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 시작/종료 시각에 맞춰 상태 표시가 바뀌도록 주기적으로 갱신
    val now by produceState(System.currentTimeMillis(), todos) {
        while (true) {
            value = System.currentTimeMillis()
            delay(15_000)
        }
    }

    fun closeEditor() {
        editing = null
        editDirty = false
    }

    fun open(req: OpenRequest) {
        when (req) {
            is OpenRequest.Edit -> todos.find { it.id == req.id }?.let { editing = EditTarget(it, isNew = false) }
            is OpenRequest.New -> editing = EditTarget(newTodo(ctx, req.title).copy(memo = req.memo), isNew = true)
        }
        editDirty = false
        tab = Tab.TODO.ordinal
    }

    LaunchedEffect(openRequest.value, todos) {
        val req = openRequest.value ?: return@LaunchedEffect
        openRequest.value = null
        // 수정 중에 알림·위젯으로 다른 할 일을 열면 저장 안 한 내용이 날아가지 않게 먼저 물음
        val same = req is OpenRequest.Edit && req.id == editing?.todo?.id && editing?.isNew == false
        if (editing != null && editDirty && !same) pending = req else if (!same) open(req)
    }
    // 기록·설정 탭에서 뒤로가기 → 할 일 탭으로, 할 일 탭에서 한 번 더 누르면 종료 (편집 화면은 자체 처리)
    BackHandler(enabled = editing == null && tab != Tab.TODO.ordinal) { tab = Tab.TODO.ordinal }

    /** 기간이 끝난 할 일을 지금부터 완료할 때까지 다시 띄움 */
    fun restart(todo: Todo) {
        val now = System.currentTimeMillis()
        Actions.save(ctx, todo.copy(startAt = now - now % 60_000L, endAt = null, hiddenKey = null, alertedKey = null, snoozeUntil = null))
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar("'${todo.title}' 다시 알림창에 띄웠어요", actionLabel = "일정 바꾸기", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) {
                TodoStore.get(ctx, todo.id)?.let { editing = EditTarget(it, isNew = false) }
            }
        }
    }

    if (onboarding) {
        OnboardingScreen(onDone = { onboarding = false })
        return
    }

    fun complete(todo: Todo) {
        Actions.save(ctx, Actions.complete(todo, System.currentTimeMillis()))
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = if (todo.isRepeat) "'${todo.title}' 오늘 완료" else "'${todo.title}' 완료",
                actionLabel = "실행 취소",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                TodoStore.get(ctx, todo.id)?.let { Actions.save(ctx, todo) }
            }
        }
    }

    AnimatedContent(
        targetState = editing,
        transitionSpec = { (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut() },
        label = "screen",
    ) { target ->
        if (target != null) {
            EditScreen(
                initial = target.todo,
                isNew = target.isNew,
                onClose = ::closeEditor,
                onSave = { todo ->
                    Actions.save(ctx, if (target.isNew) todo.copy(id = TodoStore.newId(ctx)) else todo)
                    closeEditor()
                },
                onDelete = {
                    Actions.delete(ctx, target.todo.id)
                    closeEditor()
                },
                onDirtyChange = { editDirty = it },
            )
            return@AnimatedContent
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (tab == Tab.TODO.ordinal) {
                    ExtendedFloatingActionButton(
                        onClick = { editing = EditTarget(newTodo(ctx), isNew = true) },
                        icon = { Icon(Icons.Rounded.Add, null) },
                        text = { Text("할 일 추가", fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            },
            bottomBar = { BottomBar(tab) { tab = it } },
        ) { padding ->
            when (Tab.entries[tab]) {
                Tab.TODO -> TodoScreen(
                    todos = todos,
                    now = now,
                    contentPadding = padding,
                    onEdit = { editing = EditTarget(it, isNew = false) },
                    onComplete = ::complete,
                    onSnooze = { snoozing = it },
                    onRestart = ::restart,
                )
                Tab.HISTORY -> HistoryScreen(
                    todos = todos,
                    contentPadding = padding,
                    onRestore = { Actions.save(ctx, it.copy(doneAt = null)) },
                    onDelete = { Actions.delete(ctx, it.id) },
                    onClearAll = { todos.filter { it.doneAt != null }.forEach { Actions.delete(ctx, it.id) } },
                )
                Tab.SETTINGS -> SettingsScreen(contentPadding = padding)
            }
        }
    }

    pending?.let { req ->
        DiscardDialog(onKeep = { pending = null }, onDiscard = { pending = null; open(req) })
    }

    snoozing?.let { todo ->
        SnoozeSheet(todo, onDismiss = { snoozing = null }) { option ->
            Actions.snooze(ctx, todo.id, option)
            snoozing = null
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            Tab.entries.forEachIndexed { i, t ->
                NavigationBarItem(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    icon = { Icon(t.icon, null) },
                    label = { Text(t.label, fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }
    }
}
