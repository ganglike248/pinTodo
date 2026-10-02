package com.pintodo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pintodo.data.Todo
import java.time.Instant
import java.time.ZoneId

/** 완료한 할 일 기록 (날짜별) */
@Composable
fun HistoryScreen(
    todos: List<Todo>,
    contentPadding: PaddingValues,
    onRestore: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onClearAll: () -> Unit,
) {
    val done = todos.filter { it.doneAt != null }.sortedByDescending { it.doneAt }
    val groups = done.groupBy { Format.dayLabel(it.doneAt!!) }
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            ScreenHeader("기록", if (done.isEmpty()) null else "완료한 할 일 ${done.size}개") {
                if (done.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("모두 지우기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (done.isEmpty()) {
            item(key = "empty") {
                EmptyState("완료한 할 일이 없어요", "완료한 할 일은 여기에 모이고\n언제든 되돌릴 수 있어요", Icons.Rounded.TaskAlt)
            }
        }
        groups.forEach { (day, list) ->
            item(key = "d-$day") {
                Column {
                    SectionLabel(day)
                    GroupCard {
                        list.forEachIndexed { i, todo ->
                            if (i > 0) HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            DoneRow(todo, onRestore, onDelete)
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("기록을 모두 지울까요?") },
            text = { Text("완료한 할 일 ${done.size}개가 삭제되고 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = { onClearAll(); confirmClear = false }) {
                    Text("지우기", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun DoneRow(todo: Todo, onRestore: (Todo) -> Unit, onDelete: (Todo) -> Unit) {
    val t = Instant.ofEpochMilli(todo.doneAt!!).atZone(ZoneId.systemDefault())
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        // 체크를 다시 누르면 되돌리기
        CheckCircle(checked = true, active = true, onClick = { onRestore(todo) }, size = 24.dp)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                todo.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${Format.time(t.hour * 60 + t.minute)} 완료", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        IconButton(onClick = { onDelete(todo) }) {
            Icon(Icons.Rounded.Close, "삭제", tint = MaterialTheme.colorScheme.outline)
        }
    }
}
