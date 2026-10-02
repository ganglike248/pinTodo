package com.ganglike.pintodo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 미루기 선택지 */
enum class SnoozeOption(val label: String, val buttonLabel: String) {
    MIN_10("10분", "10분 뒤"),
    MIN_30("30분", "30분 뒤"),
    HOUR_1("1시간", "1시간 뒤"),
    HOUR_3("3시간", "3시간 뒤"),
    EVENING("오늘 저녁 6시", "저녁 6시"),
    TOMORROW("내일 아침 9시", "내일 아침"),
    ;

    /** 미룬 뒤 다시 뜰 시각. 오늘 저녁이 이미 지났으면 null */
    fun until(now: Long): Long? {
        val zone = ZoneId.systemDefault()
        val minute = 60_000L
        return when (this) {
            MIN_10 -> now + 10 * minute
            MIN_30 -> now + 30 * minute
            HOUR_1 -> now + 60 * minute
            HOUR_3 -> now + 180 * minute
            EVENING -> at(LocalDate.now(zone), 18, zone).takeIf { it > now }
            TOMORROW -> at(LocalDate.now(zone).plusDays(1), 9, zone)
        }
    }

    private fun at(date: LocalDate, hour: Int, zone: ZoneId) =
        ZonedDateTime.of(date, LocalTime.of(hour, 0), zone).toInstant().toEpochMilli()
}

data class AppSettings(
    val snoozeOptions: Set<SnoozeOption> = setOf(SnoozeOption.MIN_10, SnoozeOption.HOUR_1, SnoozeOption.TOMORROW),
    val quickSnooze: SnoozeOption = SnoozeOption.MIN_10,  // 알림에 바로 보이는 미루기 버튼
    val defaultPinned: Boolean = true,
    val defaultAlertMode: AlertMode = AlertMode.BOTH,
    val dynamicColor: Boolean = false,                    // 배경화면 색상(Material You) 사용
) {
    /** 화면에 보여줄 미루기 선택지 (정의 순서 유지) */
    val enabledSnoozes get() = SnoozeOption.entries.filter { it in snoozeOptions }
}

object SettingsStore {
    private const val PREFS = "settings"
    private val state = MutableStateFlow(AppSettings())
    private var loaded = false

    fun flow(context: Context): StateFlow<AppSettings> {
        ensureLoaded(context)
        return state
    }

    fun get(context: Context): AppSettings {
        ensureLoaded(context)
        return state.value
    }

    @Synchronized
    fun update(context: Context, change: (AppSettings) -> AppSettings) {
        ensureLoaded(context)
        var next = change(state.value)
        // 미루기 선택지는 최소 1개, 바로가기 버튼은 켜진 선택지 중 하나
        if (next.snoozeOptions.isEmpty()) next = next.copy(snoozeOptions = setOf(next.quickSnooze))
        if (next.quickSnooze !in next.snoozeOptions) next = next.copy(quickSnooze = next.enabledSnoozes.first())
        prefs(context).edit()
            .putStringSet("snoozeOptions", next.snoozeOptions.map { it.name }.toSet())
            .putString("quickSnooze", next.quickSnooze.name)
            .putBoolean("defaultPinned", next.defaultPinned)
            .putString("defaultAlertMode", next.defaultAlertMode.name)
            .putBoolean("dynamicColor", next.dynamicColor)
            .commit()
        state.value = next
    }

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        val p = prefs(context)
        val d = AppSettings()
        state.value = AppSettings(
            snoozeOptions = p.getStringSet("snoozeOptions", null)
                ?.mapNotNull { runCatching { SnoozeOption.valueOf(it) }.getOrNull() }?.toSet()
                ?.ifEmpty { null } ?: d.snoozeOptions,
            quickSnooze = p.getString("quickSnooze", null)
                ?.let { runCatching { SnoozeOption.valueOf(it) }.getOrNull() } ?: d.quickSnooze,
            defaultPinned = p.getBoolean("defaultPinned", d.defaultPinned),
            defaultAlertMode = p.getString("defaultAlertMode", null)
                ?.let { runCatching { AlertMode.valueOf(it) }.getOrNull() } ?: d.defaultAlertMode,
            dynamicColor = p.getBoolean("dynamicColor", d.dynamicColor),
        )
        loaded = true
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
