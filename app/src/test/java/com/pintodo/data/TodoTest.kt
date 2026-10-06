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
        assertEquals("10/5(월) 08:00 추가 · 완료할 때까지", Format.notificationSchedule(todo()))
        assertEquals("10/5(월) 14:00 시작 · 18:00까지", Format.notificationSchedule(todo(startAt = at(5, 14), endAt = at(5, 18))))
        assertEquals("10/5(월) 14:00 시작 · 10/6(화) 09:00까지", Format.notificationSchedule(todo(startAt = at(5, 14), endAt = at(6, 9))))
        assertEquals("평일 09:00–18:00 반복", Format.notificationSchedule(todo(days = (1..5).toSet(), dailyEnd = 18 * 60)))
    }
}
