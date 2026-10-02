package com.ganglike.pintodo.ui

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ganglike.pintodo.data.AlertMode

/** 휴대폰의 현재 소리 모드 */
enum class PhoneMode(val label: String) { SOUND("소리"), VIBRATE("진동"), SILENT("무음"), DND("방해 금지") }

private fun readPhoneMode(ctx: Context): PhoneMode {
    val nm = ctx.getSystemService(NotificationManager::class.java)
    if (nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL) return PhoneMode.DND
    return when (ctx.getSystemService(AudioManager::class.java).ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> PhoneMode.VIBRATE
        AudioManager.RINGER_MODE_SILENT -> PhoneMode.SILENT
        else -> PhoneMode.SOUND
    }
}

/** 소리 모드 / 방해 금지 변경을 실시간으로 반영 */
@Composable
fun rememberPhoneMode(): PhoneMode {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(readPhoneMode(ctx)) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { mode = readPhoneMode(ctx) }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        // 시스템 브로드캐스트는 NOT_EXPORTED로도 수신됨
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ctx.registerReceiver(receiver, filter)
        }
        onDispose { ctx.unregisterReceiver(receiver) }
    }
    return mode
}

/**
 * 선택한 알림 방식이 지금 휴대폰 모드에서 실제로 어떻게 울리는지.
 * 안드로이드는 진동 모드일 때 소리 알림을 진동으로 바꿔 울리고, 무음·방해 금지에서는 둘 다 막는다.
 */
fun describeAlert(mode: AlertMode, phone: PhoneMode): Pair<String, Boolean> {
    if (mode == AlertMode.SILENT) return "소리·진동 없이 알림창에만 조용히 표시돼요." to false
    val once = "처음 뜰 때 한 번만"
    return when (phone) {
        PhoneMode.SOUND -> when (mode) {
            AlertMode.SOUND -> "$once 소리로 알려요. (진동 없음)"
            AlertMode.VIBRATE -> "$once 진동으로 알려요. (소리 없음)"
            else -> "$once 소리와 진동으로 알려요."
        } to false
        PhoneMode.VIBRATE -> when (mode) {
            AlertMode.VIBRATE -> "$once 진동으로 알려요."
            else -> "지금 휴대폰이 진동 모드라서 소리 대신 진동으로 알려요."
        } to (mode != AlertMode.VIBRATE)
        PhoneMode.SILENT -> "지금 휴대폰이 무음 모드라서 소리·진동 없이 알림창에만 표시돼요." to true
        PhoneMode.DND -> "지금 방해 금지 모드라서 소리·진동 없이 표시돼요. (방해 금지 예외 앱으로 등록하면 울려요)" to true
    }
}

@Composable
fun AlertModeHint(mode: AlertMode, modifier: Modifier = Modifier) {
    val phone = rememberPhoneMode()
    val (text, warn) = describeAlert(mode, phone)
    InfoBox(
        icon = if (warn) Icons.Rounded.WarningAmber else Icons.Rounded.Info,
        text = "휴대폰: ${phone.label} 모드 · $text",
        warn = warn,
        modifier = modifier,
    )
}
