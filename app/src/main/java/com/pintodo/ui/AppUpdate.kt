package com.pintodo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * 앱 안 업데이트 안내 (Google Play In-App Updates, 유연한 업데이트).
 * 새 버전이 있으면 Play 창으로 한 번 묻고(버전마다 1회), 받는 동안 앱은 그대로 쓰고,
 * 다 받으면 스낵바 [다시 시작]으로 설치. Play에서 설치하지 않은 앱(사이드로드)에서는 아무 일도 없음
 */
@Composable
fun AppUpdatePrompt(snackbar: SnackbarHostState) {
    val ctx = LocalContext.current
    val manager = remember { AppUpdateManagerFactory.create(ctx) }
    var downloaded by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    DisposableEffect(manager) {
        val listener = InstallStateUpdatedListener { if (it.installStatus() == InstallStatus.DOWNLOADED) downloaded = true }
        manager.registerListener(listener)
        onDispose { manager.unregisterListener(listener) }
    }

    // 앱으로 돌아올 때마다 확인: 받아 둔 업데이트가 있으면 다시 안내, 새 버전이면 (그 버전에 처음일 때만) 묻기
    LifecycleResumeEffect(manager) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                downloaded = true
                return@addOnSuccessListener
            }
            val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            val prefs = uiPrefs(ctx)
            if (!available || prefs.getInt(KEY_ASKED, 0) >= info.availableVersionCode()) return@addOnSuccessListener
            prefs.edit().putInt(KEY_ASKED, info.availableVersionCode()).apply()
            runCatching { manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)) }
        }
        onPauseOrDispose {}
    }

    LaunchedEffect(downloaded) {
        if (!downloaded) return@LaunchedEffect
        val result = snackbar.showSnackbar("새 버전을 받았어요", actionLabel = "다시 시작", duration = SnackbarDuration.Indefinite)
        if (result == SnackbarResult.ActionPerformed) manager.completeUpdate()
        else downloaded = false   // 다른 안내에 밀려 닫히면 다음에 앱으로 돌아올 때 다시 안내
    }
}

private const val KEY_ASKED = "update_asked_version"
