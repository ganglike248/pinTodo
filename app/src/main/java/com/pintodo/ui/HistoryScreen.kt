package com.pintodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pintodo.data.Todo
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 완료한 할 일 기록 (날짜별) */
@Composable
fun HistoryScreen(
    todos: List<Todo>,
    contentPadding: PaddingValues,
    onRestore: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onClearAll: () -> Unit,
) {
    val allDone = todos.filter { it.doneAt != null }.sortedByDescending { it.doneAt }
    var query by rememberSaveable { mutableStateOf("") }
    val done = if (query.isBlank()) allDone
    else allDone.filter { it.title.contains(query.trim(), ignoreCase = true) || it.memo.contains(query.trim(), ignoreCase = true) }
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
            ScreenHeader("기록", if (allDone.isEmpty()) null else "완료한 할 일 ${allDone.size}개") {
                if (allDone.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("모두 지우기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (allDone.isEmpty()) {
            item(key = "empty") {
                EmptyState("완료한 할 일이 없어요", "완료한 할 일은 여기에 모이고\n언제든 되돌릴 수 있어요", Icons.Rounded.TaskAlt)
            }
        } else {
            item(key = "stats") { WeekStats(allDone) }
            item(key = "search") {
                // 입력 채움(surfaceVariant)은 카드 위에 두는 규칙
                GroupCard { AppTextField(query, { query = it }, "제목·메모로 검색", Modifier.padding(8.dp)) }
            }
            if (done.isEmpty()) {
                item(key = "no-result") {
                    Text(
                        "'${query.trim()}'에 맞는 기록이 없어요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                    )
                }
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
            text = { Text("완료한 할 일 ${allDone.size}개가 삭제되고 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = { onClearAll(); confirmClear = false }) {
                    Text("지우기", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("취소") } },
        )
    }
}

/** 이번 주 요일별 완료 수 막대 */
@Composable
private fun WeekStats(done: List<Todo>) {
    val c = MaterialTheme.colorScheme
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val dates = done.map { Instant.ofEpochMilli(it.doneAt!!).atZone(zone).toLocalDate() }
    val counts = (0..6).map { i -> dates.count { it == monday.plusDays(i.toLong()) } }
    val week = counts.sum()
    val lastWeek = dates.count { !it.isBefore(monday.minusWeeks(1)) && it.isBefore(monday) }
    val max = counts.max().coerceAtLeast(1)
    val compare = when {
        lastWeek == 0 -> "지난주에는 완료한 할 일이 없었어요"
        week > lastWeek -> "지난주보다 ${week - lastWeek}개 더 끝냈어요"
        week == lastWeek -> "지난주와 같은 속도예요"
        else -> "지난주에는 ${lastWeek}개 끝냈어요"
    }
    GroupCard {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("이번 주 ${week}개 완료", style = MaterialTheme.typography.titleMedium)
            Text(compare, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clearAndSetSemantics {
                    contentDescription = (0..6).joinToString(", ") { "${Format.dayName(it + 1)}요일 ${counts[it]}개" }
                },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                (0..6).forEach { i ->
                    val isToday = monday.plusDays(i.toLong()) == today
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (counts[i] > 0) "${counts[i]}" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.onSurfaceVariant,
                        )
                        Box(
                            Modifier
                                .padding(top = 2.dp)
                                .fillMaxWidth()
                                .height(4.dp + 52.dp * counts[i] / max)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        counts[i] == 0 -> c.surfaceVariant
                                        isToday -> c.primary
                                        else -> c.primary.copy(alpha = 0.35f)
                                    }
                                ),
                        )
                        Text(
                            Format.dayName(i + 1),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) c.onSurface else c.outline,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneRow(todo: Todo, onRestore: (Todo) -> Unit, onDelete: (Todo) -> Unit) {
    val t = Instant.ofEpochMilli(todo.doneAt!!).atZone(ZoneId.systemDefault())
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        // 체크를 다시 누르면 되돌리기
        CheckCircle(checked = true, active = true, onClick = { onRestore(todo) }, size = 24.dp, description = "되돌리기: ${todo.title}")
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
