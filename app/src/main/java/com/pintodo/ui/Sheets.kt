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
import androidx.compose.ui.text.style.TextOverflow
import android.widget.Toast
import androidx.compose.ui.unit.dp
import com.pintodo.data.DateParser
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
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, memo: String, startAt: Long?, endAt: Long?, notify: Boolean) -> Unit,
    onDetail: (title: String, memo: String) -> Unit,
    initialTitle: String = "",
    memo: String = "",
) {
    val ctx = LocalContext.current
    val settings = SettingsStore.get(ctx)
    var title by remember { mutableStateOf(initialTitle) }
    var start by remember { mutableStateOf(if (settings.defaultNotify) 0 else 4) }
    // 제목·메모에 날짜/시각이 있으면 그 일정이 맨 앞 칩으로 생기고 자동 선택됨
    val parsed = remember(title, memo) { DateParser.parse("$title\n$memo", System.currentTimeMillis()) }
    var useParsed by remember(parsed?.matched) { mutableStateOf(parsed != null) }
    val focus = remember { FocusRequester() }
    // 마지막 '알림 없이'는 알림·예약 없이 목록에만 추가
    val starts = listOf("지금" to null, "30분 뒤" to 30, "1시간 뒤" to 60, "내일 아침 9시" to -1, "알림 없이" to -2)
    val notify = (useParsed && parsed != null) || starts[start].second != -2

    fun endAt(): Long? = if (useParsed && parsed != null) parsed.endAt else null

    fun startAt(): Long? {
        if (useParsed && parsed != null) return parsed.startAt
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
            onDone = { if (title.isNotBlank()) hide { onAdd(title.trim(), memo, startAt(), endAt(), notify) } },
            modifier = Modifier.focusRequester(focus),
        )
        if (memo.isNotBlank()) {
            Text(
                memo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (parsed != null) SoftChip(parsedLabel(parsed), useParsed) { useParsed = true }
            starts.forEachIndexed { i, (label, _) -> SoftChip(label, !useParsed && start == i) { useParsed = false; start = i } }
        }
        if (useParsed && parsed != null) {
            Text(
                "'${parsed.matched}'을(를) 읽어 일정을 맞췄어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp),
            )
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
            SecondaryButton("자세히", { hide { onDetail(title.trim(), memo) } }, Modifier.weight(1f))
            PrimaryButton("추가", { hide { onAdd(title.trim(), memo, startAt(), endAt(), notify) } }, Modifier.weight(1.6f), enabled = title.isNotBlank())
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

/** 읽은 일정 칩 문구: '내일 오후 3:00', '내일 오후 3:00–오후 5:00', '금요일 자정까지' */
private fun parsedLabel(p: DateParser.Result): String = when {
    p.startAt != null && p.endAt != null -> "${Format.dateTime(p.startAt)} – ${Format.dateTime(p.endAt)}"
    p.startAt != null -> Format.dateTime(p.startAt)
    else -> "${Format.dateTime(p.endAt!!)}까지"
}

/** 새 할 일 기본값 적용 */
fun newTodo(ctx: android.content.Context, title: String = "", startAt: Long? = null, notify: Boolean? = null): Todo {
    val s = SettingsStore.get(ctx)
    return Todo(
        id = -1, title = title, notify = notify ?: s.defaultNotify,
        pinned = s.defaultPinned, alertMode = s.defaultAlertMode, startAt = startAt,
        remindEvery = s.defaultRemindEvery,
    )
}

/** 빠른 추가 (위젯 +, 빠른 설정 타일, 다른 앱의 '공유 → PinTodo에 추가') */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val (sharedTitle, sharedMemo) = sharedText(intent)
        setContent {
            PinTodoTheme {
                QuickAddSheet(
                    onDismiss = ::finish,
                    onAdd = { title, memo, startAt, endAt, notify ->
                        val todo = newTodo(this, title, startAt, notify).copy(memo = memo.trim(), endAt = if (notify) endAt else null)
                        Actions.save(this, todo.copy(id = TodoStore.newId(this), createdAt = System.currentTimeMillis()))
                        Toast.makeText(this, "'$title' 추가했어요", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onDetail = { title, memo ->
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .putExtra(MainActivity.EXTRA_NEW_TITLE, title)
                                .putExtra(MainActivity.EXTRA_NEW_MEMO, memo)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                        finish()
                    },
                    initialTitle = sharedTitle,
                    memo = sharedMemo,
                )
            }
        }
    }

    /** 공유받은 글: 제목(EXTRA_SUBJECT)이 있으면 그것을, 없으면 첫 줄을 제목으로, 나머지는 메모로 */
    private fun sharedText(intent: Intent?): Pair<String, String> {
        if (intent?.action != Intent.ACTION_SEND) return "" to ""
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().trim()
        if (subject.isNotEmpty()) return subject.take(100) to text
        val lines = text.lines()
        val first = lines.firstOrNull().orEmpty().trim()
        return first.take(100) to (if (first.length > 100) text else lines.drop(1).joinToString("\n").trim())
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
