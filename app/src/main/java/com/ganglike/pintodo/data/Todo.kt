package com.ganglike.pintodo.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class AlertMode(val label: String) {
    SILENT("무음"),
    SOUND("소리"),
    VIBRATE("진동"),
    BOTH("소리+진동"),
}

enum class Status {
    SHOWING,    // 지금 알림창에 표시 중
    NO_ALERT,   // 알림 없이 목록·위젯에만 있는 할 일
    SNOOZED,    // 미뤄둠
    HIDDEN,     // 이번 회차 완료(반복) 또는 닫음(고정 안 함)
    SCHEDULED,  // 시작 전
    ENDED,      // 기간 종료
    DONE,       // 완료
}

/** 알림이 떠 있어야 하는 구간. end == null 이면 완료할 때까지 */
data class Window(val start: Long, val end: Long?)

data class Todo(
    val id: Int,
    val title: String,
    val memo: String = "",
    val notify: Boolean = true,     // false면 알림·예약 없이 목록에만
    val pinned: Boolean = true,
    val alertMode: AlertMode = AlertMode.BOTH,

    // 한 번: startAt == null 이면 만든 즉시, endAt == null 이면 완료할 때까지
    val startAt: Long? = null,
    val endAt: Long? = null,

    // 반복: repeatDays 가 비어 있으면 '한 번'. 1=월 ... 7=일 (DayOfWeek.value)
    val repeatDays: Set<Int> = emptySet(),
    val dailyStart: Int = 9 * 60,   // 하루 중 분
    val dailyEnd: Int? = null,      // null 이면 자정까지. dailyStart 이하면 다음 날로 넘어감

    val createdAt: Long = System.currentTimeMillis(),
    val doneAt: Long? = null,
    val snoozeUntil: Long? = null,
    val hiddenKey: Long? = null,    // 숨긴 회차의 window.start
    val alertedKey: Long? = null,   // 이미 소리/진동을 낸 회차 키
) {
    val isRepeat get() = repeatDays.isNotEmpty()

    /** now 시점에 해당하는 표시 구간 (없으면 null) */
    fun windowAt(now: Long): Window? {
        if (doneAt != null || !notify) return null
        if (!isRepeat) {
            val s = startAt ?: createdAt
            if (now < s || (endAt != null && now >= endAt)) return null
            return Window(s, endAt)
        }
        val today = localDate(now)
        // 자정을 넘기는 구간을 위해 어제 회차도 확인
        return listOf(today.minusDays(1), today)
            .filter { it.dayOfWeek.value in repeatDays }
            .map { repeatWindow(it) }
            .lastOrNull { now >= it.start && now < it.end!! }
    }

    /** now 이후 다음 표시 시작 시각 */
    fun nextStart(now: Long): Long? {
        if (doneAt != null || !notify) return null
        if (!isRepeat) return (startAt ?: createdAt).takeIf { it > now }
        val today = localDate(now)
        return (0L..7L).asSequence()
            .map { today.plusDays(it) }
            .filter { it.dayOfWeek.value in repeatDays }
            .map { repeatWindow(it).start }
            .firstOrNull { it > now }
    }

    fun status(now: Long): Status {
        if (doneAt != null) return Status.DONE
        if (!notify) return Status.NO_ALERT
        val w = windowAt(now) ?: return if (nextStart(now) != null) Status.SCHEDULED else Status.ENDED
        if (hiddenKey == w.start) return Status.HIDDEN
        if (snoozeUntil != null && now < snoozeUntil) return Status.SNOOZED
        return Status.SHOWING
    }

    /** 소리/진동은 이 키가 바뀔 때(새 회차, 미루기 종료)만 1회 */
    fun alertKey(now: Long): Long? {
        val w = windowAt(now) ?: return null
        return maxOf(w.start, snoozeUntil?.takeIf { it <= now } ?: 0L)
    }

    /** 다음으로 상태가 바뀌는 시각 → 이때 알람을 걸어 다시 동기화 */
    fun nextChange(now: Long): Long? = listOfNotNull(
        windowAt(now)?.end,
        snoozeUntil?.takeIf { it > now },
        nextStart(now),
    ).minOrNull()

    private fun repeatWindow(date: LocalDate): Window {
        val zone = ZoneId.systemDefault()
        val day = date.atStartOfDay(zone)
        val start = day.plusMinutes(dailyStart.toLong())
        val endMin = dailyEnd ?: (24 * 60)
        val end = if (endMin > dailyStart) day.plusMinutes(endMin.toLong())
        else date.plusDays(1).atStartOfDay(zone).plusMinutes(endMin.toLong())
        return Window(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
    }

    private fun localDate(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
}
