@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.pintodo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pintodo.data.DateParser
import com.pintodo.data.RepeatType
import com.pintodo.data.Todo
import java.time.LocalDate
import java.time.ZoneId

private enum class Field { START, END }

/** 일정 탭: 한 번 / 반복 / 알림 없이 */
private enum class Mode(val label: String) { ONCE("한 번"), REPEAT("반복"), NONE("알림 없이") }

/**
 * 추가·수정 화면. 자주 쓰는 것(제목, 일정)만 펼쳐 두고
 * 메모·체크리스트는 필요할 때 추가, 알림 방식은 한 줄 요약을 눌러 시트에서 바꾼다.
 */
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
    var items by rememberSaveable(stateSaver = CheckItemsSaver) { mutableStateOf(initial.items) }
    var color by rememberSaveable { mutableStateOf(initial.color) }
    var showMemo by rememberSaveable { mutableStateOf(initial.memo.isNotBlank()) }
    var showChecklist by rememberSaveable { mutableStateOf(initial.items.isNotEmpty()) }
    var notify by rememberSaveable { mutableStateOf(initial.notify) }
    var pinned by rememberSaveable { mutableStateOf(initial.pinned) }
    var alertMode by rememberSaveable { mutableStateOf(initial.alertMode) }
    var remindEvery by rememberSaveable { mutableStateOf(initial.remindEvery) }
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
    var repeatEnd by rememberSaveable {
        mutableStateOf(
            when {
                initial.repeatCount != null -> RepeatEnd.COUNT
                initial.repeatUntil != null -> RepeatEnd.DATE
                else -> RepeatEnd.NEVER
            }
        )
    }
    var untilDay by rememberSaveable { mutableStateOf(initial.repeatUntil ?: today.plusMonths(1).toEpochDay()) }
    var count by rememberSaveable { mutableIntStateOf(initial.repeatCount ?: 10) }
    var field by rememberSaveable { mutableStateOf(Field.START) }
    var alertSheet by remember { mutableStateOf(false) }
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
        repeat && repeatEnd == RepeatEnd.DATE && untilDay < today.toEpochDay() -> "반복 종료일이 지났어요"
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
        val until = if (type != RepeatType.NONE && repeatEnd == RepeatEnd.DATE) untilDay else null
        val times = if (type != RepeatType.NONE && repeatEnd == RepeatEnd.COUNT) count else null
        // 격주·n일마다·횟수는 처음 정한 날을 기준으로 셈. 규칙을 바꾸면 오늘부터 다시 셈
        val sameRule = type == initial.repeatType && interval == initial.repeatInterval && times == initial.repeatCount
        val anchor = if (interval > 1 || times != null) (if (sameRule) initial.repeatAnchor else null) ?: today.toEpochDay() else null
        val base = initial.copy(
            title = title.trim(),
            memo = if (showMemo) memo.trim() else "",
            items = if (showChecklist) items.filter { it.text.isNotBlank() } else emptyList(),
            color = color,
            notify = notify,
            pinned = pinned,
            alertMode = alertMode,
            remindEvery = if (notify) remindEvery else null,
            startAt = if (repeat || !notify) null else startAt,
            endAt = if (repeat || !notify) null else endAt,
            repeatType = type,
            repeatDays = if (type == RepeatType.WEEKLY) days.toSet() else emptySet(),
            repeatInterval = interval,
            monthDay = monthDay,
            repeatAnchor = anchor,
            repeatUntil = until,
            repeatCount = times,
            dailyStart = dailyStart,
            dailyEnd = dailyEnd,
        )
        val scheduleChanged = base.notify != initial.notify || base.startAt != initial.startAt || base.endAt != initial.endAt ||
            base.repeatType != initial.repeatType || base.repeatDays != initial.repeatDays ||
            base.repeatInterval != initial.repeatInterval || base.monthDay != initial.monthDay ||
            base.repeatAnchor != initial.repeatAnchor || base.repeatUntil != initial.repeatUntil ||
            base.repeatCount != initial.repeatCount || base.dailyStart != initial.dailyStart ||
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
    val draft = build()
    val dirty = content(draft) != content(initial)
    LaunchedEffect(dirty) { onDirtyChange(dirty) }
    fun requestClose() = if (dirty) confirmDiscard = true else onClose()
    BackHandler { requestClose() }

    val mode = when {
        !notify -> Mode.NONE
        repeat -> Mode.REPEAT
        else -> Mode.ONCE
    }

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
                // 지금 고른 일정을 한 줄로 (스크롤하지 않아도 확인)
                Text(
                    error ?: summary(draft),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
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
            // ── 내용: 제목 + (메모) + (체크리스트) + 색 라벨 ──
            GroupCard {
                val focus = remember { FocusRequester() }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(title, { title = it }, "무엇을 해야 하나요?  예) 내일 오후 3시 회의", Modifier.focusRequester(focus), imeAction = ImeAction.Next)
                    if (showMemo) AppTextField(memo, { memo = it }, "메모", singleLine = false, minLines = 2)
                    if (showChecklist) ChecklistEditor(items) { items = it }
                    val auto = autoMatched
                    if (auto != null && parsed?.matched == auto) {
                        AutoScheduleHint(auto) {
                            dismissedMatch = auto
                            revertAuto()
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!showMemo) AddChip("메모") { showMemo = true }
                        if (!showChecklist) AddChip("체크리스트") { showChecklist = true }
                    }
                    LabelColorPicker(color) { color = it }
                }
                if (isNew) LaunchedEffect(Unit) { focus.requestFocus() }
            }

            // ── 일정: 한 번 / 반복 / 알림 없이 ──
            Section("일정") {
                SegmentTabs(
                    Mode.entries.map { it.label }, mode.ordinal,
                    { i ->
                        manual {
                            when (Mode.entries[i]) {
                                Mode.ONCE -> { notify = true; repeat = false }
                                Mode.REPEAT -> { notify = true; repeat = true }
                                Mode.NONE -> notify = false
                            }
                        }
                    },
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )

                when (mode) {
                    Mode.NONE -> Text(
                        "알림창에는 뜨지 않고 목록과 위젯에만 보여요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                    )
                    Mode.ONCE -> {
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
                    }
                    Mode.REPEAT -> {
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
                            RepeatType.EVERY_DAYS -> Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Stepper("${everyDays}일마다", onMinus = { manual { everyDays = (everyDays - 1).coerceAtLeast(2) } },
                                    onPlus = { manual { everyDays = (everyDays + 1).coerceAtMost(30) } })
                            }
                            else -> DayPicker(days) { d -> manual { days = d } }
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
                        RepeatEndEditor(
                            mode = repeatEnd, untilDay = untilDay, count = count,
                            onMode = { manual { repeatEnd = it } },
                            onUntil = { manual { untilDay = it } },
                            onCount = { manual { count = it } },
                        )
                    }
                }
            }

            // ── 알림 방식: 한 줄 요약, 누르면 시트 ──
            if (notify) Section("알림") {
                SettingRow(
                    title = "알림 방식",
                    value = alertSummary(pinned, alertMode, remindEvery),
                    icon = alertMode.icon(),
                    onClick = { alertSheet = true },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (alertSheet) {
        AlertSheet(
            pinned, alertMode, remindEvery,
            onPinned = { pinned = it }, onMode = { alertMode = it }, onRemind = { remindEvery = it },
            onDismiss = { alertSheet = false },
        )
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
    t.title.trim(), t.memo.trim(), t.items, t.color, t.notify, t.pinned, t.alertMode, t.remindEvery, t.startAt, t.endAt, t.repeatType,
    t.repeatDays.takeIf { t.repeatType == RepeatType.WEEKLY },
    t.repeatInterval.takeIf { t.isRepeat },
    t.monthDay.takeIf { t.repeatType == RepeatType.MONTHLY },
    t.repeatUntil, t.repeatCount,
    t.dailyStart.takeIf { t.isRepeat },
    t.dailyEnd.takeIf { t.isRepeat },
)

private fun summary(t: Todo): String {
    if (!t.notify) return "알림 없이 목록과 위젯에만 보여요"
    if (t.isRepeat) {
        val end = t.dailyEnd?.let { "${Format.time(it)}까지" } ?: "자정까지"
        val until = Format.repeatEnd(t).let { if (it.isEmpty()) "" else " · $it" }
        return "${Format.repeat(t)} ${Format.time(t.dailyStart)}부터 $end$until"
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
