@file:OptIn(ExperimentalMaterial3Api::class)

package com.pintodo.ui

import androidx.compose.foundation.background
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
        modifier = modifier.height(itemHeight * WheelVisible).clearAndSetSemantics {
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

/**
 * 날짜 칩 + 시각 휠을 화면에 바로 펼쳐 둔 편집기 (시트를 열지 않음).
 * value == null이면 emptyLabel 상태이고 휠은 흐리게 fallback 시각을 보여준다. 휠을 돌리면 그 시각으로 정해진다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineDateTime(
    value: Long?,
    fallback: Long,
    emptyLabel: String,
    onChange: (Long?) -> Unit,
    quick: List<Pair<String, Long>> = emptyList(),
) {
    val zone = ZoneId.systemDefault()
    val shown = Instant.ofEpochMilli(value ?: fallback).atZone(zone)
    val date = shown.toLocalDate()
    val minute = shown.hour * 60 + shown.minute
    val today = LocalDate.now(zone)
    var showCalendar by remember { mutableStateOf(false) }
    fun of(d: LocalDate, m: Int) = d.atStartOfDay(zone).plusMinutes(m.toLong()).toInstant().toEpochMilli()

    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftChip(emptyLabel, value == null) { onChange(null) }
            SoftChip("오늘", value != null && date == today) { onChange(of(today, minute)) }
            SoftChip("내일", value != null && date == today.plusDays(1)) { onChange(of(today.plusDays(1), minute)) }
            val other = value != null && date != today && date != today.plusDays(1)
            SoftChip(if (other) Format.date(date) else "날짜 선택", other) { showCalendar = true }
            quick.forEach { (label, ms) -> SoftChip(label, value == ms) { onChange(ms) } }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.alpha(if (value == null) 0.4f else 1f)) {
            TimeWheel(minute) { m -> if (value != null || m != minute) onChange(of(date, m)) }
        }
    }

    if (showCalendar) {
        CalendarDialog(date, onDismiss = { showCalendar = false }) { onChange(of(it, minute)); showCalendar = false }
    }
}

/** 시각만 바로 펼친 편집기 (반복 할 일의 하루 시간대). value == null이면 emptyLabel 상태 */
@Composable
fun InlineTime(value: Int?, fallback: Int, emptyLabel: String?, onChange: (Int?) -> Unit) {
    Column {
        if (emptyLabel != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftChip(emptyLabel, value == null) { onChange(null) }
                SoftChip("시각 지정", value != null) { onChange(value ?: fallback) }
            }
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.alpha(if (value == null) 0.4f else 1f)) {
            TimeWheel(value ?: fallback) { m -> if (value != null || m != fallback) onChange(m) }
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
fun CalendarDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
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
