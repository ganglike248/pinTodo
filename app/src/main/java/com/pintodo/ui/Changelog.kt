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
 * 사용자가 체감하는 변화만 적는다. 버전을 올릴 때마다 맨 위에 추가할 것.
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
        "0.4.1", "2026.10.02",
        listOf(
            moved("플레이스토어 출시 준비로 앱 식별자가 바뀌어서, 이전 버전과는 별개의 앱으로 새로 설치돼요. 이전 버전의 할 일은 옮겨지지 않아요"),
        ),
    ),
    Release(
        "0.4.0", "2026.10.02",
        listOf(
            moved("앱 이름이 'PinTodo'로 바뀌었어요"),
            new("설정 > 버전을 누르면 지금 보시는 업데이트 기록을 볼 수 있어요"),
            moved("설정에서 GitHub 항목을 뺐어요"),
        ),
    ),
    Release(
        "0.3.0", "2026.10.02",
        listOf(
            new("알림 없는 할 일: 할 일마다 '알림 받기'를 끄면 알림창엔 안 뜨고 목록·위젯에만 보여요"),
            new("빠른 추가에 '알림 없이' 선택지가 생겼어요"),
            new("설정 > 새 할 일 기본값에서 '알림 받기'를 기본으로 끌 수 있어요"),
            fixed("위젯이 늦게 바뀌던 문제를 고쳤어요. 이제 할 일을 바꾸면 바로 반영돼요"),
            improved("위젯의 빈 곳을 누르면 앱이 열려요"),
            improved("일정의 시작·종료처럼 눌러서 바꾸는 항목에 '>' 표시가 붙었어요"),
        ),
    ),
    Release(
        "0.2.0", "2026.10.02",
        listOf(
            new("아래 메뉴(할 일 / 기록 / 설정)가 생겼어요"),
            moved("완료한 할 일은 목록 아래 대신 '기록' 탭에서 날짜별로 볼 수 있어요"),
            new("홈 화면 위젯: 할 일 목록을 보고 바로 완료하거나 추가할 수 있어요"),
            new("알림창 빠른 설정에 '할 일 추가' 타일을 넣을 수 있어요"),
            new("미루기 선택지(10분 ~ 내일 아침)를 설정에서 고를 수 있어요"),
            moved("알림 버튼이 [완료] [1시간 뒤에]에서 [완료] [10분 뒤] [미루기…]로 바뀌었어요"),
            new("목록에서 카드를 왼쪽으로 밀면 미룰 수 있어요"),
            improved("시각을 휠을 돌려서 고를 수 있어요"),
            new("알림 방식을 고를 때 지금 휴대폰 모드(진동·무음 등)에서 실제로 어떻게 울리는지 알려줘요"),
            improved("화면 디자인을 새로 다듬고 위쪽 제목을 작게 줄였어요"),
            fixed("빠른 추가 창에서 키보드가 바로 안 올라오던 문제를 고쳤어요"),
        ),
    ),
    Release(
        "0.1.0", "2026.10.02",
        listOf(
            new("할 일을 완료할 때까지 알림창에 고정해요. 밀어서 지워도 다시 나타나요"),
            new("소리·진동은 처음 뜰 때 한 번만 울려요"),
            new("한 번(시작~종료) 또는 반복(요일·시간대) 일정을 정할 수 있어요"),
            new("휴대폰을 껐다 켜거나 앱을 업데이트해도 알림이 그대로 돌아와요"),
            new("카드를 밀어서 완료하고, 실수하면 실행 취소할 수 있어요"),
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
