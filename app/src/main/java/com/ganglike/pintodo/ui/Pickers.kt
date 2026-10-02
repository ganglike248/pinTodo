@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganglike.pintodo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** 날짜 → 시간 순서로 고르는 다이얼로그 */
@Composable
fun DateTimePickerDialog(
    initial: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initialDt = Instant.ofEpochMilli(initial).atZone(zone)
    var date by remember { mutableStateOf<LocalDate?>(null) }

    if (date == null) {
        // DatePicker는 UTC 자정 기준 millis를 사용
        val todayUtc = LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialDt.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        date = Instant.ofEpochMilli(state.selectedDateMillis ?: todayUtc)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                    },
                ) { Text("다음") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    } else {
        TimePickerDialog(
            title = "시각 선택",
            initialMinute = initialDt.hour * 60 + initialDt.minute,
            onDismiss = onDismiss,
            onConfirm = { minute ->
                val ms = date!!.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
                onConfirm(ms)
            },
        )
    }
}

@Composable
fun TimePickerDialog(
    title: String,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60 % 24,
        initialMinute = initialMinute % 60,
        is24Hour = true,
    )
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 400.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("취소") }
                    TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("확인") }
                }
            }
        }
    }
}
