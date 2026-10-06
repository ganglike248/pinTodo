@file:OptIn(ExperimentalLayoutApi::class)

package com.pintodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pintodo.data.AlertMode
import com.pintodo.data.CheckItem
import com.pintodo.data.LabelColor
import com.pintodo.data.SettingsStore
import java.time.LocalDate

/** 체크리스트를 화면 회전·프로세스 종료 뒤에도 유지 */
val CheckItemsSaver = listSaver<List<CheckItem>, Any>(
    save = { list -> list.flatMap { listOf(it.text, it.done) } },
    restore = { flat -> flat.chunked(2).map { CheckItem(it[0] as String, it[1] as Boolean) } },
)

/** 체크리스트 편집: 체크 / 글 고치기 / 지우기 + 맨 아래 '항목 추가' */
@Composable
fun ChecklistEditor(items: List<CheckItem>, onChange: (List<CheckItem>) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    fun add() {
        if (draft.isBlank()) return
        onChange(items + CheckItem(draft.trim()))
        draft = ""
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 4.dp),
    ) {
        Text(
            "체크리스트" + if (items.isEmpty()) "" else "  ${items.count { it.done }}/${items.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 14.dp, top = 6.dp),
        )
        items.forEachIndexed { i, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(item.done, { done -> onChange(items.mapIndexed { j, it -> if (j == i) it.copy(done = done) else it }) })
                PlainField(
                    value = item.text,
                    onValueChange = { text -> onChange(items.mapIndexed { j, it -> if (j == i) it.copy(text = text) else it }) },
                    placeholder = "",
                    done = item.done,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onChange(items.filterIndexed { j, _ -> j != i }) }) {
                    Icon(Icons.Rounded.Close, "항목 지우기: ${item.text}", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            }
            PlainField(draft, { draft = it }, "항목 추가", done = false, modifier = Modifier.weight(1f), onDone = ::add)
            if (draft.isNotBlank()) TextButton(onClick = ::add) { Text("추가", fontWeight = FontWeight.Bold) }
        }
    }
}

/** 테두리·채움 없는 한 줄 입력 (체크리스트 항목) */
@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    done: Boolean,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = if (done) c.outline else c.onSurface,
            textDecoration = if (done) TextDecoration.LineThrough else null,
        ),
        cursorBrush = SolidColor(c.primary),
        keyboardOptions = KeyboardOptions(imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = modifier.padding(vertical = 12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.outline)
                inner()
            }
        },
    )
}

/** '+ 메모'처럼 숨겨 둔 입력을 꺼내는 작은 칩 */
@Composable
fun AddChip(label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 색 라벨: 없음 + 6색 */
@Composable
fun LabelColorPicker(selected: LabelColor?, onSelect: (LabelColor?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Text("라벨", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        ColorDot(null, selected == null) { onSelect(null) }
        LabelColor.entries.forEach { color -> ColorDot(color, selected == color) { onSelect(color) } }
    }
}

@Composable
private fun ColorDot(color: LabelColor?, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = color?.let { "${it.label} 라벨" } ?: "라벨 없음"
                this.selected = selected
                role = Role.RadioButton
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color?.let { Color(it.argb) } ?: c.surfaceVariant)
                .then(if (selected) Modifier.border(2.dp, c.onSurface.copy(alpha = 0.5f), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            when {
                selected && color != null -> Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Color.White)
                color == null -> Icon(Icons.Rounded.Block, null, Modifier.size(14.dp), tint = c.outline)
            }
        }
    }
}

/** 반복 종료 */
enum class RepeatEnd(val label: String) { NEVER("계속"), DATE("날짜까지"), COUNT("횟수") }

@Composable
fun RepeatEndEditor(
    mode: RepeatEnd,
    untilDay: Long,
    count: Int,
    onMode: (RepeatEnd) -> Unit,
    onUntil: (Long) -> Unit,
    onCount: (Int) -> Unit,
) {
    var calendar by remember { mutableStateOf(false) }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("반복 종료", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RepeatEnd.entries.forEach { m -> SoftChip(m.label, mode == m) { onMode(m) } }
        }
        when (mode) {
            RepeatEnd.DATE -> FieldCell("마지막 날", "${Format.date(LocalDate.ofEpochDay(untilDay))}까지", true, Modifier.fillMaxWidth()) { calendar = true }
            RepeatEnd.COUNT -> Stepper("${count}회 하고 끝내기", onMinus = { onCount((count - 1).coerceAtLeast(1)) }, onPlus = { onCount((count + 1).coerceAtMost(99)) })
            RepeatEnd.NEVER -> {}
        }
    }
    if (calendar) {
        CalendarDialog(LocalDate.ofEpochDay(untilDay), onDismiss = { calendar = false }) { onUntil(it.toEpochDay()); calendar = false }
    }
}

/** 다시 울리기 선택지(분) */
val REMIND_OPTIONS = listOf(30, 60, 120, 180)

/** 알림 방식 한 줄 요약: '고정 · 소리+진동 · 1시간마다' */
fun alertSummary(pinned: Boolean, mode: AlertMode, remind: Int?): String =
    listOfNotNull(if (pinned) "고정" else "밀면 숨김", mode.label, Format.remind(remind)).joinToString(" · ")

/** 고정 여부 + 소리/진동 + 다시 울리기를 한 시트에서 */
@Composable
fun AlertSheet(
    pinned: Boolean,
    mode: AlertMode,
    remind: Int?,
    onPinned: (Boolean) -> Unit,
    onMode: (AlertMode) -> Unit,
    onRemind: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val quietNight = SettingsStore.get(LocalContext.current).quietNight
    AppSheet(onDismiss) { hide ->
        SheetTitle("알림 방식")
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onPinned(!pinned) }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Icons.Rounded.PushPin)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("알림창에 고정", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    if (pinned) "밀어서 지워도 다시 나타나요" else "밀어서 지우면 이번엔 숨겨요",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(pinned, onPinned)
        }
        Spacer(Modifier.height(16.dp))
        AlertModePicker(mode, onMode, reminds = remind != null)
        Spacer(Modifier.height(20.dp))
        Text("다시 울리기", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftChip("안 함", remind == null) { onRemind(null) }
            REMIND_OPTIONS.forEach { m -> SoftChip(Format.remind(m)!!, remind == m) { onRemind(m) } }
        }
        Hint(
            when {
                remind == null -> "처음 뜰 때만 알려요"
                mode == AlertMode.SILENT -> "무음이라 다시 울리지 않아요. 소리나 진동을 골라 주세요"
                quietNight -> "완료할 때까지 ${Format.remind(remind)} 한 번씩 다시 알려요. 밤(오후 10시~오전 8시)에는 쉬어요 (설정 > 알림)"
                else -> "완료할 때까지 ${Format.remind(remind)} 한 번씩 다시 알려요. 밤에도 울려요 — 쉬게 하려면 설정 > 알림"
            }
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("확인", { hide(onDismiss) })
    }
}

@Composable
fun AutoScheduleHint(matched: String, onUndo: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.primaryContainer).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Event, null, Modifier.size(18.dp), tint = c.primary)
        Spacer(Modifier.width(8.dp))
        Text("'$matched'을(를) 읽어 일정을 맞췄어요", style = MaterialTheme.typography.bodySmall, color = c.onPrimaryContainer, modifier = Modifier.weight(1f))
        TextButton(onClick = onUndo) { Text("되돌리기", fontWeight = FontWeight.Bold) }
    }
}

/** 시작/종료처럼 고르는 칸. 고른 칸의 휠이 아래에 펼쳐짐 */
@Composable
fun FieldCell(label: String, value: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.primaryContainer else c.surfaceVariant)
            .then(if (selected) Modifier.border(1.5.dp, c.primary, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) c.primary else c.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMinus) { Icon(Icons.Rounded.Remove, "줄이기") }
        Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onClick = onPlus) { Icon(Icons.Rounded.Add, "늘리기") }
    }
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

/** 요일 7칸 + 매일/평일/주말 */
@Composable
fun DayPicker(days: List<Int>, onChange: (List<Int>) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (1..7).forEach { d ->
            DayToggle(Format.dayName(d), d in days, Modifier.weight(1f)) {
                onChange(if (d in days) days - d else (days + d).sorted())
            }
        }
    }
    FlowRow(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SoftChip("매일", days.toSet() == (1..7).toSet()) { onChange((1..7).toList()) }
        SoftChip("평일", days.toSet() == (1..5).toSet()) { onChange((1..5).toList()) }
        SoftChip("주말", days.toSet() == setOf(6, 7)) { onChange(listOf(6, 7)) }
    }
}

@Composable
private fun DayToggle(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (selected) c.primary else c.surfaceVariant)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) c.onPrimary else c.onSurfaceVariant,
        )
    }
}
