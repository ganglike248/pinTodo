@file:OptIn(ExperimentalMaterial3Api::class)

package com.pintodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import java.time.YearMonth
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs

private val WheelItemHeight = 48.dp
private const val WheelVisible = 5

/**
 * 돌려서 고르는 휠. 끝없이 이어지도록 같은 값을 여러 번 반복해 두고 가운데에서 시작한다. (loop=false면 한 번만)
 * 한 칸 넘어갈 때마다 햅틱으로 '드르륵' 느낌을 준다.
 * TalkBack에서는 휠 하나가 한 항목으로 읽히고, '다음 값/이전 값' 동작으로 바꿀 수 있다.
 */
@Composable
fun WheelPicker(
    count: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = WheelItemHeight,
    loop: Boolean = true,
    description: String = "",
) {
    val loops = if (loop) 400 else 1
    val half = WheelVisible / 2
    val start = remember { (loops / 2) * count + value }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val fling = rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Center)
    val haptic = LocalHapticFeedback.current
    val onChange by rememberUpdatedState(onValueChange)
    val scope = rememberCoroutineScope()
    // 바깥 값에 맞춰 이동하는 중에는 지나가는 값을 알리지 않음 (중간 값으로 되돌아가는 문제 방지)
    var syncing by remember { mutableStateOf(false) }

    // 화면 가운데에 가장 가까운 칸
    val center by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: start
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { center }.drop(1).collect {
            if (syncing) return@collect
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            onChange(it % count)
        }
    }
    // 바깥에서 값이 바뀌면(빠른 선택 등) 그 값으로 이동
    LaunchedEffect(value) {
        if (center % count != value && !state.isScrollInProgress) {
            syncing = true
            try {
                state.animateScrollToItem(center - center % count + value)
            } finally {
                syncing = false
            }
        }
    }

    fun step(delta: Int): Boolean {
        val target = center + delta
        if (target !in 0 until count * loops) return false
        scope.launch { state.animateScrollToItem(target) }
        return true
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = itemHeight * half),
        horizontalAlignment = Alignment.CenterHorizontally,
        // 휠 끝(예: 오전·오후)에 닿고 남은 드래그·플링이 화면 스크롤로 넘어가지 않게 여기서 소비
        modifier = modifier.height(itemHeight * WheelVisible).nestedScroll(KeepScrollInside).clearAndSetSemantics {
            contentDescription = description
            stateDescription = label(center % count)
            customActions = listOf(
                CustomAccessibilityAction("다음 값") { step(1) },
                CustomAccessibilityAction("이전 값") { step(-1) },
            )
        },
    ) {
        items(count * loops) { index ->
            val dist = abs(index - center)
            Box(Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    label(index % count),
                    fontSize = if (dist == 0) 26.sp else 21.sp,
                    fontWeight = if (dist == 0) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(
                        when (dist) {
                            0 -> 1f
                            1 -> 0.4f
                            else -> 0.15f
                        }
                    ),
                )
            }
        }
    }
}

private object KeepScrollInside : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available
    override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
}

/** 가운데 선택 띠 위에 휠들을 나란히 */
@Composable
fun WheelRow(content: @Composable RowScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().height(WheelItemHeight)
                .clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

/** 오전/오후 : 시 : 분 휠 (12시간제) */
@Composable
fun TimeWheel(minuteOfDay: Int, onChange: (Int) -> Unit) {
    var pm by remember { mutableIntStateOf(if (minuteOfDay / 60 % 24 >= 12) 1 else 0) }
    var hour by remember { mutableIntStateOf(minuteOfDay / 60 % 12) }   // 0 → 12시
    var minute by remember { mutableIntStateOf(minuteOfDay % 60) }
    LaunchedEffect(minuteOfDay) {
        pm = if (minuteOfDay / 60 % 24 >= 12) 1 else 0
        hour = minuteOfDay / 60 % 12
        minute = minuteOfDay % 60
    }
    fun emit() = onChange((pm * 12 + hour) * 60 + minute)
    WheelRow {
        WheelPicker(2, pm, { pm = it; emit() }, { if (it == 0) "오전" else "오후" }, Modifier.width(84.dp), loop = false, description = "오전 오후")
        WheelPicker(12, hour, { hour = it; emit() }, { if (it == 0) "12" else "$it" }, Modifier.width(64.dp), description = "시")
        Text(":", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 2.dp))
        WheelPicker(60, minute, { minute = it; emit() }, { "%02d".format(it) }, Modifier.width(72.dp), description = "분")
    }
}

/** 일정 줄에서 지금 펼친 부분 */
enum class Part { DATE, TIME }

/**
 * 갤럭시 캘린더처럼 [시작 › 종료] 두 칸을 가로로 나란히. 각 칸은 머리글(라벨 + 배지) · 날짜 · 시각.
 * 날짜·시각을 누르면 두 칸 아래(전체 폭)에 달력·휠이 펼쳐짐 → [DateTimePanel]
 */
@Composable
fun RangeColumns(start: @Composable ColumnScope.() -> Unit, end: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, content = start)
        Icon(
            Icons.Rounded.ChevronRight, null,
            Modifier.padding(horizontal = 4.dp).size(24.dp).align(Alignment.CenterVertically),
            tint = MaterialTheme.colorScheme.outline,
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, content = end)
    }
}

/**
 * 한 칸(시작 또는 종료): 라벨 + 배지, 날짜, 시각. 날짜·시각은 늘 보이고,
 * 배지(지금 바로·완료할 때까지)가 켜져 있으면(value == null) 흐리게 보임. 배지를 끄면 보이던 날짜·시각으로 정해짐
 */
@Composable
fun ColumnScope.DateTimeEnd(
    label: String,
    value: Long?,
    shown: Long,
    badge: String,
    open: Part?,
    onOpen: (Part?) -> Unit,
    onBadge: (Boolean) -> Unit,
) {
    val z = Instant.ofEpochMilli(shown).atZone(ZoneId.systemDefault())
    fun toggle(p: Part) = onOpen(if (open == p) null else p)
    EndHeader(label) { ToggleBadge(badge, value == null, onBadge) }
    Spacer(Modifier.height(10.dp))
    PickValue(dateText(z.toLocalDate()), big = false, selected = open == Part.DATE, dim = value == null, "$label 날짜 바꾸기") { toggle(Part.DATE) }
    Spacer(Modifier.height(6.dp))
    PickValue(Format.time(z.hour * 60 + z.minute), big = true, selected = open == Part.TIME, dim = value == null, "$label 시간 바꾸기") { toggle(Part.TIME) }
}

/** 시각만 있는 칸 (반복 할 일의 하루 시간대). badge가 있으면 value == null일 때 켜짐 */
@Composable
fun ColumnScope.TimeEnd(
    label: String,
    value: Int?,
    shown: Int,
    badge: String?,
    open: Boolean,
    onOpen: (Boolean) -> Unit,
    onBadge: (Boolean) -> Unit = {},
    sub: String? = null,
) {
    EndHeader(label) { if (badge != null) ToggleBadge(badge, value == null, onBadge) }
    Spacer(Modifier.height(10.dp))
    PickValue(Format.time(shown), big = true, selected = open, dim = value == null, "$label 바꾸기") { onOpen(!open) }
    if (sub != null) Text(sub, modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** 펼친 칸의 달력 또는 휠 (칸 아래 전체 폭). 시각은 빠른 선택 + 휠, 날짜는 화면 안 달력(고르면 시각 휠로 넘어감) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateTimePanel(
    open: Part?,
    value: Long?,
    shown: Long,
    onChange: (Long) -> Unit,
    onClose: () -> Unit,
    onShowTime: () -> Unit,
    quick: List<Pair<String, Long>> = emptyList(),
    minDate: LocalDate? = null,
) {
    val zone = ZoneId.systemDefault()
    val z = Instant.ofEpochMilli(shown).atZone(zone)
    val date = z.toLocalDate()
    val minute = z.hour * 60 + z.minute
    fun of(d: LocalDate, m: Int) = d.atStartOfDay(zone).plusMinutes(m.toLong()).toInstant().toEpochMilli()
    Expand(open == Part.TIME) {
        Column(Modifier.padding(top = 16.dp)) {
            if (quick.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.forEach { (text, ms) -> QuickChip(text, value == ms) { onChange(ms); onClose() } }
                }
                Spacer(Modifier.height(12.dp))
            }
            Box(Modifier.alpha(if (value == null) 0.4f else 1f)) {
                TimeWheel(minute) { m -> if (value != null || m != minute) onChange(of(date, m)) }
            }
        }
    }
    Expand(open == Part.DATE) {
        Box(Modifier.padding(top = 4.dp)) {
            InlineCalendar(date, minDate) { onChange(of(it, minute)); onShowTime() }   // 날짜를 고르면 이어서 시각 휠
        }
    }
}

/** 시각 휠만 펼치는 패널 (반복 할 일). value == null이면 흐리게 shown을 보여주고, 돌리면 그 시각으로 정해짐 */
@Composable
fun TimePanel(open: Boolean, value: Int?, shown: Int, onChange: (Int) -> Unit) {
    Expand(open) {
        Box(Modifier.padding(top = 12.dp).alpha(if (value == null) 0.4f else 1f)) {
            TimeWheel(shown) { m -> if (value != null || m != shown) onChange(m) }
        }
    }
}

/** 칸 머리글: 라벨 + (배지). 배지가 없는 칸과 높이를 맞춤 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EndHeader(label: String, badge: @Composable () -> Unit) {
    // 좁은 화면·큰 글씨에서 배지가 잘리지 않게, 자리가 없으면 배지는 다음 줄로
    FlowRow(
        Modifier.heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        badge()
    }
}

/** 켜고 끄는 작은 배지 (지금 바로 · 완료할 때까지 · 자정까지). 켜짐=파란 채움, 꺼짐=테두리 */
@Composable
private fun ToggleBadge(text: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(percent = 50)
    val base = MaterialTheme.typography.labelMedium
    // 칸보다 길면 글자를 줄여 한 줄에 (좁은 화면·큰 글씨)
    BasicText(
        text,
        style = base.copy(fontWeight = FontWeight.Bold, color = if (on) c.onPrimary else c.onSurfaceVariant),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = base.fontSize, stepSize = 0.5.sp),
        modifier = Modifier
            .clip(shape)
            .background(if (on) c.primary else c.surface)
            .border(1.dp, if (on) c.primary else c.outline.copy(alpha = 0.5f), shape)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** 빠른 선택(30분 뒤 등) 작은 칩. SoftChip과 같은 모양(안 고름=테두리, 고름=파란 채움)에 크기만 작게 */
@Composable
private fun QuickChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(percent = 50)
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (selected) c.onPrimary else c.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) c.primary else c.surface)
            .border(1.dp, if (selected) c.primary else c.outline.copy(alpha = 0.5f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** 누르면 아래에 달력·휠이 펼쳐지는 값. 펼친 동안은 파란색, 배지가 켜져 있으면 흐리게 */
@Composable
private fun PickValue(text: String, big: Boolean, selected: Boolean, dim: Boolean, clickLabel: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val base = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
    // 칸보다 길면 자르지 않고 글자를 줄여 한 줄에 맞춤 (좁은 화면·큰 글씨)
    BasicText(
        text,
        style = base.copy(fontWeight = if (big) FontWeight.Bold else FontWeight.Medium, color = if (selected) c.primary else c.onSurface),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = base.fontSize, stepSize = 0.5.sp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
            .semantics { stateDescription = if (selected) "펼침" else "접힘" }
            .alpha(if (dim && !selected) 0.4f else 1f)
            .padding(vertical = 4.dp),
    )
}

/** 11/12(목) · 오늘 */
private fun dateText(date: LocalDate): String {
    val today = LocalDate.now()
    return Format.date(date) + when (date) {
        today -> " · 오늘"
        today.plusDays(1) -> " · 내일"
        else -> ""
    }
}

@Composable
private fun Expand(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) { content() }
}

/**
 * 화면에 펼쳐 두는 달력. 날짜를 누르면 바로 반영.
 * Material DatePicker는 너비가 360dp로 고정이라 좁은 화면·큰 글씨에서 토요일이 잘려서, 칸 너비에 맞춰 7등분하는 달력을 직접 그림.
 * 오늘(또는 minDate) 이전은 못 고름. 좌우 화살표나 옆으로 밀어서 달 이동
 */
@Composable
private fun InlineCalendar(initial: LocalDate, minDate: LocalDate?, onPick: (LocalDate) -> Unit) {
    val c = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val first = listOfNotNull(today, minDate).max()
    var month by remember { mutableStateOf(YearMonth.from(initial)) }
    val canPrev = month > YearMonth.from(first)
    var drag by remember { mutableFloatStateOf(0f) }
    val swipe = with(LocalDensity.current) { 56.dp.toPx() }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { _, dx -> drag += dx },
                    onDragEnd = {
                        if (drag < -swipe) month = month.plusMonths(1)
                        else if (drag > swipe && month > YearMonth.from(first)) month = month.minusMonths(1)
                    },
                )
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${month.year}년 ${month.monthValue}월",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            IconButton(onClick = { month = month.minusMonths(1) }, enabled = canPrev) {
                Icon(Icons.Rounded.ChevronLeft, "이전 달", tint = if (canPrev) c.onSurface else c.outline.copy(alpha = 0.4f))
            }
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.Rounded.ChevronRight, "다음 달", tint = c.onSurface)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val offset = month.atDay(1).dayOfWeek.value % 7   // 일요일 시작
        val length = month.lengthOfMonth()
        val rows = (offset + length + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = r * 7 + col - offset + 1
                    Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..length) {
                            val date = month.atDay(day)
                            DayCell(date, selected = date == initial, isToday = date == today, enabled = !date.isBefore(first)) { onPick(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, isToday: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    // 칸이 좁으면 원도 칸에 맞춰 작게 (최대 40dp)
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(minOf(40.dp, maxWidth - 2.dp, maxHeight))
                .clip(CircleShape)
                .background(if (selected) c.primary else Color.Transparent)
                .then(if (isToday && !selected) Modifier.border(1.dp, c.primary, CircleShape) else Modifier)
                .clickable(enabled = enabled, onClickLabel = "고르기", onClick = onClick)
                .semantics {
                    contentDescription = Format.date(date)
                    this.selected = selected
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    selected -> c.onPrimary
                    !enabled -> c.outline.copy(alpha = 0.5f)
                    isToday -> c.primary
                    else -> c.onSurface
                },
            )
        }
    }
}

/** 공통 하단 시트. content에 넘기는 hide(after)로 닫는 애니메이션 후 동작 실행 */
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(hide: (after: () -> Unit) -> Unit) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hide: (() -> Unit) -> Unit = { after ->
        scope.launch { state.hide() }.invokeOnCompletion { after() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier.padding(horizontal = Dimens.ScreenPadding).padding(bottom = 12.dp).navigationBarsPadding(),
        ) {
            content(hide)
        }
    }
}

@Composable
fun SheetTitle(text: String, sub: String? = null) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
    if (sub != null) {
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
fun CalendarDialog(initial: LocalDate, minDate: LocalDate? = null, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    // 오늘 이전은 못 고름. 종료 날짜는 시작 날짜 이전도 못 고름
    val first = listOfNotNull(LocalDate.now(), minDate).max()
    val todayUtc = first.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    // DatePicker는 UTC 자정 기준 millis를 사용
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPick(Instant.ofEpochMilli(state.selectedDateMillis ?: todayUtc).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text("선택") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}
