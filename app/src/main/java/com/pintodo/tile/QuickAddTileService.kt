package com.pintodo.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pintodo.data.Status
import com.pintodo.data.TodoStore
import com.pintodo.ui.QuickAddActivity

/** 알림창 빠른 설정 타일: 누르면 바로 할 일 추가 시트가 열림 */
class QuickAddTileService : TileService() {

    override fun onStartListening() {
        val tile = qsTile ?: return
        val now = System.currentTimeMillis()
        val showing = TodoStore.all(this).count { it.status(now) == Status.SHOWING }
        tile.state = Tile.STATE_INACTIVE
        tile.label = "할 일 추가"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (showing > 0) "알림 중 ${showing}개" else "PinTodo"
        }
        tile.updateTile()
    }

    override fun onClick() {
        if (isLocked) unlockAndRun { openQuickAdd() } else openQuickAdd()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openQuickAdd() {
        val intent = Intent(this, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        fun component(context: Context) = ComponentName(context, QuickAddTileService::class.java)

        /** 타일 부제(알림 중 개수) 갱신 요청 */
        fun refresh(context: Context) {
            runCatching { requestListeningState(context, component(context)) }
        }
    }
}
