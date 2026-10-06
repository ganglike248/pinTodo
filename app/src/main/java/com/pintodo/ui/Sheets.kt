package com.pintodo.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pintodo.data.SettingsStore
import com.pintodo.data.SnoozeOption
import com.pintodo.data.Todo
import com.pintodo.data.TodoStore
import com.pintodo.notify.ActionReceiver
import com.pintodo.notify.Actions
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

/** 회색 채움 입력창 (밑줄·테두리 없음) */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Default,
    onDone: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline) },
        singleLine = singleLine,
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** 미루기 선택 시트 (알림의 '미루기…', 목록에서 왼쪽으로 밀기) */
@Composable
fun SnoozeSheet(todo: Todo, onDismiss: () -> Unit, onPick: (SnoozeOption) -> Unit) {
    val ctx = LocalContext.current
    val now = System.currentTimeMillis()
    val options = SettingsStore.get(ctx).enabledSnoozes.map { it to it.until(now) }
    AppSheet(onDismiss) { hide ->
        SheetTitle("언제 다시 알려드릴까요?", todo.title)
        options.forEach { (option, until) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { hide { onPick(option) } }
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(Icons.Rounded.Snooze, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(14.dp))
                Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(Format.dateTime(until), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            "선택지는 설정 > 미루기에서 바꿀 수 있어요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp, start = 8.dp),
        )
    }
}

/** 빠른 추가 시트 (위젯 +, 빠른 설정 타일) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddSheet(onDismiss: () -> Unit, onAdd: (title: String, startAt: Long?, notify: Boolean) -> Unit, onDetail: (String) -> Unit) {
    val ctx = LocalContext.current
    val settings = SettingsStore.get(ctx)
    var title by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(if (settings.defaultNotify) 0 else 4) }
    val focus = remember { FocusRequester() }
    // 마지막 '알림 없이'는 알림·예약 없이 목록에만 추가
    val starts = listOf("지금" to null, "30분 뒤" to 30, "1시간 뒤" to 60, "내일 아침 9시" to -1, "알림 없이" to -2)
    val notify = starts[start].second != -2

    fun startAt(): Long? {
        val now = System.currentTimeMillis()
        return when (val m = starts[start].second) {
            null, -2 -> null
            -1 -> {
                val zone = ZoneId.systemDefault()
                LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusHours(9).toInstant().toEpochMilli()
            }
            else -> (now + m * 60_000L).let { it - it % 60_000L }
        }
    }

    AppSheet(onDismiss) { hide ->
        SheetTitle("할 일 추가")
        AppTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "무엇을 해야 하나요?",
            imeAction = ImeAction.Done,
            onDone = { if (title.isNotBlank()) hide { onAdd(title.trim(), startAt(), notify) } },
            modifier = Modifier.focusRequester(focus),
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            starts.forEachIndexed { i, (label, _) -> SoftChip(label, start == i) { start = i } }
        }
        Spacer(Modifier.height(14.dp))
        if (notify) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            if (settings.defaultPinned) {
                Icon(Icons.Rounded.PushPin, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(4.dp))
            }
            Icon(settings.defaultAlertMode.icon(), null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(6.dp))
            Text(
                (if (settings.defaultPinned) "고정 · " else "") + settings.defaultAlertMode.label + " · 기본값은 설정에서 바꿀 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.imePadding(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("자세히", { hide { onDetail(title.trim()) } }, Modifier.weight(1f))
            PrimaryButton("추가", { hide { onAdd(title.trim(), startAt(), notify) } }, Modifier.weight(1.6f), enabled = title.isNotBlank())
        }
        // 시트 창이 붙은 뒤에 포커스를 줘야 키보드가 올라옴
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            delay(300)
            focus.requestFocus()
            keyboard?.show()
        }
    }
}

/** 새 할 일 기본값 적용 */
fun newTodo(ctx: android.content.Context, title: String = "", startAt: Long? = null, notify: Boolean? = null): Todo {
    val s = SettingsStore.get(ctx)
    return Todo(
        id = -1, title = title, notify = notify ?: s.defaultNotify,
        pinned = s.defaultPinned, alertMode = s.defaultAlertMode, startAt = startAt,
    )
}

class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PinTodoTheme {
                QuickAddSheet(
                    onDismiss = ::finish,
                    onAdd = { title, startAt, notify ->
                        val todo = newTodo(this, title, startAt, notify)
                        Actions.save(this, todo.copy(id = TodoStore.newId(this), createdAt = System.currentTimeMillis()))
                        finish()
                    },
                    onDetail = { title ->
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .putExtra(MainActivity.EXTRA_NEW_TITLE, title)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                        finish()
                    },
                )
            }
        }
    }
}

class SnoozeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val id = intent.getIntExtra(ActionReceiver.EXTRA_ID, -1)
        val todo = TodoStore.get(this, id) ?: return finish()
        setContent {
            PinTodoTheme {
                SnoozeSheet(todo, onDismiss = ::finish) { option ->
                    Actions.snooze(this, id, option)
                    finish()
                }
            }
        }
    }
}
