package com.pintodo.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * 제목·메모에 적힌 한국어 날짜/시각을 읽어 일정으로 바꾼다.
 * 예) "내일 오후 3시 회의", "금요일까지 보고서", "3시부터 5시까지", "30분 뒤 전화", "10월 9일 저녁"
 */
object DateParser {
    /** startAt == null 이면 '지금부터', endAt == null 이면 '완료할 때까지'. matched는 읽은 원문 */
    data class Result(val startAt: Long?, val endAt: Long?, val matched: String)

    private const val MINUTE = 60_000L
    private const val MERIDIEM = "오전|오후|아침|점심|저녁|밤|새벽|낮"
    private const val DOW = "월화수목금토일"
    private val AM = setOf("오전", "아침", "새벽")
    private val PM = setOf("오후", "저녁", "밤", "낮", "점심")

    private val RELATIVE = Regex("""(\d{1,3})\s*시간\s*(?:(\d{1,2})\s*분\s*)?(?:뒤|후)|(\d{1,3})\s*분\s*(?:뒤|후)""")
    private val COLON = Regex("""(?:($MERIDIEM)\s*)?(?<!\d)(\d{1,2})\s*:\s*(\d{2})(?!\d)""")
    private val HOUR = Regex("""(?:($MERIDIEM)\s*)?(?<![\d:])(\d{1,2})\s*시(?!간)(?:\s*(반)|\s*(\d{1,2})\s*분)?""")
    private val NOON = Regex("""정오|자정""")
    private val BARE_MERIDIEM = Regex("""아침|점심|저녁|밤""")
    private val RANGE_LINK = Regex("""^\s*(?:부터|에서)?\s*(?:~|-|–|부터|에서)?\s*$""")
    private val UNTIL = Regex("""^\s*(?:까지|전까지)""")

    private class TimeHit(val range: IntRange, val minute: Int, val ambiguous: Boolean, val meridiem: String?)
    private class DateHit(val range: IntRange, val date: LocalDate)

    fun parse(text: String, now: Long, zone: ZoneId = ZoneId.systemDefault()): Result? {
        if (text.isBlank()) return null
        val nowDt = Instant.ofEpochMilli(now).atZone(zone)
        val today = nowDt.toLocalDate()
        fun at(date: LocalDate, minute: Int) = date.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()

        // 1) 30분 뒤, 2시간 뒤 → 지금 기준
        RELATIVE.find(text)?.let { m ->
            val minutes = m.groupValues[3].toLongOrNull()
                ?: (m.groupValues[1].toLong() * 60 + (m.groupValues[2].toLongOrNull() ?: 0))
            if (minutes <= 0) return null
            val start = now + minutes * MINUTE
            return Result(start - start % MINUTE, null, m.value.trim())
        }

        val date = findDate(text, today)
        val times = findTimes(text)

        // 2) 시각이 없으면 날짜만: '까지'면 그날 자정까지, 아니면 그날 아침 9시부터
        if (times.isEmpty()) {
            val d = date ?: return null
            if (until(text, d.range)) return Result(null, at(d.date.plusDays(1), 0), text.slice(d.range))
            // 날짜만 있고 '저녁에' 같은 말이 붙어 있으면 그 시간대로
            val bare = BARE_MERIDIEM.find(text)?.let { m ->
                val minute = when (m.value) { "아침" -> 9 * 60; "점심" -> 12 * 60; "저녁" -> 18 * 60; else -> 21 * 60 }
                m.range to minute
            }
            if (bare != null) return Result(at(d.date, bare.second), null, span(text, d.range, bare.first))
            if (d.date == today) return null  // '오늘'만으로는 바꿀 것이 없음
            return Result(at(d.date, 9 * 60), null, text.slice(d.range))
        }

        // 3) 시각이 있으면: 날짜가 없을 때는 가장 가까운 미래 시각
        val first = times[0]
        val startMs = resolve(first, date?.date, today, nowDt.hour * 60 + nowDt.minute) { d, m -> at(d, m) }
        val startDate = Instant.ofEpochMilli(startMs).atZone(zone).toLocalDate()

        // '3시부터 5시까지', '오후 2시~4시' → 구간
        val second = times.getOrNull(1)
        if (second != null && RANGE_LINK.matches(text.substring(first.range.last + 1, second.range.first))) {
            val startMinute = Instant.ofEpochMilli(startMs).atZone(zone).let { it.hour * 60 + it.minute }
            var endMinute = second.minute
            // 끝 시각에 오전/오후가 없으면 시작 뒤로 오는 가장 가까운 시각 (오후 3시부터 5시 → 17시)
            if (second.ambiguous && endMinute <= startMinute && endMinute + 12 * 60 > startMinute) endMinute += 12 * 60
            var endMs = at(startDate, endMinute)
            if (endMs <= startMs) endMs = at(startDate.plusDays(1), second.minute)
            return Result(startMs, endMs, span(text, date?.range, first.range, second.range))
        }

        // '금요일 오후 6시까지' → 마감
        if (until(text, first.range)) return Result(null, startMs, span(text, date?.range, first.range))
        return Result(startMs, null, span(text, date?.range, first.range))
    }

    /** 오전/오후가 없는 1~12시는 날짜가 있으면 1~6시를 오후로, 없으면 다가오는 시각으로 */
    private fun resolve(
        t: TimeHit, date: LocalDate?, today: LocalDate, nowMinute: Int, at: (LocalDate, Int) -> Long,
    ): Long {
        if (!t.ambiguous) {
            val d = date ?: if (t.minute > nowMinute) today else today.plusDays(1)
            return at(d, t.minute)
        }
        val hour = t.minute / 60
        val am = (hour % 12) * 60 + t.minute % 60
        val pm = am + 12 * 60
        // 12시는 낮 12시, 1~6시는 오후가 흔함
        val candidates = if (hour == 12 || hour in 1..6) listOf(pm, am) else listOf(am, pm)
        if (date != null) return at(date, candidates[0])
        candidates.firstOrNull { it > nowMinute }?.let { return at(today, it) }
        return at(today.plusDays(1), candidates[0])
    }

    private fun until(text: String, range: IntRange) = UNTIL.containsMatchIn(text.substring(range.last + 1))

    private fun span(text: String, vararg ranges: IntRange?): String {
        val rs = ranges.filterNotNull()
        return text.substring(rs.minOf { it.first }, rs.maxOf { it.last } + 1).trim()
    }

    private fun findTimes(text: String): List<TimeHit> {
        val hits = mutableListOf<TimeHit>()
        COLON.findAll(text).forEach { m ->
            toMinute(m.groupValues[1], m.groupValues[2].toInt(), m.groupValues[3].toInt())?.let { hits += it(m.range) }
        }
        HOUR.findAll(text).forEach { m ->
            val minute = if (m.groupValues[3].isNotEmpty()) 30 else m.groupValues[4].toIntOrNull() ?: 0
            toMinute(m.groupValues[1], m.groupValues[2].toInt(), minute)?.let { hits += it(m.range) }
        }
        NOON.findAll(text).forEach { m ->
            hits += TimeHit(m.range, if (m.value == "정오") 12 * 60 else 24 * 60, false, null)
        }
        return hits.sortedBy { it.range.first }
            .fold(mutableListOf()) { acc, h -> if (acc.none { it.range.last >= h.range.first }) acc += h; acc }
    }

    private fun toMinute(meridiem: String, hour: Int, minute: Int): ((IntRange) -> TimeHit)? {
        if (minute > 59 || hour > 24) return null
        val mer = meridiem.ifEmpty { null }
        val value = when {
            mer in AM -> if (hour > 12) return null else (hour % 12) * 60 + minute
            mer in PM -> when {
                hour == 12 && mer == "밤" -> 24 * 60 + minute
                hour < 12 -> (hour + 12) * 60 + minute
                else -> hour * 60 + minute
            }
            else -> hour * 60 + minute
        }
        val ambiguous = mer == null && hour in 1..12
        return { range -> TimeHit(range, value, ambiguous, mer) }
    }

    private fun findDate(text: String, today: LocalDate): DateHit? {
        fun valid(y: Int, m: Int, d: Int) = runCatching { LocalDate.of(y, m, d) }.getOrNull()

        // 10월 9일, 10/9
        Regex("""(?<!\d)(\d{1,2})\s*월\s*(\d{1,2})\s*일""").find(text)?.let { m ->
            monthDay(today, m.groupValues[1].toInt(), m.groupValues[2].toInt())?.let { return DateHit(m.range, it) }
        }
        Regex("""(?<![\d/])(\d{1,2})/(\d{1,2})(?![\d/])""").find(text)?.let { m ->
            monthDay(today, m.groupValues[1].toInt(), m.groupValues[2].toInt())?.let { return DateHit(m.range, it) }
        }
        // 다음 달 5일
        Regex("""(다음|이번)\s*달\s*(\d{1,2})\s*일""").find(text)?.let { m ->
            val month = if (m.groupValues[1] == "다음") today.plusMonths(1) else today
            valid(month.year, month.monthValue, m.groupValues[2].toInt())?.let { return DateHit(m.range, it) }
        }
        // 3일 뒤, 2주 후
        Regex("""(\d{1,3})\s*(일|주)\s*(?:뒤|후)""").find(text)?.let { m ->
            val n = m.groupValues[1].toLong()
            return DateHit(m.range, if (m.groupValues[2] == "주") today.plusWeeks(n) else today.plusDays(n))
        }
        // 다음 주 월요일, 이번 주 금요일
        Regex("""(다음|담|이번|돌아오는)\s*주\s*([$DOW])요일""").find(text)?.let { m ->
            val dow = DayOfWeek.of(DOW.indexOf(m.groupValues[2]) + 1)
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val week = if (m.groupValues[1] == "이번") monday else monday.plusWeeks(1)
            return DateHit(m.range, week.plusDays(dow.value - 1L))
        }
        // 금요일 → 다가오는 금요일 (오늘이면 오늘)
        Regex("""([$DOW])요일""").find(text)?.let { m ->
            val dow = DayOfWeek.of(DOW.indexOf(m.groupValues[1]) + 1)
            return DateHit(m.range, today.with(TemporalAdjusters.nextOrSame(dow)))
        }
        Regex("""오늘|내일|모레|글피""").find(text)?.let { m ->
            val plus = when (m.value) { "오늘" -> 0L; "내일" -> 1L; "모레" -> 2L; else -> 3L }
            return DateHit(m.range, today.plusDays(plus))
        }
        // 15일까지 → 이번 달(지났으면 다음 달)
        Regex("""(?<![\d/월])(\d{1,2})\s*일(?=\s*(?:까지|에|부터|날)|\s*$)""").find(text)?.let { m ->
            val day = m.groupValues[1].toInt()
            val thisMonth = valid(today.year, today.monthValue, day)?.takeIf { !it.isBefore(today) }
            val next = today.plusMonths(1)
            (thisMonth ?: valid(next.year, next.monthValue, day))?.let { return DateHit(m.range, it) }
        }
        return null
    }

    /** 올해 그 날짜, 이미 지났으면 내년 */
    private fun monthDay(today: LocalDate, month: Int, day: Int): LocalDate? {
        val d = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return null
        return if (d.isBefore(today)) d.plusYears(1) else d
    }
}
