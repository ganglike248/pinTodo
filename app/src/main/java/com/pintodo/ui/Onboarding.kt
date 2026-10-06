package com.pintodo.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.pintodo.notify.Notifier
import com.pintodo.notify.Sync

private const val KEY_ONBOARDED = "onboarded"

/** 처음 실행이고 필요한 허용이 하나라도 빠졌으면 안내 화면을 보여줌 */
fun needsOnboarding(ctx: Context): Boolean {
    val prefs = uiPrefs(ctx)
    if (prefs.getBoolean(KEY_ONBOARDED, false)) return false
    val allOk = Notifier.enabled(ctx) && Sync.canExact(ctx) && ignoringBattery(ctx)
    if (allOk) prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
    return !allOk
}

private class Step(val title: String, val body: String, val ok: Boolean, val action: () -> Unit)

/** 처음 실행 안내: 알림 → 알람 및 리마인더 → 배터리 순서로 하나씩 허용 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var notifOk by remember { mutableStateOf(Notifier.enabled(ctx)) }
    var exactOk by remember { mutableStateOf(Sync.canExact(ctx)) }
    var batteryOk by remember { mutableStateOf(ignoringBattery(ctx)) }
    var askedNotif by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        notifOk = Notifier.enabled(ctx)
        exactOk = Sync.canExact(ctx)
        batteryOk = ignoringBattery(ctx)
        onPauseOrDispose { }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOk = Notifier.enabled(ctx)
        Sync.run(ctx)
    }

    val steps = buildList {
        add(Step("알림 허용", "할 일을 알림창에 띄우려면 꼭 필요해요.", notifOk) {
            // 한 번 거절하면 시스템이 다시 묻지 않으므로 그다음부터는 설정 화면으로
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotif) {
                askedNotif = true
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ctx.startActivity(notificationSettings(ctx))
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Step("알람 및 리마인더", "정한 시각에 늦지 않게 알림을 띄우고 내려요. 허용하지 않으면 몇 분 늦을 수 있어요.", exactOk) {
                ctx.startActivity(exactAlarmSettings(ctx))
            })
        }
        add(Step("배터리 '제한 없음'", "절전 때문에 지운 알림이 다시 안 뜨거나 늦는 걸 막아요. 앱 정보 > 배터리에서 '제한 없음'을 골라 주세요.", batteryOk) {
            ctx.startActivity(batteryExemption(ctx))
        })
    }
    val current = steps.indexOfFirst { !it.ok }
    fun finish() {
        uiPrefs(ctx).edit().putBoolean(KEY_ONBOARDED, true).apply()
        Sync.run(ctx)
        onDone()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(40.dp))
            Text("PinTodo를 시작하기 전에", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "할 일이 제때 뜨고, 완료할 때까지 사라지지 않으려면 아래 허용이 필요해요. 위에서부터 하나씩 눌러 주세요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { i, step -> StepCard(i + 1, step, highlighted = i == current) }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(if (current == -1) "시작하기" else "다음에 하고 시작하기", ::finish)
        if (current != -1) {
            Text(
                "나중에 설정 > 시스템에서도 바꿀 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp),
            )
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepCard(number: Int, step: Step, highlighted: Boolean) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Dimens.CardRadius)
    Surface(
        color = c.surface,
        shape = shape,
        modifier = if (highlighted) Modifier.border(1.5.dp, c.primary, shape) else Modifier,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(28.dp).background(if (step.ok) c.primary else c.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (step.ok) Icon(Icons.Rounded.Check, "완료", Modifier.size(18.dp), tint = c.onPrimary)
                else Text("$number", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(step.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(step.body, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                if (!step.ok) {
                    TextButton(onClick = step.action, contentPadding = PaddingValues(horizontal = 0.dp), modifier = Modifier.padding(top = 2.dp)) {
                        Text("허용하기", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("허용됨", style = MaterialTheme.typography.labelMedium, color = c.primary, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}
