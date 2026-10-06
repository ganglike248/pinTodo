@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.pintodo.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Notifications
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pintodo.data.DateParser
import com.pintodo.data.RepeatType
import com.pintodo.data.Todo
import java.time.LocalDate
import java.time.ZoneId

private enum class Field { START, END }

@Composable
fun EditScreen(
    initial: Todo,
    isNew: Boolean,
    onClose: () -> Unit,
    onSave: (Todo) -> Unit,
    onDelete: () -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
) {
    val openedAt = remember { System.currentTimeMillis() }
    val today = remember { LocalDate.now() }
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var memo by rememberSaveable { mutableStateOf(initial.memo) }
    var notify by rememberSaveable { mutableStateOf(initial.notify) }
    var pinned by rememberSaveable { mutableStateOf(initial.pinned) }
    var alertMode by rememberSaveable { mutableStateOf(initial.alertMode) }
    var repeat by rememberSaveable { mutableStateOf(initial.isRepeat) }
    var startAt by rememberSaveable { mutableStateOf(initial.startAt) }
    var endAt by rememberSaveable { mutableStateOf(initial.endAt) }
    var kind by rememberSaveable { mutableStateOf(initial.repeatType.takeIf { it != RepeatType.NONE } ?: RepeatType.WEEKLY) }
    var days by rememberSaveable { mutableStateOf(initial.repeatDays.ifEmpty { (1..5).toSet() }.toList()) }
    var weeks by rememberSaveable { mutableIntStateOf(if (initial.repeatType == RepeatType.WEEKLY) initial.repeatInterval else 1) }
    var everyDays by rememberSaveable { mutableIntStateOf(if (initial.repeatType == RepeatType.EVERY_DAYS) initial.repeatInterval else 2) }
    var monthDay by rememberSaveable { mutableIntStateOf(if (initial.repeatType == RepeatType.MONTHLY) initial.monthDay else today.dayOfMonth) }
    var dailyStart by rememberSaveable { mutableStateOf(initial.dailyStart) }
    var dailyEnd by rememberSaveable { mutableStateOf(initial.dailyEnd) }
    var field by rememberSaveable { mutableStateOf(Field.START) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    // 제목·메모의 날짜/시각을 읽어 일정에 자동 반영. 일정을 직접 만지면 그때부터는 손대지 않음
    var manualSchedule by rememberSaveable { mutableStateOf(false) }
    var autoMatched by rememberSaveable { mutableStateOf<String?>(null) }
    var dismissedMatch by rememberSaveable { mutableStateOf<String?>(null) }
    // 이미 있는 할 일은 원래 글에 있던 날짜로 일정을 덮어쓰지 않음 (새 할 일은 넘겨받은 제목도 읽음)
    val initialMatch = remember { if (isNew) null else DateParser.parse("${initial.title}\n${initial.memo}", openedAt)?.matched }
    val parsed = remember(title, memo) { DateParser.parse("$title\n$memo", System.currentTimeMillis()) }
    fun manual(change: () -> Unit) { manualSchedule = true; change() }
    fun revertAuto() {
        repeat = initial.isRepeat; startAt = initial.startAt; endAt = initial.endAt
        autoMatched = null
    }
    LaunchedEffect(parsed?.matched) {
        val p = parsed
        if (manualSchedule || !notify) return@LaunchedEffect
        if (p == null) { if (autoMatched != null) revertAuto(); return@LaunchedEffect }
        if (p.matched == initialMatch || p.matched == dismissedMatch || p.matched == autoMatched) return@LaunchedEffect
        repeat = false
        startAt = p.startAt
        endAt = p.endAt
        field = if (p.startAt == null) Field.END else Field.START
        autoMatched = p.matched
    }

    val now = System.currentTimeMillis()
    val error = when {
        !notify -> null
        repeat && kind == RepeatType.WEEKLY && days.isEmpty() -> "요일을 하나 이상 선택해 주세요"
        repeat && dailyEnd == dailyStart -> "시작과 종료 시각이 같아요"
        !repeat && endAt != null && endAt!! <= (startAt ?: now) -> "종료가 시작보다 늦어야 해요"
        else -> null
    }
    val canSave = title.isNotBlank() && error == null

    fun build(): Todo {
        // 알림을 끄면 일정은 의미가 없으므로 비움 (완료하면 바로 기록으로)
        val type = if (repeat && notify) kind else RepeatType.NONE
        val interval = when (type) {
            RepeatType.WEEKLY -> weeks
            RepeatType.EVERY_DAYS -> everyDays
            else -> 1
        }
        // 격주·n일마다는 처음 정한 날을 기준으로 셈. 규칙을 바꾸면 오늘부터 다시 셈
        val sameRule = type == initial.repeatType && interval == initial.repeatInterval
        val anchor = if (interval > 1) (if (sameRule) initial.repeatAnchor else null) ?: today.toEpochDay() else null
        val base = initial.copy(
            title = title.trim(),
            memo = memo.trim(),
            notify = notify,
            pinned = pinned,
            alertMode = alertMode,
            startAt = if (repeat || !notify) null else startAt,
            endAt = if (repeat || !notify) null else endAt,
            repeatType = type,
            repeatDays = if (type == RepeatType.WEEKLY) days.toSet() else emptySet(),
            repeatInterval = interval,
            monthDay = monthDay,
            repeatAnchor = anchor,
            dailyStart = dailyStart,
            dailyEnd = dailyEnd,
        )
        val scheduleChanged = base.notify != initial.notify || base.startAt != initial.startAt || base.endAt != initial.endAt ||
            base.repeatType != initial.repeatType || base.repeatDays != initial.repeatDays ||
            base.repeatInterval != initial.repeatInterval || base.monthDay != initial.monthDay ||
            base.repeatAnchor != initial.repeatAnchor || base.dailyStart != initial.dailyStart ||
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

    // 저장하지 않은 변경이 있으면 닫기 전에 확인
    val dirty = content(build()) != content(initial)
    LaunchedEffect(dirty) { onDirtyChange(dirty) }
    fun requestClose() = if (dirty) confirmDiscard = true else onClose()
    BackHandler { requestClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = ::requestClose) { Icon(Icons.Rounded.Close, "닫기") } },
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
                    AppTextField(title, { title = it }, "무엇을 해야 하나요?  예) 내일 오후 3시 회의", Modifier.focusRequester(focus), imeAction = ImeAction.Next)
                    AppTextField(memo, { memo = it }, "메모 (선택)", singleLine = false, minLines = 2)
                    val auto = autoMatched
                    if (auto != null && parsed?.matched == auto) {
                        AutoScheduleHint(auto) {
                            dismissedMatch = auto
                            revertAuto()
                        }
                    }
                }
                if (isNew) LaunchedEffect(Unit) { focus.requestFocus() }
            }

            Section("알림") {
                SettingRow(
                    title = "알림 받기",
                    value = if (notify) "알림창에 표시해요" else "알림 없이 목록과 위젯에만 보여요",
                    icon = Icons.Rounded.Notifications,
                    onClick = { notify = !notify },
                    trailing = { Switch(checked = notify, onCheckedChange = { notify = it }) },
                )
                if (notify) {
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
            }

            if (notify) Section("일정") {
                SegmentTabs(listOf("한 번", "반복"), if (repeat) 1 else 0, { manual { repeat = it == 1 } }, Modifier.padding(horizontal = 16.dp, vertical = 10.dp))

                if (!repeat) {
                    // 시작/종료 칸을 고르면 아래에 날짜·시각 휠이 바로 펼쳐져 있음
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldCell("시작", startAt?.let { Format.dateTime(it) } ?: "지금 바로", field == Field.START, Modifier.weight(1f)) { field = Field.START }
                        FieldCell("종료", endAt?.let { Format.dateTime(it) } ?: "완료할 때까지", field == Field.END, Modifier.weight(1f)) { field = Field.END }
                    }
                    Box(Modifier.padding(16.dp)) {
                        key(field) {
                            when (field) {
                                Field.START -> InlineDateTime(
                                    value = startAt,
                                    fallback = roundMinute(openedAt + 60 * 60_000L),
                                    emptyLabel = "지금 바로",
                                    onChange = { manual { startAt = it } },
                                    quick = listOf(
                                        "30분 뒤" to roundMinute(openedAt + 30 * 60_000L),
                                        "1시간 뒤" to roundMinute(openedAt + 60 * 60_000L),
                                        "내일 아침" to tomorrowAt(9 * 60),
                                    ),
                                )
                                Field.END -> InlineDateTime(
                                    value = endAt,
                                    fallback = roundMinute((startAt ?: openedAt) + 60 * 60_000L),
                                    emptyLabel = "완료할 때까지",
                                    onChange = { manual { endAt = it } },
                                )
                            }
                        }
                    }
                } else {
                    FlowRow(
                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SoftChip("매주", kind == RepeatType.WEEKLY && weeks == 1) { manual { kind = RepeatType.WEEKLY; weeks = 1 } }
                        SoftChip("격주", kind == RepeatType.WEEKLY && weeks == 2) { manual { kind = RepeatType.WEEKLY; weeks = 2 } }
                        SoftChip("매월", kind == RepeatType.MONTHLY) { manual { kind = RepeatType.MONTHLY } }
                        SoftChip("며칠마다", kind == RepeatType.EVERY_DAYS) { manual { kind = RepeatType.EVERY_DAYS } }
                    }
                    when (kind) {
                        RepeatType.MONTHLY -> Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            WheelRow {
                                WheelPicker(31, monthDay - 1, { manual { monthDay = it + 1 } }, { "매월 " + Format.monthDay(it + 1) },
                                    Modifier.width(200.dp), description = "매월 며칠")
                            }
                            Hint("말일을 고르면 30일·28일까지인 달에도 마지막 날에 알려요")
                        }
                        RepeatType.EVERY_DAYS -> Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Stepper("${everyDays}일마다", onMinus = { manual { everyDays = (everyDays - 1).coerceAtLeast(2) } },
                                onPlus = { manual { everyDays = (everyDays + 1).coerceAtMost(30) } })
                        }
                        else -> {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                (1..7).forEach { d ->
                                    DayToggle(Format.dayName(d), d in days, Modifier.weight(1f)) {
                                        manual { days = if (d in days) days - d else (days + d).sorted() }
                                    }
                                }
                            }
                            FlowRow(
                                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SoftChip("매일", days.toSet() == (1..7).toSet()) { manual { days = (1..7).toList() } }
                                SoftChip("평일", days.toSet() == (1..5).toSet()) { manual { days = (1..5).toList() } }
                                SoftChip("주말", days.toSet() == setOf(6, 7)) { manual { days = listOf(6, 7) } }
                            }
                        }
                    }
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldCell("시작 시각", Format.time(dailyStart), field == Field.START, Modifier.weight(1f)) { field = Field.START }
                        FieldCell(
                            "종료 시각",
                            dailyEnd?.let { Format.time(it) + if (it <= dailyStart) " (다음 날)" else "" } ?: "자정까지",
                            field == Field.END, Modifier.weight(1f),
                        ) { field = Field.END }
                    }
                    Box(Modifier.padding(16.dp)) {
                        key(field) {
                            when (field) {
                                Field.START -> InlineTime(dailyStart, dailyStart, null) { manual { dailyStart = it ?: dailyStart } }
                                Field.END -> InlineTime(dailyEnd, (dailyStart + 60) % (24 * 60), "자정까지") { manual { dailyEnd = it } }
                            }
                        }
                    }
                }

                Text(
                    summary(build()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
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
    if (confirmDiscard) {
        DiscardDialog(onKeep = { confirmDiscard = false }, onDiscard = { confirmDiscard = false; onClose() })
    }
}

/** 저장하지 않은 변경을 버릴지 묻기 */
@Composable
fun DiscardDialog(onKeep: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text("저장하지 않고 나갈까요?") },
        text = { Text("바꾼 내용이 사라져요.") },
        confirmButton = { TextButton(onClick = onDiscard) { Text("나가기", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onKeep) { Text("계속 편집") } },
    )
}

/** 비교용: 사용자가 바꿀 수 있는 내용만 (화면에 안 보이는 기본값 차이는 무시) */
private fun content(t: Todo): List<Any?> = listOf(
    t.title.trim(), t.memo.trim(), t.notify, t.pinned, t.alertMode, t.startAt, t.endAt, t.repeatType,
    t.repeatDays.takeIf { t.repeatType == RepeatType.WEEKLY },
    t.repeatInterval.takeIf { t.isRepeat },
    t.monthDay.takeIf { t.repeatType == RepeatType.MONTHLY },
    t.dailyStart.takeIf { t.isRepeat },
    t.dailyEnd.takeIf { t.isRepeat },
)

@Composable
private fun AutoScheduleHint(matched: String, onUndo: () -> Unit) {
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
private fun FieldCell(label: String, value: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
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
private fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
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
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
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

private fun summary(t: Todo): String {
    if (t.isRepeat) {
        val end = t.dailyEnd?.let { "${Format.time(it)}까지" } ?: "자정까지"
        val from = t.repeatAnchor?.let { LocalDate.ofEpochDay(it) }?.let { d ->
            if (t.repeatType == RepeatType.WEEKLY) " (${Format.date(d)}이 있는 주부터 셈)" else " (${Format.date(d)}부터 셈)"
        } ?: ""
        return "${Format.repeat(t)} ${Format.time(t.dailyStart)}부터 $end 알림창에 표시돼요$from"
    }
    val start = t.startAt?.let { "${Format.dateTime(it)}부터" } ?: "지금부터"
    val end = t.endAt?.let { "${Format.dateTime(it)}까지" } ?: "완료할 때까지"
    return "$start $end 알림창에 표시돼요"
}

private fun roundMinute(ms: Long) = ms - ms % 60_000L

private fun tomorrowAt(minute: Int): Long {
    val zone = ZoneId.systemDefault()
    return LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
}
