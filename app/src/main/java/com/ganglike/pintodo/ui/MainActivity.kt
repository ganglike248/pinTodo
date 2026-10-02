package com.ganglike.pintodo.ui

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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganglike.pintodo.data.Todo
import com.ganglike.pintodo.data.TodoStore
import com.ganglike.pintodo.notify.Actions
import com.ganglike.pintodo.notify.Sync
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TODO_ID = "todo_id"
    }

    // 알림을 눌러 들어오면 해당 할 일 편집 화면을 연다
    private val openRequest = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) openRequest.value = intent.todoId()
        setContent {
            PinTodoTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    App(openRequest)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openRequest.value = intent.todoId()
    }

    override fun onResume() {
        super.onResume()
        // 강제 종료 등으로 사라진 알림/알람 복원
        Sync.run(this)
    }

    private fun Intent.todoId() = getIntExtra(EXTRA_TODO_ID, -1).takeIf { it >= 0 }
}

private data class EditTarget(val todo: Todo, val isNew: Boolean)

@Composable
private fun App(openRequest: MutableState<Int?>) {
    val ctx = LocalContext.current
    val todos by TodoStore.flow(ctx).collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 시작/종료 시각에 맞춰 상태 표시가 바뀌도록 주기적으로 갱신
    val now by produceState(System.currentTimeMillis(), todos) {
        while (true) {
            value = System.currentTimeMillis()
            delay(15_000)
        }
    }

    LaunchedEffect(openRequest.value, todos) {
        val id = openRequest.value ?: return@LaunchedEffect
        todos.find { it.id == id }?.let { editing = EditTarget(it, isNew = false) }
        openRequest.value = null
    }

    BackHandler(enabled = editing != null) { editing = null }

    fun complete(todo: Todo) {
        val t = System.currentTimeMillis()
        Actions.save(ctx, Actions.complete(todo, t))
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
        transitionSpec = {
            (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut()
        },
        label = "screen",
    ) { target ->
        if (target == null) {
            ListScreen(
                todos = todos,
                now = now,
                snackbar = snackbar,
                onAdd = { editing = EditTarget(Todo(id = -1, title = ""), isNew = true) },
                onEdit = { editing = EditTarget(it, isNew = false) },
                onComplete = ::complete,
                onRestore = { Actions.save(ctx, it.copy(doneAt = null)) },
                onDelete = { Actions.delete(ctx, it.id) },
                onClearDone = { todos.filter { it.doneAt != null }.forEach { Actions.delete(ctx, it.id) } },
            )
        } else {
            EditScreen(
                initial = target.todo,
                isNew = target.isNew,
                onClose = { editing = null },
                onSave = { todo ->
                    Actions.save(ctx, if (target.isNew) todo.copy(id = TodoStore.newId(ctx)) else todo)
                    editing = null
                },
                onDelete = {
                    Actions.delete(ctx, target.todo.id)
                    editing = null
                },
            )
        }
    }
}
