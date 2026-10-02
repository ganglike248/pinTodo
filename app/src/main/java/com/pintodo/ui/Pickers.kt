@file:OptIn(ExperimentalMaterial3Api::class)

package com.pintodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
 * 돌려서 고르는 휠. 끝없이 이어지도록 같은 값을 여러 번 반복해 두고 가운데에서 시작한다.
 * 한 칸 넘어갈 때마다 햅틱으로 '드르륵' 느낌을 준다.
 */
@Composable
fun WheelPicker(
    count: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = WheelItemHeight,
) {
    val loops = 400
    val half = WheelVisible / 2
    val start = remember { (loops / 2) * count + value }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val fling = rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Center)
    val haptic = LocalHapticFeedback.current
    val onChange by rememberUpdatedState(onValueChange)

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
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            onChange(it % count)
        }
    }
    // 바깥에서 값이 바뀌면(빠른 선택 등) 그 값으로 이동
    LaunchedEffect(value) {
        if (center % count != value && !state.isScrollInProgress) {
            state.animateScrollToItem(center - center % count + value)
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = itemHeight * half),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(itemHeight * WheelVisible),
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

/** 시 : 분 휠 */
@Composable
fun TimeWheel(minuteOfDay: Int, onChange: (Int) -> Unit) {
    var hour by remember { mutableIntStateOf(minuteOfDay / 60 % 24) }
    var minute by remember { mutableIntStateOf(minuteOfDay % 60) }
    LaunchedEffect(minuteOfDay) {
        hour = minuteOfDay / 60 % 24
        minute = minuteOfDay % 60
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // 가운데 선택 띠
        Box(
            Modifier.fillMaxWidth().height(WheelItemHeight)
                .clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            WheelPicker(24, hour, { hour = it; onChange(hour * 60 + minute) }, { "%02d".format(it) }, Modifier.width(88.dp))
            Text(":", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            WheelPicker(60, minute, { minute = it; onChange(hour * 60 + minute) }, { "%02d".format(it) }, Modifier.width(88.dp))
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

/** 시각만 고르는 시트 (반복 할 일의 하루 시간대) */
@Composable
fun TimeSheet(
    title: String,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    clearLabel: String? = null,
    onClear: (() -> Unit)? = null,
) {
    var minute by remember { mutableIntStateOf(initialMinute) }
    AppSheet(onDismiss) { hide ->
        SheetTitle(title)
        TimeWheel(minute) { minute = it }
        Spacer(Modifier.height(24.dp))
        SheetButtons(clearLabel, onClear?.let { { hide(it) } }) { hide { onConfirm(minute) } }
    }
}

/** 날짜(달력) + 시각(휠)을 고르는 시트 */
@Composable
fun DateTimeSheet(
    title: String,
    initial: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    clearLabel: String? = null,
    onClear: (() -> Unit)? = null,
) {
    val zone = ZoneId.systemDefault()
    val init = Instant.ofEpochMilli(initial).atZone(zone)
    var date by remember { mutableStateOf(init.toLocalDate()) }
    var minute by remember { mutableIntStateOf(init.hour * 60 + init.minute) }
    var showCalendar by remember { mutableStateOf(false) }
    val today = LocalDate.now(zone)
    val result = date.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()

    AppSheet(onDismiss) { hide ->
        SheetTitle(title, Format.dateTime(result))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftChip("오늘", date == today) { date = today }
            SoftChip("내일", date == today.plusDays(1)) { date = today.plusDays(1) }
            val other = date != today && date != today.plusDays(1)
            SoftChip(if (other) Format.date(date) else "날짜 선택", other) { showCalendar = true }
        }
        Spacer(Modifier.height(16.dp))
        TimeWheel(minute) { minute = it }
        Spacer(Modifier.height(24.dp))
        SheetButtons(clearLabel, onClear?.let { { hide(it) } }) { hide { onConfirm(result) } }
    }

    if (showCalendar) {
        CalendarDialog(date, onDismiss = { showCalendar = false }) { date = it; showCalendar = false }
    }
}

@Composable
private fun SheetButtons(clearLabel: String?, onClear: (() -> Unit)?, onConfirm: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (clearLabel != null && onClear != null) {
            SecondaryButton(clearLabel, onClear, Modifier.weight(1f))
        }
        PrimaryButton("확인", onConfirm, Modifier.weight(if (clearLabel != null) 1.4f else 1f))
    }
}

@Composable
private fun CalendarDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
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
