package com.pintodo.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

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

enum class RepeatType {
    NONE,       // 한 번
    WEEKLY,     // 요일 반복 (repeatInterval 주마다: 1=매주, 2=격주)
    MONTHLY,    // 매월 monthDay일 (그 달에 없는 날이면 말일)
    EVERY_DAYS, // repeatInterval 일마다
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

    // 반복. repeatDays: 1=월 ... 7=일 (DayOfWeek.value)
    val repeatDays: Set<Int> = emptySet(),
    val repeatType: RepeatType = if (repeatDays.isEmpty()) RepeatType.NONE else RepeatType.WEEKLY,
    val repeatInterval: Int = 1,
    val monthDay: Int = 1,
    val repeatAnchor: Long? = null, // 격주·n일마다의 기준 날짜 (LocalDate.toEpochDay). null이면 만든 날
    val dailyStart: Int = 9 * 60,   // 하루 중 분
    val dailyEnd: Int? = null,      // null 이면 자정까지. dailyStart 이하면 다음 날로 넘어감

    val createdAt: Long = System.currentTimeMillis(),
    val doneAt: Long? = null,
    val snoozeUntil: Long? = null,
    val hiddenKey: Long? = null,    // 숨긴 회차의 window.start
    val alertedKey: Long? = null,   // 이미 소리/진동을 낸 회차 키
    val wearDismissedKey: Long? = null, // 밀어서 지운 회차 키 → 이 회차는 워치로 다시 보내지 않음
) {
    val isRepeat get() = repeatType != RepeatType.NONE

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
            .filter { occursOn(it) }
            .map { repeatWindow(it) }
            .lastOrNull { now >= it.start && now < it.end!! }
    }

    /** now 이후 다음 표시 시작 시각 */
    fun nextStart(now: Long): Long? {
        if (doneAt != null || !notify) return null
        if (!isRepeat) return (startAt ?: createdAt).takeIf { it > now }
        val today = localDate(now)
        // 가장 긴 주기(매월, 4주마다, 30일마다)도 두 달 안에 한 번은 돌아옴
        return (0L..62L).asSequence()
            .map { today.plusDays(it) }
            .filter { occursOn(it) }
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

    /** 이 날짜에 회차가 시작되는지 */
    fun occursOn(date: LocalDate): Boolean {
        val anchor = repeatAnchor?.let(LocalDate::ofEpochDay) ?: localDate(createdAt)
        val n = repeatInterval.coerceAtLeast(1).toLong()
        return when (repeatType) {
            RepeatType.NONE -> false
            RepeatType.WEEKLY -> date.dayOfWeek.value in repeatDays &&
                ChronoUnit.WEEKS.between(monday(anchor), monday(date)).mod(n) == 0L
            RepeatType.MONTHLY -> date.dayOfMonth == minOf(monthDay, date.lengthOfMonth())
            RepeatType.EVERY_DAYS -> !date.isBefore(anchor) && (date.toEpochDay() - anchor.toEpochDay()).mod(n) == 0L
        }
    }

    private fun monday(d: LocalDate) = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

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
