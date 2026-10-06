package com.pintodo.ui

import com.pintodo.data.RepeatType
import com.pintodo.data.Status
import com.pintodo.data.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Format {
    private const val DAY = 24 * 60 * 60_000L
    private val DAY_NAMES = listOf("월", "화", "수", "목", "금", "토", "일")

    /** 오전 9:00, 오후 12:30 (12시간제) */
    fun time(minuteOfDay: Int): String {
        val h = minuteOfDay / 60 % 24
        val m = minuteOfDay % 60
        return "${if (h < 12) "오전" else "오후"} ${if (h % 12 == 0) 12 else h % 12}:%02d".format(m)
    }

    /** 45분 / 2시간 / 1시간 20분 */
    fun duration(ms: Long): String {
        val min = ((ms + 59_999) / 60_000).coerceAtLeast(1)
        val h = min / 60
        val m = min % 60
        return when {
            h == 0L -> "${m}분"
            m == 0L || h >= 10 -> "${h}시간"
            else -> "${h}시간 ${m}분"
        }
    }

    fun dateTime(ms: Long): String {
        val zone = ZoneId.systemDefault()
        val dt = Instant.ofEpochMilli(ms).atZone(zone)
        val today = LocalDate.now(zone)
        // 오늘 밤 12시는 '내일 00:00' 대신 '자정'
        if (dt.toLocalDate() == today.plusDays(1) && dt.hour == 0 && dt.minute == 0) return "자정"
        val hm = time(dt.hour * 60 + dt.minute)
        val day = when (dt.toLocalDate()) {
            today -> "오늘"
            today.plusDays(1) -> "내일"
            today.minusDays(1) -> "어제"
            else -> date(dt.toLocalDate())
        }
        return "$day $hm"
    }

    /** 10/3(토) */
    fun date(d: LocalDate) = "${d.monthValue}/${d.dayOfMonth}(${DAY_NAMES[d.dayOfWeek.value - 1]})"

    /** 기록 화면의 날짜 묶음 제목 */
    fun dayLabel(ms: Long): String {
        val zone = ZoneId.systemDefault()
        val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when (d) {
            today -> "오늘"
            today.minusDays(1) -> "어제"
            else -> date(d)
        }
    }

    fun days(days: Set<Int>): String = when (days) {
        (1..7).toSet() -> "매일"
        (1..5).toSet() -> "평일"
        setOf(6, 7) -> "주말"
        else -> days.sorted().joinToString("·") { DAY_NAMES[it - 1] }
    }

    fun dayName(day: Int) = DAY_NAMES[day - 1]

    fun monthDay(day: Int) = if (day >= 31) "말일" else "${day}일"

    /** 반복 주기: 평일 / 격주 월·수 / 매월 15일 / 3일마다 */
    fun repeat(t: Todo): String = when (t.repeatType) {
        RepeatType.NONE -> ""
        RepeatType.WEEKLY -> when (t.repeatInterval) {
            1 -> days(t.repeatDays)
            2 -> "격주 ${days(t.repeatDays)}"
            else -> "${t.repeatInterval}주마다 ${days(t.repeatDays)}"
        }
        RepeatType.MONTHLY -> "매월 ${monthDay(t.monthDay)}"
        RepeatType.EVERY_DAYS -> if (t.repeatInterval == 1) "매일" else "${t.repeatInterval}일마다"
    }

    /** 반복 할 일의 하루 시간대: 오전 9:00–오후 6:00 */
    fun dailyRange(t: Todo): String = time(t.dailyStart) + (t.dailyEnd?.let { "–${time(it)}" } ?: "부터")

    /** 목록 카드에 표시할 일정 요약 */
    fun schedule(t: Todo): String {
        if (!t.notify) return "알림 없음"
        if (t.isRepeat) return "${repeat(t)} ${dailyRange(t)}"
        val start = t.startAt
        val end = t.endAt
        return when {
            start == null && end == null -> "완료할 때까지"
            start == null -> "${dateTime(end!!)}까지"
            end == null -> "${dateTime(start)}부터"
            else -> "${dateTime(start)} – ${dateTime(end)}"
        }
    }

    /**
     * 알림에 붙는 일정 문구: 사용자가 정한 시각을 그대로 보여줌.
     * 알림은 날짜가 바뀌어도 다시 그려지지 않을 수 있어서 '오늘/내일' 대신 날짜로 표시
     */
    fun notificationSchedule(t: Todo): String {
        if (t.isRepeat) return "${schedule(t)} 반복"
        val start = t.startAt ?: t.createdAt
        val from = "${fullDateTime(start)} ${if (t.startAt == null) "추가" else "시작"}"
        val end = t.endAt ?: return "$from · 완료할 때까지"
        val sameDay = localDate(end) == localDate(start)
        return "$from · ${if (sameDay) time(minuteOfDay(end)) else fullDateTime(end)}까지"
    }

    /** 10/3(토) 14:30 */
    private fun fullDateTime(ms: Long) = "${date(localDate(ms))} ${time(minuteOfDay(ms))}"

    private fun localDate(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun minuteOfDay(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }

    fun status(t: Todo, now: Long): String = when (t.status(now)) {
        Status.SHOWING -> t.windowAt(now)?.end?.takeIf { it - now < DAY }
            ?.let { "알림 중 · ${duration(it - now)} 남음" } ?: "알림 중"
        Status.NO_ALERT -> "알림 없음"
        Status.SNOOZED -> "${dateTime(t.snoozeUntil!!)}까지 미룸"
        Status.HIDDEN -> if (t.isRepeat) "오늘 완료" else "알림 닫음"
        Status.SCHEDULED -> "${dateTime(t.nextStart(now)!!)} 시작"
        Status.ENDED -> "기간 종료"
        Status.DONE -> "${dateTime(t.doneAt!!)} 완료"
    }
}
