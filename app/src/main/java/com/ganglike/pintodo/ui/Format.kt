package com.ganglike.pintodo.ui

import com.ganglike.pintodo.data.Status
import com.ganglike.pintodo.data.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Format {
    private val DAY_NAMES = listOf("월", "화", "수", "목", "금", "토", "일")

    fun time(minuteOfDay: Int) = "%02d:%02d".format(minuteOfDay / 60 % 24, minuteOfDay % 60)

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

    /** 목록 카드에 표시할 일정 요약 */
    fun schedule(t: Todo): String {
        if (!t.notify) return "알림 없음"
        if (t.isRepeat) {
            val end = t.dailyEnd?.let { "–${time(it)}" } ?: "부터"
            return "${days(t.repeatDays)} ${time(t.dailyStart)}$end"
        }
        val start = t.startAt
        val end = t.endAt
        return when {
            start == null && end == null -> "완료할 때까지"
            start == null -> "${dateTime(end!!)}까지"
            end == null -> "${dateTime(start)}부터"
            else -> "${dateTime(start)} – ${dateTime(end)}"
        }
    }

    /** 알림에 붙는 짧은 일정 문구 */
    fun notificationSchedule(t: Todo, now: Long): String {
        val end = t.windowAt(now)?.end ?: return if (t.isRepeat) "${days(t.repeatDays)} 반복" else ""
        return "${dateTime(end)}까지" + if (t.isRepeat) " · ${days(t.repeatDays)} 반복" else ""
    }

    fun status(t: Todo, now: Long): String = when (t.status(now)) {
        Status.SHOWING -> "알림 중"
        Status.NO_ALERT -> "알림 없음"
        Status.SNOOZED -> "${dateTime(t.snoozeUntil!!)}까지 미룸"
        Status.HIDDEN -> if (t.isRepeat) "오늘 완료" else "알림 닫음"
        Status.SCHEDULED -> "${dateTime(t.nextStart(now)!!)} 시작"
        Status.ENDED -> "기간 종료"
        Status.DONE -> "${dateTime(t.doneAt!!)} 완료"
    }
}
