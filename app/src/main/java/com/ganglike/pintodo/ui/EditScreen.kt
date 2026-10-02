@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ganglike.pintodo.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "닫기") } },
                title = { Text(if (isNew) "새 할 일" else "할 일 수정", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.DeleteOutline, "삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
            ) {
                if (error != null) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }
                PrimaryButton(if (isNew) "추가하기" else "저장하기", { onSave(build()) }, enabled = canSave)
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 제목 / 메모
            GroupCard {
                val focus = remember { FocusRequester() }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(title, { title = it }, "무엇을 해야 하나요?", Modifier.focusRequester(focus), imeAction = ImeAction.Next)
                    AppTextField(memo, { memo = it }, "메모 (선택)", singleLine = false, minLines = 2)
                }
                if (isNew) LaunchedEffect(Unit) { focus.requestFocus() }
            }

            Section("알림") {
                SettingRow(
                    title = "알림창에 고정",
                    value = if (pinned) "밀어서 지워도 다시 나타나요" else "밀어서 지우면 이번엔 숨겨요",
                    icon = Icons.Rounded.PushPin,
                    onClick = { pinned = !pinned },
                    trailing = { Switch(checked = pinned, onCheckedChange = { pinned = it }) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.padding(16.dp)) {
                    Text("알림 방식", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                    AlertModePicker(alertMode) { alertMode = it }
                }
            }

            Section("일정") {
                SegmentTabs(listOf("한 번", "반복"), if (repeat) 1 else 0, { repeat = it == 1 }, Modifier.padding(horizontal = 16.dp, vertical = 10.dp))

                if (!repeat) {
                    SettingRow(
                        title = "시작",
                        value = startAt?.let { Format.dateTime(it) } ?: "지금 바로",
                        icon = Icons.Rounded.Alarm,
                        valueColor = MaterialTheme.colorScheme.primary,
                        onClick = { picker = Picker.START },
                    )
                    FlowRow(
                        Modifier.padding(start = 70.dp, end = 16.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SoftChip("지금", startAt == null) { startAt = null }
                        SoftChip("30분 뒤") { startAt = roundMinute(now + 30 * 60_000L) }
                        SoftChip("1시간 뒤") { startAt = roundMinute(now + 60 * 60_000L) }
                        SoftChip("내일 아침") { startAt = tomorrowAt(9 * 60) }
                    }
                    SettingRow(
                        title = "종료",
                        value = endAt?.let { Format.dateTime(it) } ?: "완료할 때까지",
                        icon = Icons.Rounded.Flag,
                        valueColor = MaterialTheme.colorScheme.primary,
                        onClick = { picker = Picker.END },
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (1..7).forEach { d ->
                            DayToggle(Format.dayName(d), d in days, Modifier.weight(1f)) {
                                days = if (d in days) days - d else (days + d).sorted()
                            }
                        }
                    }
                    FlowRow(
                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SoftChip("매일", days.toSet() == (1..7).toSet()) { days = (1..7).toList() }
                        SoftChip("평일", days.toSet() == (1..5).toSet()) { days = (1..5).toList() }
                        SoftChip("주말", days.toSet() == setOf(6, 7)) { days = listOf(6, 7) }
                    }
                    SettingRow(
                        title = "시작 시각",
                        value = Format.time(dailyStart),
                        icon = Icons.Rounded.Alarm,
                        valueColor = MaterialTheme.colorScheme.primary,
                        onClick = { picker = Picker.DAILY_START },
                    )
                    SettingRow(
                        title = "종료 시각",
                        value = dailyEnd?.let { Format.time(it) + if (it <= dailyStart) " (다음 날)" else "" } ?: "자정까지",
                        icon = Icons.Rounded.Flag,
                        valueColor = MaterialTheme.colorScheme.primary,
                        onClick = { picker = Picker.DAILY_END },
                    )
                }

                Text(
                    summary(repeat, days.toSet(), dailyStart, dailyEnd, startAt, endAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    when (picker) {
        Picker.START -> DateTimeSheet(
            title = "언제부터 알릴까요?",
            initial = startAt ?: roundMinute(now + 60 * 60_000L),
            onDismiss = { picker = null },
            onConfirm = { startAt = it; picker = null },
            clearLabel = "지금 바로",
            onClear = { startAt = null; picker = null },
        )
        Picker.END -> DateTimeSheet(
            title = "언제까지 알릴까요?",
            initial = endAt ?: roundMinute((startAt ?: now) + 60 * 60_000L),
            onDismiss = { picker = null },
            onConfirm = { endAt = it; picker = null },
            clearLabel = "완료할 때까지",
            onClear = { endAt = null; picker = null },
        )
        Picker.DAILY_START -> TimeSheet(
            title = "매일 몇 시부터?",
            initialMinute = dailyStart,
            onDismiss = { picker = null },
            onConfirm = { dailyStart = it; picker = null },
        )
        Picker.DAILY_END -> TimeSheet(
            title = "몇 시까지?",
            initialMinute = dailyEnd ?: (dailyStart + 60) % (24 * 60),
            onDismiss = { picker = null },
            onConfirm = { dailyEnd = it; picker = null },
            clearLabel = "자정까지",
            onClear = { dailyEnd = null; picker = null },
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
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (selected) c.primary else c.surfaceVariant)
            .clickable(onClick = onClick),
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
