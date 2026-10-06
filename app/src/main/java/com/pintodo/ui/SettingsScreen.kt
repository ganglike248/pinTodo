package com.pintodo.ui

import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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

private enum class SettingSheet { QUICK_SNOOZE, ALERT_MODE }

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
            Section("미루기") {
                SettingRow(
                    title = "알림에 바로 보이는 버튼",
                    value = s.quickSnooze.buttonLabel,
                    icon = Icons.Rounded.Snooze,
                    valueColor = MaterialTheme.colorScheme.primary,
                    onClick = { sheet = SettingSheet.QUICK_SNOOZE },
                )
                Divider()
                Text(
                    "'미루기…'를 눌렀을 때 보여줄 선택지",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 2.dp),
                )
                SnoozeOption.entries.forEach { option ->
                    val checked = option in s.snoozeOptions
                    SettingRow(
                        title = option.label,
                        dense = true,
                        onClick = { update { it.copy(snoozeOptions = toggle(it.snoozeOptions, option)) } },
                        trailing = {
                            Checkbox(checked, onCheckedChange = { update { it.copy(snoozeOptions = toggle(it.snoozeOptions, option)) } })
                        },
                    )
                }
                InfoBox(
                    Icons.Rounded.Info,
                    "알림에는 [완료] [${s.quickSnooze.buttonLabel}] [미루기…] 버튼이 보여요. 목록에서 카드를 왼쪽으로 밀어도 미룰 수 있어요.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        item {
            Section("새 할 일 기본값") {
                SettingRow(
                    title = "알림 받기",
                    value = if (s.defaultNotify) "알림창에 표시해요" else "알림 없이 목록·위젯에만 추가해요",
                    icon = Icons.Rounded.Notifications,
                    onClick = { update { it.copy(defaultNotify = !it.defaultNotify) } },
                    trailing = { Switch(s.defaultNotify, { v -> update { it.copy(defaultNotify = v) } }) },
                )
                Divider()
                SettingRow(
                    title = "알림창에 고정",
                    value = if (s.defaultPinned) "밀어서 지워도 다시 나타나요" else "밀어서 지우면 이번엔 숨겨요",
                    icon = Icons.Rounded.PushPin,
                    onClick = { update { it.copy(defaultPinned = !it.defaultPinned) } },
                    trailing = { Switch(s.defaultPinned, { v -> update { it.copy(defaultPinned = v) } }) },
                )
                Divider()
                SettingRow(
                    title = "알림 방식",
                    value = s.defaultAlertMode.label,
                    icon = s.defaultAlertMode.icon(),
                    valueColor = MaterialTheme.colorScheme.primary,
                    onClick = { sheet = SettingSheet.ALERT_MODE },
                )
            }
        }

        item {
            Section("알림창 · 홈 화면") {
                SettingRow(
                    title = "빠른 설정에 '할 일 추가' 타일 넣기",
                    value = "알림창을 내려서 바로 할 일을 추가해요",
                    icon = Icons.Rounded.Tune,
                    onClick = { addTile(ctx) },
                )
                Divider()
                SettingRow(
                    title = "홈 화면에 위젯 추가",
                    value = "진행 중·예정인 할 일을 홈 화면에서 봐요",
                    icon = Icons.Rounded.Widgets,
                    onClick = { addWidget(ctx) },
                )
                Divider()
                SettingRow(
                    title = "스마트워치에도 알림",
                    value = if (s.wearable) "갤럭시 워치·밴드 등 연결된 기기로도 보내요" else "휴대폰 알림창에만 표시해요",
                    icon = Icons.Rounded.Watch,
                    onClick = { update { it.copy(wearable = !it.wearable) } },
                    trailing = { Switch(s.wearable, { v -> update { it.copy(wearable = v) } }) },
                )
                if (s.wearable) {
                    InfoBox(
                        Icons.Rounded.Info,
                        "워치에는 처음 뜰 때 한 번 전달되고, 밀어서 지우면 워치에서는 사라지고 휴대폰에만 남아요. 워치 앱(Galaxy Wearable, Huawei Health 등)의 알림 설정에서 PinTodo가 켜져 있어야 해요.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                Section("화면") {
                    SettingRow(
                        title = "배경화면 색상 사용",
                        value = "Material You — 배경화면에 맞춰 앱 색이 바뀌어요",
                        icon = Icons.Rounded.Palette,
                        onClick = { update { it.copy(dynamicColor = !it.dynamicColor) } },
                        trailing = { Switch(s.dynamicColor, { v -> update { it.copy(dynamicColor = v) } }) },
                    )
                }
            }
        }

        item {
            Section("시스템") {
                SettingRow(
                    title = "알림 설정",
                    value = "채널별 소리·진동, 잠금화면 표시 등",
                    icon = Icons.Rounded.Notifications,
                    onClick = { ctx.startActivity(notificationSettings(ctx)) },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Divider()
                    SettingRow(
                        title = "알람 및 리마인더",
                        value = if (exactOk) "허용됨 · 정한 시각에 바로 알려요" else "허용 안 됨 · 알림이 몇 분 늦을 수 있어요",
                        valueColor = if (exactOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Rounded.Alarm,
                        onClick = { ctx.startActivity(exactAlarmSettings(ctx)) },
                    )
                }
                Divider()
                SettingRow(
                    title = "배터리 절전 예외",
                    value = if (batteryOk) "등록됨" else "등록 안 됨 · 앱 정보 > 배터리 > '제한 없음'",
                    valueColor = if (batteryOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Rounded.BatteryChargingFull,
                    onClick = if (batteryOk) null else ({ ctx.startActivity(batteryExemption(ctx)) }),
                )
            }
        }

        item {
            Section("백업") {
                SettingRow(
                    title = "백업 파일 만들기",
                    value = "할 일과 설정을 파일로 저장해요 (휴대폰을 바꿀 때)",
                    icon = Icons.Rounded.Backup,
                    onClick = { exportLauncher.launch("pintodo-backup-${LocalDate.now()}.json") },
                )
                Divider()
                SettingRow(
                    title = "백업에서 복원",
                    value = "백업 파일을 골라 할 일과 설정을 되돌려요",
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
            }
        }
    }

    when (sheet) {
        SettingSheet.QUICK_SNOOZE -> AppSheet({ sheet = null }) { hide ->
            SheetTitle("알림에 바로 보이는 버튼", "켜 둔 미루기 선택지 중에서 골라요")
            s.enabledSnoozes.forEach { option ->
                SettingRow(
                    title = option.buttonLabel,
                    onClick = { update { it.copy(quickSnooze = option) }; hide { sheet = null } },
                    trailing = { RadioButton(option == s.quickSnooze, onClick = { update { it.copy(quickSnooze = option) }; hide { sheet = null } }) },
                )
            }
        }
        SettingSheet.ALERT_MODE -> AppSheet({ sheet = null }) { hide ->
            SheetTitle("새 할 일 알림 방식")
            AlertModePicker(s.defaultAlertMode) { mode -> update { it.copy(defaultAlertMode = mode) } }
            Spacer(Modifier.height(20.dp))
            PrimaryButton("확인", { hide { sheet = null } })
        }
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
fun AlertModePicker(mode: AlertMode, onChange: (AlertMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AlertMode.entries.forEach { m ->
                ChoiceTile(m.icon(), m.label.replace("소리+진동", "모두"), m == mode, { onChange(m) }, Modifier.weight(1f))
            }
        }
        AlertModeHint(mode)
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
