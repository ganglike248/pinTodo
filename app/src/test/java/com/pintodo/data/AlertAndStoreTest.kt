package com.pintodo.data

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/** 소리·진동 1회 판정, 밤 조용히, 저장 형식(JSON) 호환 */
class AlertAndStoreTest {
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

    private fun todo() = Todo(id = 1, title = "t", createdAt = at(5, 8))

    // ── 소리·진동 1회 ──

    @Test fun `같은 회차 안에서는 알림 키가 그대로라 다시 울리지 않음`() {
        val t = todo().copy(startAt = at(5, 14))
        val key = t.alertKey(at(5, 14))
        val alerted = t.copy(alertedKey = key)
        // Sync는 key != alertedKey 일 때만 울림
        assertEquals(key, alerted.alertKey(at(5, 20)))
        assertEquals(key, alerted.alertKey(at(9, 3)))
    }

    @Test fun `반복은 다음 회차에 키가 바뀌어 한 번 더 울림`() {
        val t = todo().copy(repeatDays = setOf(1, 2), repeatType = RepeatType.WEEKLY, dailyStart = 9 * 60, dailyEnd = 10 * 60)
        val mon = t.alertKey(at(5, 9, 30))
        val tue = t.alertKey(at(6, 9, 30))
        assertEquals(at(5, 9), mon)
        assertEquals(at(6, 9), tue)
        assertNull(t.alertKey(at(5, 11)))   // 구간 밖에는 울릴 것 없음
    }

    @Test fun `완료·알림 없음 할 일은 알림 키와 예약이 없음`() {
        val done = todo().copy(doneAt = at(5, 9))
        assertNull(done.alertKey(at(5, 10)))
        assertNull(done.nextChange(at(5, 10)))
        val silent = todo().copy(notify = false, remindEvery = 30)
        assertEquals(Status.NO_ALERT, silent.status(at(5, 10)))
        assertNull(silent.alertKey(at(5, 10)))
        assertNull(silent.nextChange(at(5, 10)))
    }

    @Test fun `미뤄둔 동안에는 다시 울리기 예약 대신 미루기 끝에 깨어남`() {
        val t = todo().copy(startAt = at(5, 14), remindEvery = 10, snoozeUntil = at(5, 16))
        assertEquals(Status.SNOOZED, t.status(at(5, 15)))
        assertEquals(at(5, 16), t.nextChange(at(5, 15)))
    }

    @Test fun `알림 머리글 시각은 13일 넘게 지나면 넣지 않음`() {
        val t = todo().copy(startAt = at(1, 9))
        assertEquals(at(1, 9), t.shownSince(at(13, 9)))       // 12일 지남
        assertNull(t.shownSince(at(14, 9)))                   // 13일 지남 → Android 16이 무시하기 전에 뺌
    }

    // ── 밤(오후 10시~오전 8시) ──

    @Test fun `밤 판정 경계`() {
        assertFalse(SettingsStore.isNight(at(5, 21, 59)))
        assertTrue(SettingsStore.isNight(at(5, 22, 0)))
        assertTrue(SettingsStore.isNight(at(6, 7, 59)))
        assertFalse(SettingsStore.isNight(at(6, 8, 0)))
    }

    @Test fun `설정 기본값은 밤 조용히 꺼짐`() {
        assertFalse(AppSettings().quietNight)
    }

    // ── 저장 형식 ──

    @Test fun `모든 필드가 저장 후 그대로 복원됨`() {
        val t = Todo(
            id = 7, title = "운동", memo = "메모", items = listOf(CheckItem("a", true), CheckItem("b")),
            color = LabelColor.GREEN, notify = true, pinned = false, alertMode = AlertMode.VIBRATE, remindEvery = 30,
            startAt = at(5, 9), endAt = at(5, 18),
            repeatDays = setOf(1, 3, 5), repeatType = RepeatType.WEEKLY, repeatInterval = 2, monthDay = 31,
            repeatAnchor = LocalDate.of(2026, 10, 5).toEpochDay(), repeatUntil = LocalDate.of(2026, 12, 31).toEpochDay(),
            repeatCount = 10, dailyStart = 19 * 60, dailyEnd = 1 * 60,
            createdAt = at(5, 8), doneAt = null, snoozeUntil = at(5, 20), hiddenKey = at(5, 19),
            alertedKey = at(5, 19), wearDismissedKey = at(5, 19),
        )
        assertEquals(t, TodoStore.fromJson(TodoStore.toJson(t)))
        val minimal = Todo(id = 2, title = "x", createdAt = at(5, 8))
        assertEquals(minimal, TodoStore.fromJson(TodoStore.toJson(minimal)))
    }

    @Test fun `v1 데이터는 제목을 text에서 읽고 이미 울린 것으로 처리`() {
        val t = TodoStore.fromJson(JSONObject("""{"id":3,"text":"옛 할 일"}"""))
        assertEquals("옛 할 일", t.title)
        assertEquals(0L, t.alertedKey)
        assertEquals(RepeatType.NONE, t.repeatType)
        assertTrue(t.notify)
    }

    @Test fun `v0_4_3 이전 요일 반복은 WEEKLY로 읽음`() {
        val t = TodoStore.fromJson(JSONObject("""{"id":4,"title":"t","alertMode":"BOTH","repeatDays":[1,3]}"""))
        assertEquals(RepeatType.WEEKLY, t.repeatType)
        assertEquals(setOf(1, 3), t.repeatDays)
    }

    @Test fun `모르는 라벨 색은 무시하고 나머지는 읽음`() {
        val t = TodoStore.fromJson(JSONObject("""{"id":5,"title":"t","alertMode":"SOUND","color":"PINK"}"""))
        assertNull(t.color)
        assertEquals(AlertMode.SOUND, t.alertMode)
    }
}
