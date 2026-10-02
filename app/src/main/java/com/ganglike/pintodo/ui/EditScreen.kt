@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ganglike.pintodo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ganglike.pintodo.data.AlertMode
import com.ganglike.pintodo.data.Todo
import java.time.LocalDate
import java.time.ZoneId

private enum class Picker { START, END, DAILY_START, DAILY_END }

@Composable
fun EditScreen(
    initial: Todo,
    isNew: Boolean,
    onClose: () -> Unit,
    onSave: (Todo) -> Unit,
    onDelete: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var memo by rememberSaveable { mutableStateOf(initial.memo) }
    var pinned by rememberSaveable { mutableStateOf(initial.pinned) }
    var alertMode by rememberSaveable { mutableStateOf(initial.alertMode) }
    var repeat by rememberSaveable { mutableStateOf(initial.isRepeat) }
    var startAt by rememberSaveable { mutableStateOf(initial.startAt) }
    var endAt by rememberSaveable { mutableStateOf(initial.endAt) }
    var days by rememberSaveable { mutableStateOf(initial.repeatDays.ifEmpty { (1..5).toSet() }.toList()) }
    var dailyStart by rememberSaveable { mutableStateOf(initial.dailyStart) }
    var dailyEnd by rememberSaveable { mutableStateOf(initial.dailyEnd) }
    var picker by remember { mutableStateOf<Picker?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val error = when {
        repeat && days.isEmpty() -> "요일을 하나 이상 선택해 주세요"
        repeat && dailyEnd == dailyStart -> "시작과 종료 시각이 같아요"
        !repeat && endAt != null && endAt!! <= (startAt ?: now) -> "종료가 시작보다 늦어야 해요"
        else -> null
    }
    val canSave = title.isNotBlank() && error == null

    fun build(): Todo {
        val base = initial.copy(
            title = title.trim(),
            memo = memo.trim(),
            pinned = pinned,
            alertMode = alertMode,
            startAt = if (repeat) null else startAt,
            endAt = if (repeat) null else endAt,
            repeatDays = if (repeat) days.toSet() else emptySet(),
            dailyStart = dailyStart,
            dailyEnd = dailyEnd,
        )
        val scheduleChanged = base.startAt != initial.startAt || base.endAt != initial.endAt ||
            base.repeatDays != initial.repeatDays || base.dailyStart != initial.dailyStart ||
            base.dailyEnd != initial.dailyEnd
        return when {
            isNew -> base.copy(createdAt = System.currentTimeMillis())
            // 일정이 바뀌면 새 할 일처럼 다시 알림
            scheduleChanged -> base.copy(hiddenKey = null, alertedKey = null, snoozeUntil = null)
            // 한 번짜리를 '닫음' 상태에서 수정하면 다시 표시
            !base.isRepeat -> base.copy(hiddenKey = null)
            else -> base
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "닫기") } },
                title = { Text(if (isNew) "새 할 일" else "할 일 수정") },
                actions = {
                    TextButton(onClick = { onSave(build()) }, enabled = canSave) {
                        Text("저장", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 제목/내용
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val focus = remember { FocusRequester() }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("내용 (선택)") },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isNew) LaunchedEffect(Unit) { focus.requestFocus() }
            }

            SectionCard("알림") {
                SettingRow(
                    icon = Icons.Rounded.PushPin,
                    title = "알림창에 고정",
                    value = if (pinned) "밀어서 지워도 다시 나타나요" else "밀어서 지우면 이번엔 숨겨요",
                    onClick = { pinned = !pinned },
                    trailing = { Switch(checked = pinned, onCheckedChange = { pinned = it }) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("알림 방식", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        AlertMode.entries.forEachIndexed { i, mode ->
                            SegmentedButton(
                                selected = alertMode == mode,
                                onClick = { alertMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(i, AlertMode.entries.size),
                                icon = { Icon(mode.icon(), null, Modifier.padding(0.dp)) },
                                label = { Text(mode.label.replace("소리+진동", "모두"), maxLines = 1) },
                            )
                        }
                    }
                    Text(
                        if (alertMode == AlertMode.SILENT) "소리·진동 없이 알림창에만 표시돼요"
                        else "처음 뜰 때 한 번만 울리고, 이후엔 조용히 표시돼요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
            }

            SectionCard("일정") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SegmentedButton(selected = !repeat, onClick = { repeat = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("한 번") }
                    SegmentedButton(selected = repeat, onClick = { repeat = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("반복") }
                }

                if (!repeat) {
                    SettingRow(
                        icon = Icons.Rounded.Alarm,
                        title = "시작",
                        value = startAt?.let { Format.dateTime(it) } ?: "지금 바로",
                        onClick = { picker = Picker.START },
                        trailing = { if (startAt != null) ClearButton { startAt = null } },
                    )
                    FlowRow(
                        Modifier.padding(start = 58.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuickChip("30분 뒤") { startAt = roundMinute(now + 30 * 60_000L) }
                        QuickChip("1시간 뒤") { startAt = roundMinute(now + 60 * 60_000L) }
                        QuickChip("내일 아침 9시") { startAt = tomorrowAt(9 * 60) }
                    }
                    SettingRow(
                        icon = Icons.Rounded.Flag,
                        title = "종료",
                        value = endAt?.let { Format.dateTime(it) } ?: "완료할 때까지",
                        onClick = { picker = Picker.END },
                        trailing = { if (endAt != null) ClearButton { endAt = null } },
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (1..7).forEach { d ->
                            DayToggle(Format.dayName(d), d in days, Modifier.weight(1f)) {
                                days = if (d in days) days - d else (days + d).sorted()
                            }
                        }
                    }
                    FlowRow(
                        Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuickChip("매일") { days = (1..7).toList() }
                        QuickChip("평일") { days = (1..5).toList() }
                        QuickChip("주말") { days = listOf(6, 7) }
                    }
                    SettingRow(
                        icon = Icons.Rounded.Alarm,
                        title = "시작 시각",
                        value = Format.time(dailyStart),
                        onClick = { picker = Picker.DAILY_START },
                    )
                    SettingRow(
                        icon = Icons.Rounded.Flag,
                        title = "종료 시각",
                        value = dailyEnd?.let { Format.time(it) + if (it <= dailyStart) " (다음 날)" else "" } ?: "하루 끝까지",
                        onClick = { picker = Picker.DAILY_END },
                        trailing = { if (dailyEnd != null) ClearButton { dailyEnd = null } },
                    )
                }

                Text(
                    error ?: summary(repeat, days.toSet(), dailyStart, dailyEnd, startAt, endAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }

            if (!isNew) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.Delete, null)
                    Spacer(Modifier.padding(4.dp))
                    Text("삭제")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    when (picker) {
        Picker.START -> DateTimePickerDialog(
            initial = startAt ?: roundMinute(now + 60 * 60_000L),
            onDismiss = { picker = null },
            onConfirm = { startAt = it; picker = null },
        )
        Picker.END -> DateTimePickerDialog(
            initial = endAt ?: roundMinute((startAt ?: now) + 60 * 60_000L),
            onDismiss = { picker = null },
            onConfirm = { endAt = it; picker = null },
        )
        Picker.DAILY_START -> TimePickerDialog(
            title = "시작 시각",
            initialMinute = dailyStart,
            onDismiss = { picker = null },
            onConfirm = { dailyStart = it; picker = null },
        )
        Picker.DAILY_END -> TimePickerDialog(
            title = "종료 시각",
            initialMinute = dailyEnd ?: (dailyStart + 60) % (24 * 60),
            onDismiss = { picker = null },
            onConfirm = { dailyEnd = it; picker = null },
        )
        null -> {}
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("삭제할까요?") },
            text = { Text("'${initial.title}'을(를) 삭제하면 되돌릴 수 없어요.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("삭제", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun DayToggle(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (selected) c.primary else c.surfaceContainerHighest,
        contentColor = if (selected) c.onPrimary else c.onSurfaceVariant,
        modifier = modifier.aspectRatio(1f).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else null)
        }
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) }, shape = CircleShape)
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Rounded.Close, "지우기", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun summary(repeat: Boolean, days: Set<Int>, dailyStart: Int, dailyEnd: Int?, startAt: Long?, endAt: Long?): String {
    if (repeat) {
        val end = dailyEnd?.let { "${Format.time(it)}까지" } ?: "자정까지"
        return "${Format.days(days)} ${Format.time(dailyStart)}부터 $end 알림창에 표시돼요"
    }
    val start = startAt?.let { "${Format.dateTime(it)}부터" } ?: "지금부터"
    val end = endAt?.let { "${Format.dateTime(it)}까지" } ?: "완료할 때까지"
    return "$start $end 알림창에 표시돼요"
}

private fun roundMinute(ms: Long) = ms - ms % 60_000L

private fun tomorrowAt(minute: Int): Long {
    val zone = ZoneId.systemDefault()
    return LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
}
