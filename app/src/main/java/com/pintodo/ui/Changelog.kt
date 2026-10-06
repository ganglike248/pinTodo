@file:OptIn(ExperimentalMaterial3Api::class)

package com.pintodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.ui.window.DialogProperties

/**
 * 업데이트 기록 (설정 > 버전).
 * 사용자가 체감하는 변화만, 버전당 3~5줄 이내의 짧고 쉬운 말로. 버전을 올릴 때마다 맨 위에 추가할 것.
 */
enum class ChangeKind(val label: String) { NEW("새 기능"), IMPROVED("개선"), MOVED("변경"), FIXED("수정") }

data class Change(val kind: ChangeKind, val text: String)
data class Release(val version: String, val date: String, val changes: List<Change>)

private fun new(t: String) = Change(ChangeKind.NEW, t)
private fun improved(t: String) = Change(ChangeKind.IMPROVED, t)
private fun moved(t: String) = Change(ChangeKind.MOVED, t)
private fun fixed(t: String) = Change(ChangeKind.FIXED, t)

val RELEASES = listOf(
    Release(
        "0.4.6", "2026.10.06",
        listOf(
            improved("위젯에서 할 일 목록을 스크롤할 수 있어요"),
            fixed("일정의 한 번·반복·알림 없이를 바꿀 때 두 칸이 같이 깜빡이던 문제를 고쳤어요"),
        ),
    ),
    Release(
        "0.4.5", "2026.10.06",
        listOf(
            new("할 일을 정한 간격마다 다시 알려 줄 수 있어요"),
            new("설정에서 밤에는 다시 울리지 않게 할 수 있어요"),
            new("처음 쓸 때 예시 할 일로 바로 시작할 수 있어요"),
            new("설정에서 별점 남기기, 친구에게 알려주기, 의견 보내기를 할 수 있어요"),
        ),
    ),
    Release(
        "0.4.4", "2026.10.06",
        listOf(
            new("할 일 안에 체크리스트를 만들 수 있어요"),
            new("할 일에 색 라벨을 붙일 수 있어요"),
            new("반복을 날짜나 횟수로 끝낼 수 있어요"),
            new("휴대폰을 바꾸면 Google 백업으로 할 일이 옮겨져요"),
            improved("편집 화면과 설정을 더 단순하게 정리했어요"),
        ),
    ),
    Release(
        "0.4.3", "2026.10.06",
        listOf(
            improved("시간 휠이 바로 보이고, 시각은 오전/오후로 보여요"),
            new("'내일 오후 3시'처럼 쓰면 일정이 자동으로 맞춰져요"),
            new("격주·매월·며칠마다 반복할 수 있어요"),
            new("다른 앱에서 공유해 추가하고, 파일로 백업할 수 있어요"),
            improved("알림에 남은 시간, 기록에 주간 통계가 보여요"),
        ),
    ),
    Release(
        "0.4.2", "2026.10.06",
        listOf(
            new("스마트워치에도 알림이 가요"),
            improved("알림에 정해 둔 시간이 함께 보여요"),
            improved("기록·설정에서 뒤로가기를 누르면 할 일 탭으로 가요"),
        ),
    ),
    Release(
        "0.4.1", "2026.10.02",
        listOf(moved("앱이 새로 설치돼요. 이전 버전의 할 일은 옮겨지지 않아요")),
    ),
    Release(
        "0.4.0", "2026.10.02",
        listOf(
            moved("앱 이름이 PinTodo로 바뀌었어요"),
            new("설정 > 버전에서 업데이트 기록을 볼 수 있어요"),
        ),
    ),
    Release(
        "0.3.0", "2026.10.02",
        listOf(
            new("알림 없이 목록에만 두는 할 일을 만들 수 있어요"),
            fixed("위젯이 늦게 바뀌던 문제를 고쳤어요"),
        ),
    ),
    Release(
        "0.2.0", "2026.10.02",
        listOf(
            new("할 일 / 기록 / 설정 메뉴가 생겼어요"),
            new("홈 화면 위젯과 빠른 설정 타일을 쓸 수 있어요"),
            new("미루기 시간을 골라서 미룰 수 있어요"),
        ),
    ),
    Release(
        "0.1.0", "2026.10.02",
        listOf(
            new("완료할 때까지 알림창에 고정되는 할 일"),
            new("소리·진동은 처음 한 번만 울려요"),
        ),
    ),
)

@Composable
private fun kindColor(kind: ChangeKind): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (kind) {
        ChangeKind.NEW -> c.primaryContainer to c.primary
        ChangeKind.IMPROVED -> if (isSystemInDarkTheme()) Color(0xFF15372A) to Color(0xFF3DD68C)
        else Color(0xFFE5F8EF) to Color(0xFF02A262)
        ChangeKind.MOVED -> c.tertiaryContainer to c.onTertiaryContainer
        ChangeKind.FIXED -> c.surfaceVariant to c.onSurfaceVariant
    }
}

@Composable
fun ChangelogDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // 다이얼로그 창은 상태바 아이콘 색을 따로 맞춰야 밝은 배경에서 보임
        val view = LocalView.current
        val dark = isSystemInDarkTheme()
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { w ->
                WindowCompat.getInsetsController(w, w.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로") }
                    },
                    title = { Text("업데이트 기록", style = MaterialTheme.typography.titleMedium) },
                )
            },
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(RELEASES, key = { it.version }) { release ->
                    GroupCard {
                        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                            Text("v${release.version}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Text(release.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        release.changes.forEach { ChangeRow(it) }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeRow(change: Change) {
    val (bg, fg) = kindColor(change.kind)
    Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.width(52.dp).clip(RoundedCornerShape(6.dp)).background(bg).padding(vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(change.kind.label, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(change.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
