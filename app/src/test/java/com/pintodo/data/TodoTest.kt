package com.pintodo.data

import com.pintodo.notify.Actions
import com.pintodo.ui.Format
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class TodoTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private lateinit var saved: TimeZone

    @Before fun setUp() {
        saved = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
    }

    @After fun tearDown() = TimeZone.setDefault(saved)

    /** 2026-10-05는 월요일 */
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun todo(
        startAt: Long? = null, endAt: Long? = null,
        days: Set<Int> = emptySet(), dailyStart: Int = 9 * 60, dailyEnd: Int? = null,
    ) = Todo(id = 1, title = "t", startAt = startAt, endAt = endAt, repeatDays = days,
        dailyStart = dailyStart, dailyEnd = dailyEnd, createdAt = at(5, 8))

    // ── 한 번 ──

    @Test fun `시작 전에는 예정, 구간 안에서는 표시, 끝나면 종료`() {
        val t = todo(startAt = at(5, 14), endAt = at(5, 18))
        assertEquals(Status.SCHEDULED, t.status(at(5, 13)))
        assertEquals(Status.SHOWING, t.status(at(5, 14)))
        assertEquals(Status.ENDED, t.status(at(5, 18)))
    }

    @Test fun `시작이 없으면 만든 즉시 완료할 때까지`() {
        val t = todo()
        assertEquals(Status.SHOWING, t.status(at(10, 0)))
        assertNull(t.nextChange(at(10, 0)))
    }

    @Test fun `다음 변경 시각은 구간 끝과 미루기 끝 중 빠른 것`() {
        val t = todo(startAt = at(5, 14), endAt = at(5, 18)).copy(snoozeUntil = at(5, 15))
        assertEquals(Status.SNOOZED, t.status(at(5, 14, 30)))
        assertEquals(at(5, 15), t.nextChange(at(5, 14, 30)))
        assertEquals(at(5, 18), t.nextChange(at(5, 15)))
    }

    // ── 반복 ──

    @Test fun `평일 반복은 주말을 건너뛰고 월요일에 시작`() {
        val t = todo(days = (1..5).toSet(), dailyStart = 9 * 60, dailyEnd = 18 * 60)
        // 10/10(토) 정오 → 다음 시작은 10/12(월) 09:00
        assertEquals(Status.SCHEDULED, t.status(at(10, 12)))
        assertEquals(at(12, 9), t.nextStart(at(10, 12)))
    }

    @Test fun `자정을 넘기는 구간은 다음 날 새벽까지 표시`() {
        val t = todo(days = setOf(1), dailyStart = 22 * 60, dailyEnd = 2 * 60)  // 월 22:00 ~ 화 02:00
        assertEquals(Status.SHOWING, t.status(at(6, 1)))
        assertEquals(at(6, 2), t.windowAt(at(6, 1))!!.end)
        assertEquals(Status.SCHEDULED, t.status(at(6, 3)))
    }

    @Test fun `시작과 종료가 같으면 24시간`() {
        val t = todo(days = setOf(1), dailyStart = 9 * 60, dailyEnd = 9 * 60)
        assertEquals(at(6, 9), t.windowAt(at(5, 10))!!.end)
    }

    @Test fun `오늘 하나뿐인 요일이 지났으면 7일 뒤`() {
        val t = todo(days = setOf(1), dailyStart = 9 * 60, dailyEnd = 10 * 60)
        assertEquals(at(12, 9), t.nextStart(at(5, 11)))
    }

    @Test fun `반복 완료는 이번 회차만 숨기고 다음 회차에 다시 표시`() {
        val t = todo(days = setOf(1, 2), dailyStart = 9 * 60, dailyEnd = 18 * 60)
        val done = Actions.complete(t, at(5, 10))
        assertEquals(Status.HIDDEN, done.status(at(5, 11)))
        assertEquals(Status.SHOWING, done.status(at(6, 10)))
    }

    // ── 소리·진동 1회 ──

    @Test fun `알림 키는 회차 시작, 미루기가 끝나면 미루기 종료 시각`() {
        val t = todo(startAt = at(5, 14))
        assertEquals(at(5, 14), t.alertKey(at(5, 14, 5)))
        val snoozed = t.copy(snoozeUntil = at(5, 15))
        assertEquals(at(5, 14), snoozed.alertKey(at(5, 14, 30)))
        assertEquals(at(5, 15), snoozed.alertKey(at(5, 15, 1)))
    }

    // ── 미루기 ──

    @Test fun `저녁 6시 미루기는 이미 지났으면 다음 날 저녁`() {
        assertEquals(at(5, 18), SnoozeOption.EVENING.until(at(5, 17)))
        assertEquals(at(6, 18), SnoozeOption.EVENING.until(at(5, 19)))
    }

    // ── 알림 문구 ──

    @Test fun `알림 일정 문구는 항상 정한 시각을 보여줌`() {
        assertEquals("10/5(월) 오전 8:00 추가 · 완료할 때까지", Format.notificationSchedule(todo()))
        assertEquals("10/5(월) 오후 2:00 시작 · 오후 6:00까지", Format.notificationSchedule(todo(startAt = at(5, 14), endAt = at(5, 18))))
        assertEquals("10/5(월) 오후 2:00 시작 · 10/6(화) 오전 9:00까지", Format.notificationSchedule(todo(startAt = at(5, 14), endAt = at(6, 9))))
        assertEquals("평일 오전 9:00–오후 6:00 반복", Format.notificationSchedule(todo(days = (1..5).toSet(), dailyEnd = 18 * 60)))
    }

    @Test fun `시각은 오전 오후 12시간제`() {
        assertEquals("오전 12:00", Format.time(0))
        assertEquals("오전 9:05", Format.time(9 * 60 + 5))
        assertEquals("오후 12:30", Format.time(12 * 60 + 30))
        assertEquals("오후 11:59", Format.time(23 * 60 + 59))
    }

    // ── 반복 주기 확장 ──

    @Test fun `격주는 기준 주부터 한 주 건너 반복`() {
        // 기준: 10/5(월) 주. 월요일 격주 → 10/5, 10/19 O / 10/12 X
        val t = todo(days = setOf(1)).copy(repeatInterval = 2, repeatAnchor = java.time.LocalDate.of(2026, 10, 5).toEpochDay())
        assertEquals(at(19, 9), t.nextStart(at(5, 10)))
        assertEquals(Status.SHOWING, t.status(at(19, 10)))
        assertEquals(Status.SCHEDULED, t.status(at(12, 10)))
    }

    @Test fun `매월 31일은 짧은 달에 말일로`() {
        val t = todo().copy(repeatType = RepeatType.MONTHLY, monthDay = 31, dailyStart = 9 * 60)
        assertEquals(at(31, 9), t.nextStart(at(5, 10)))
        // 11월은 30일까지
        val nov30 = LocalDateTime.of(2026, 11, 30, 9, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(nov30, t.nextStart(at(31, 10)))
    }

    @Test fun `n일마다는 기준 날부터`() {
        val t = todo().copy(repeatType = RepeatType.EVERY_DAYS, repeatInterval = 3,
            repeatAnchor = java.time.LocalDate.of(2026, 10, 5).toEpochDay())
        assertEquals(Status.SHOWING, t.status(at(5, 10)))
        assertEquals(at(8, 9), t.nextStart(at(5, 10)))
        assertEquals(Status.SCHEDULED, t.status(at(6, 10)))
    }

    // ── 반복 종료 ──

    @Test fun `횟수만큼 반복하고 끝나면 종료`() {
        // 10/5(월)부터 매일 3회 → 10/5, 6, 7
        val t = todo(days = (1..7).toSet(), dailyEnd = 10 * 60).copy(repeatCount = 3,
            repeatAnchor = java.time.LocalDate.of(2026, 10, 5).toEpochDay())
        assertEquals(at(7, 9), t.nextStart(at(6, 11)))
        assertEquals(Status.SHOWING, t.status(at(7, 9, 30)))
        assertEquals(Status.ENDED, t.status(at(7, 11)))
        assertNull(t.nextStart(at(7, 11)))
    }

    @Test fun `종료일까지만 반복`() {
        val t = todo(days = setOf(1, 3), dailyEnd = 10 * 60).copy(repeatUntil = java.time.LocalDate.of(2026, 10, 12).toEpochDay())
        assertEquals(at(12, 9), t.nextStart(at(8, 11)))       // 10/12(월) 마지막
        assertEquals(Status.ENDED, t.status(at(12, 11)))       // 10/14(수)는 없음
    }

    @Test fun `반복 종료 문구`() {
        val t = todo(days = (1..5).toSet(), dailyEnd = 18 * 60)
        assertEquals("평일 오전 9:00–오후 6:00 · 10회", Format.schedule(t.copy(repeatCount = 10)))
        assertEquals("평일 오전 9:00–오후 6:00 · 12/31(목)까지", Format.schedule(t.copy(repeatUntil = java.time.LocalDate.of(2026, 12, 31).toEpochDay())))
    }

    // ── 다시 울리기 ──

    @Test fun `다시 울리기는 간격마다 알림 키가 바뀌고 다음 변경 시각에 잡힘`() {
        val t = todo(startAt = at(5, 14)).copy(remindEvery = 60)
        assertEquals(at(5, 14), t.alertKey(at(5, 14, 30)))
        assertEquals(at(5, 15), t.alertKey(at(5, 15, 10)))
        assertEquals(false, t.isReminder(at(5, 14, 30)))
        assertEquals(true, t.isReminder(at(5, 15, 10)))
        assertEquals(at(5, 16), t.nextChange(at(5, 15, 10)))
    }

    @Test fun `미루기가 끝나면 그 시각부터 다시 셈`() {
        val t = todo(startAt = at(5, 14)).copy(remindEvery = 60, snoozeUntil = at(5, 14, 40))
        assertEquals(at(5, 14, 40), t.alertKey(at(5, 15, 0)))
        assertEquals(at(5, 15, 40), t.alertKey(at(5, 15, 45)))
    }
}
