package com.pintodo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DateParserTest {
    private val zone = ZoneId.of("Asia/Seoul")

    /** 기준: 2026-10-06(화) 오전 10:20 */
    private val now = at(6, 10, 20)

    private fun at(day: Int, hour: Int, minute: Int = 0, month: Int = 10) =
        LocalDateTime.of(2026, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun parse(text: String) = DateParser.parse(text, now, zone)

    @Test fun `날짜와 시각`() {
        assertEquals(at(7, 15), parse("내일 오후 3시 회의")!!.startAt)
        assertEquals(at(7, 15, 30), parse("내일 3시 반 치과")!!.startAt)       // 1~6시는 오후
        assertEquals(at(8, 9), parse("모레 9시 운동")!!.startAt)               // 7~11시는 오전
        assertEquals(at(7, 12), parse("내일 12시 점심 약속")!!.startAt)         // 12시는 낮
        assertEquals(at(9, 18), parse("10월 9일 저녁 6시")!!.startAt)
        assertEquals(at(9, 18, 30), parse("10/9 18:30 저녁")!!.startAt)
        assertEquals(at(7, 22), parse("내일 밤 10시")!!.startAt)
        assertEquals("내일 오후 3시", parse("내일 오후 3시 회의")!!.matched)
    }

    @Test fun `시각만 있으면 다가오는 시각`() {
        assertEquals(at(6, 15), parse("3시 회의")!!.startAt)
        assertEquals(at(6, 11), parse("11시 전화")!!.startAt)
        assertEquals(at(6, 21), parse("9시 출근")!!.startAt)                   // 오전 9시는 지남 → 오후 9시
    }

    @Test fun `요일`() {
        assertEquals(at(9, 9), parse("금요일 발표")!!.startAt)                  // 날짜만 → 오전 9시
        assertEquals(at(12, 14), parse("다음 주 월요일 2시")!!.startAt)
        assertEquals(at(8, 18), parse("이번 주 목요일 저녁 약속")!!.startAt)
    }

    @Test fun `까지는 마감`() {
        val r = parse("금요일까지 보고서")!!
        assertNull(r.startAt)
        assertEquals(at(10, 0), r.endAt)                                       // 금요일 자정까지
        val r2 = parse("오늘 오후 6시까지 제출")!!
        assertNull(r2.startAt)
        assertEquals(at(6, 18), r2.endAt)
    }

    @Test fun `구간`() {
        val r = parse("내일 오후 3시부터 5시까지 회의")!!
        assertEquals(at(7, 15), r.startAt)
        assertEquals(at(7, 17), r.endAt)
        val r2 = parse("2시~4시 스터디")!!
        assertEquals(at(6, 14), r2.startAt)
        assertEquals(at(6, 16), r2.endAt)
    }

    @Test fun `상대 시각`() {
        assertEquals(at(6, 10, 50), parse("30분 뒤 전화")!!.startAt)
        assertEquals(at(6, 12, 20), parse("2시간 후 약 먹기")!!.startAt)
        assertEquals(at(9, 9), parse("3일 뒤 택배")!!.startAt)
    }

    @Test fun `날짜가 없으면 null`() {
        assertNull(parse("우유 사기"))
        assertNull(parse("오늘 할 일"))
        assertNull(parse("2시간 공부하기"))
    }
}
