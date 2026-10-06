package com.pintodo.ui

import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pintodo.R
import com.pintodo.data.AlertMode
import com.pintodo.data.Backup
import com.pintodo.data.TodoStore
import com.pintodo.data.SettingsStore
import com.pintodo.data.SnoozeOption
import com.pintodo.notify.Sync
import com.pintodo.tile.QuickAddTileService
import com.pintodo.widget.TodoWidgetReceiver
import java.time.LocalDate

private enum class SettingSheet { QUICK_SNOOZE, SNOOZE_OPTIONS, ALERT_MODE }

const val PRIVACY_URL = "https://ganglike248.github.io/pinTodo/privacy-policy"
const val FEEDBACK_URL = "https://github.com/ganglike248/pinTodo/issues"
private const val STORE_URL = "https://play.google.com/store/apps/details?id=com.pintodo"

private fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

/** Play 스토어 앱으로, 없으면 웹으로 */
private fun openStore(ctx: Context) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}"))) }
        .onFailure { openUrl(ctx, STORE_URL) }
}

private fun shareApp(ctx: Context) {
    val text = "할 일을 끝낼 때까지 알림창에 고정해 주는 무료 앱 PinTodo\n$STORE_URL"
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "PinTodo 알려주기"))
}

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val ctx = LocalContext.current
    val s by SettingsStore.flow(ctx).collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<SettingSheet?>(null) }
    var showChangelog by remember { mutableStateOf(false) }
    var batteryOk by remember { mutableStateOf(ignoringBattery(ctx)) }
    var exactOk by remember { mutableStateOf(Sync.canExact(ctx)) }
    LifecycleResumeEffect(Unit) {
        batteryOk = ignoringBattery(ctx)
        exactOk = Sync.canExact(ctx)
        onPauseOrDispose { }
    }
    var restoring by remember { mutableStateOf<Backup.Content?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(Backup.export(ctx).toByteArray()) }
        }.isSuccess
        Toast.makeText(ctx, if (ok) "백업 파일을 저장했어요" else "저장하지 못했어요", Toast.LENGTH_SHORT).show()
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            Backup.read(text)
        }.onSuccess { restoring = it }
            .onFailure { Toast.makeText(ctx, it.message ?: "백업 파일을 읽지 못했어요", Toast.LENGTH_LONG).show() }
    }
    fun update(change: (com.pintodo.data.AppSettings) -> com.pintodo.data.AppSettings) {
        SettingsStore.update(ctx, change)
        Sync.run(ctx) // 알림 버튼(미루기 라벨) 반영
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("설정") }

        item {
            Section("알림") {
                SettingRow(
                    title = "알림에 바로 보이는 미루기",
                    value = s.quickSnooze.buttonLabel,
                    icon = Icons.Rounded.Snooze,
                    valueColor = MaterialTheme.colorScheme.primary,
                    onClick = { sheet = SettingSheet.QUICK_SNOOZE },
                )
                Divider()
                SettingRow(
                    title = "미루기 선택지",
                    value = s.enabledSnoozes.joinToString(" · ") { it.label },
                    icon = Icons.Rounded.Tune,
                    onClick = { sheet = SettingSheet.SNOOZE_OPTIONS },
                )
                Divider()
                SettingRow(
                    title = "밤에는 다시 울리지 않기",
                    value = if (s.quietNight) "오후 10시~오전 8시에는 '다시 울리기'를 쉬어요" else "꺼짐 · 밤에도 정한 간격마다 다시 울려요",
                    icon = Icons.Rounded.Bedtime,
                    onClick = { update { it.copy(quietNight = !it.quietNight) } },
                    trailing = { Switch(s.quietNight, { v -> update { it.copy(quietNight = v) } }) },
                )
                Divider()
                SettingRow(
                    title = "스마트워치에도 알림",
                    value = if (s.wearable) "워치 앱 알림 설정에서 PinTodo를 켜 두세요" else "휴대폰에만 표시해요",
                    icon = Icons.Rounded.Watch,
                    onClick = { update { it.copy(wearable = !it.wearable) } },
                    trailing = { Switch(s.wearable, { v -> update { it.copy(wearable = v) } }) },
                )
            }
        }

        item {
            Section("새 할 일 기본값") {
                SettingRow(
                    title = "알림 받기",
                    value = if (s.defaultNotify) "알림창에 표시해요" else "목록·위젯에만 추가해요",
                    icon = Icons.Rounded.Notifications,
                    onClick = { update { it.copy(defaultNotify = !it.defaultNotify) } },
                    trailing = { Switch(s.defaultNotify, { v -> update { it.copy(defaultNotify = v) } }) },
                )
                Divider()
                SettingRow(
                    title = "알림 방식",
                    value = alertSummary(s.defaultPinned, s.defaultAlertMode, s.defaultRemindEvery),
                    icon = s.defaultAlertMode.icon(),
                    valueColor = MaterialTheme.colorScheme.primary,
                    onClick = { sheet = SettingSheet.ALERT_MODE },
                )
            }
        }

        item {
            Section("바로가기") {
                SettingRow(
                    title = "빠른 설정 타일 추가",
                    value = "알림창을 내려서 바로 할 일을 추가해요",
                    icon = Icons.Rounded.Tune,
                    onClick = { addTile(ctx) },
                )
                Divider()
                SettingRow(
                    title = "홈 화면 위젯 추가",
                    value = "할 일을 홈 화면에서 보고 완료해요",
                    icon = Icons.Rounded.Widgets,
                    onClick = { addWidget(ctx) },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Divider()
                    SettingRow(
                        title = "배경화면 색상 사용",
                        value = "배경화면에 맞춰 앱 색이 바뀌어요",
                        icon = Icons.Rounded.Palette,
                        onClick = { update { it.copy(dynamicColor = !it.dynamicColor) } },
                        trailing = { Switch(s.dynamicColor, { v -> update { it.copy(dynamicColor = v) } }) },
                    )
                }
            }
        }

        item {
            Section("권한") {
                SettingRow(
                    title = "알림 설정",
                    value = "채널별 소리·진동, 잠금화면 표시",
                    icon = Icons.Rounded.Notifications,
                    onClick = { ctx.startActivity(notificationSettings(ctx)) },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Divider()
                    SettingRow(
                        title = "알람 및 리마인더",
                        value = if (exactOk) "허용됨" else "허용 안 됨 · 알림이 늦을 수 있어요",
                        valueColor = if (exactOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Rounded.Alarm,
                        onClick = { ctx.startActivity(exactAlarmSettings(ctx)) },
                    )
                }
                Divider()
                SettingRow(
                    title = "배터리 제한 없음",
                    value = if (batteryOk) "설정됨" else "설정 안 됨 · 앱 정보 > 배터리",
                    valueColor = if (batteryOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Rounded.BatteryChargingFull,
                    onClick = if (batteryOk) null else ({ ctx.startActivity(batteryExemption(ctx)) }),
                )
            }
        }

        item {
            Section("백업") {
                SettingRow(
                    title = "자동 백업",
                    value = "휴대폰의 Google 백업이 켜져 있으면 새 휴대폰으로 자동으로 옮겨져요",
                    icon = Icons.Rounded.CloudDone,
                )
                Divider()
                SettingRow(
                    title = "백업 파일 만들기",
                    value = "할 일과 설정을 파일로 저장해요",
                    icon = Icons.Rounded.Backup,
                    onClick = { exportLauncher.launch("pintodo-backup-${LocalDate.now()}.json") },
                )
                Divider()
                SettingRow(
                    title = "파일에서 복원",
                    value = "백업 파일로 할 일과 설정을 되돌려요",
                    icon = Icons.Rounded.Restore,
                    // 일부 파일 앱은 .json을 text/plain이나 octet-stream으로 알려줌
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                )
            }
        }

        item {
            Section("정보") {
                SettingRow(
                    title = "버전",
                    value = "${appVersion(ctx)} · 업데이트 기록 보기",
                    icon = Icons.Rounded.Info,
                    onClick = { showChangelog = true },
                )
                Divider()
                SettingRow(
                    title = "별점 남기기",
                    value = "PinTodo가 도움이 됐다면 응원해 주세요",
                    icon = Icons.Rounded.StarOutline,
                    onClick = { openStore(ctx) },
                )
                Divider()
                SettingRow(
                    title = "친구에게 알려주기",
                    value = "할 일을 자주 놓치는 친구에게 공유해요",
                    icon = Icons.Rounded.Share,
                    onClick = { shareApp(ctx) },
                )
                Divider()
                SettingRow(
                    title = "의견 보내기",
                    value = "불편한 점이나 바라는 기능을 알려 주세요",
                    icon = Icons.Rounded.Feedback,
                    onClick = { openUrl(ctx, FEEDBACK_URL) },
                )
                Divider()
                SettingRow(
                    title = "개인정보처리방침",
                    value = "수집하는 정보가 없어요",
                    icon = Icons.Rounded.Shield,
                    onClick = { openUrl(ctx, PRIVACY_URL) },
                )
            }
        }
    }

    when (sheet) {
        SettingSheet.QUICK_SNOOZE -> AppSheet({ sheet = null }) { hide ->
            SheetTitle("알림에 바로 보이는 미루기", "켜 둔 미루기 선택지 중에서 골라요")
            s.enabledSnoozes.forEach { option ->
                SettingRow(
                    title = option.buttonLabel,
                    onClick = { update { it.copy(quickSnooze = option) }; hide { sheet = null } },
                    trailing = { RadioButton(option == s.quickSnooze, onClick = { update { it.copy(quickSnooze = option) }; hide { sheet = null } }) },
                )
            }
        }
        SettingSheet.SNOOZE_OPTIONS -> AppSheet({ sheet = null }) { hide ->
            SheetTitle("미루기 선택지", "알림의 '미루기…'와 목록에서 밀었을 때 보여요")
            SnoozeOption.entries.forEach { option ->
                val checked = option in s.snoozeOptions
                SettingRow(
                    title = option.label,
                    dense = true,
                    onClick = { update { it.copy(snoozeOptions = toggle(it.snoozeOptions, option)) } },
                    trailing = { Checkbox(checked, onCheckedChange = { update { it.copy(snoozeOptions = toggle(it.snoozeOptions, option)) } }) },
                )
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("확인", { hide { sheet = null } })
        }
        SettingSheet.ALERT_MODE -> AlertSheet(
            pinned = s.defaultPinned, mode = s.defaultAlertMode, remind = s.defaultRemindEvery,
            onPinned = { v -> update { it.copy(defaultPinned = v) } },
            onMode = { m -> update { it.copy(defaultAlertMode = m) } },
            onRemind = { r -> update { it.copy(defaultRemindEvery = r) } },
            onDismiss = { sheet = null },
        )
        null -> {}
    }

    if (showChangelog) ChangelogDialog(onClose = { showChangelog = false })

    restoring?.let { content ->
        val current = TodoStore.all(ctx).size
        AlertDialog(
            onDismissRequest = { restoring = null },
            title = { Text("백업에서 복원할까요?") },
            text = {
                Text(
                    "${if (content.exportedAt > 0) Format.dateTime(content.exportedAt) + "에 만든 " else ""}백업의 할 일 ${content.todos.size}개와 설정으로 바꿔요." +
                        if (current > 0) "\n지금 있는 할 일 ${current}개는 지워져요." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Backup.restore(ctx, content)
                    Sync.run(ctx)
                    restoring = null
                    Toast.makeText(ctx, "할 일 ${content.todos.size}개를 복원했어요", Toast.LENGTH_SHORT).show()
                }) { Text("복원", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { restoring = null }) { Text("취소") } },
        )
    }
}

/** 알림 방식 4칸 + 현재 휴대폰 모드에서 실제 동작 안내 */
@Composable
fun AlertModePicker(mode: AlertMode, onChange: (AlertMode) -> Unit, reminds: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AlertMode.entries.forEach { m ->
                ChoiceTile(m.icon(), m.label.replace("소리+진동", "모두"), m == mode, { onChange(m) }, Modifier.weight(1f))
            }
        }
        AlertModeHint(mode, reminds = reminds)
    }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)

private fun toggle(set: Set<SnoozeOption>, o: SnoozeOption) = if (o in set) set - o else set + o

private fun appVersion(ctx: Context): String =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "-"

private fun addTile(ctx: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ctx.getSystemService(StatusBarManager::class.java).requestAddTileService(
            QuickAddTileService.component(ctx),
            ctx.getString(R.string.tile_label),
            Icon.createWithResource(ctx, R.drawable.ic_pin),
            ctx.mainExecutor,
        ) { result ->
            val msg = when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "타일을 추가했어요"
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "이미 추가되어 있어요"
                else -> null
            }
            if (msg != null) Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(ctx, "알림창 > 빠른 설정 편집에서 '할 일 추가'를 끌어다 놓으세요", Toast.LENGTH_LONG).show()
    }
}

private fun addWidget(ctx: Context) {
    val awm = AppWidgetManager.getInstance(ctx)
    if (awm.isRequestPinAppWidgetSupported) {
        awm.requestPinAppWidget(ComponentName(ctx, TodoWidgetReceiver::class.java), null, null)
    } else {
        Toast.makeText(ctx, "홈 화면을 길게 누르고 위젯 > PinTodo를 추가하세요", Toast.LENGTH_LONG).show()
    }
}
