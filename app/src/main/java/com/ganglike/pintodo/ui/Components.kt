package com.ganglike.pintodo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ganglike.pintodo.data.AlertMode

fun AlertMode.icon(): ImageVector = when (this) {
    AlertMode.SILENT -> Icons.Rounded.NotificationsOff
    AlertMode.SOUND -> Icons.Rounded.MusicNote
    AlertMode.VIBRATE -> Icons.Rounded.Vibration
    AlertMode.BOTH -> Icons.Rounded.NotificationsActive
}

/** 탭 화면 상단 제목 (작고 단정하게) */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    inset: Dp = 4.dp,  // 목록 안(좌우 16dp 여백)에서 쓰는 기준
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(start = inset, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

/** 섹션 제목 (회색 작은 글씨) */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** 테두리 없는 흰 카드 묶음 */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 6.dp), content = content)
    }
}

/** 제목 + 섹션 카드 */
@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionLabel(title)
        GroupCard(content = content)
    }
}

@Composable
fun SettingRow(
    title: String,
    value: String? = null,
    icon: ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    dense: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val vertical = if (dense) 0.dp else 14.dp
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 20.dp, end = if (trailing != null) 12.dp else 20.dp, top = vertical, bottom = vertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconBadge(icon)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
        }
        when {
            trailing != null -> trailing()
            // 눌러서 바꾸는 줄임을 알 수 있게
            onClick != null -> Icon(
                Icons.Rounded.ChevronRight, null,
                Modifier.size(22.dp), tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** 둥근 회색 배경 아이콘 */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(size * 0.55f), tint = tint)
    }
}

/** 하단 큰 파란 버튼 */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().height(Dimens.ButtonHeight),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            disabledContentColor = Color.White,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** 회색 보조 버튼 */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(Dimens.ButtonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** 둘 중 하나 고르는 알약 모양 탭 (한 번 / 반복) */
@Composable
fun SegmentTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSel = i == selected
            val bg by animateColorAsState(if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent, label = "tab")
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(11.dp)).background(bg).clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 아이콘 + 라벨 선택 타일 (알림 방식) */
@Composable
fun ChoiceTile(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) c.primaryContainer else c.surfaceVariant, label = "tileBg")
    val fg = if (selected) c.primary else c.onSurfaceVariant
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (selected) Modifier.border(1.5.dp, c.primary, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = fg)
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

/** 작은 칩 (빠른 선택) */
@Composable
fun SoftChip(label: String, selected: Boolean = false, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) c.primaryContainer else c.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) c.primary else c.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 원형 체크 (완료 버튼) */
@Composable
fun CheckCircle(checked: Boolean, active: Boolean, onClick: (() -> Unit)?, size: Dp = 26.dp) {
    val c = MaterialTheme.colorScheme
    val ring = if (active) c.primary else c.outline.copy(alpha = 0.6f)
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (checked) c.primary else Color.Transparent)
                .then(if (!checked) Modifier.border(2.dp, ring, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Rounded.Check, null, Modifier.size(size * 0.65f), tint = c.onPrimary)
        }
    }
}

/** 안내 박스 (정보/주의) */
@Composable
fun InfoBox(icon: ImageVector, text: String, warn: Boolean = false, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (warn) c.tertiaryContainer else c.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, Modifier.padding(top = 1.dp).size(18.dp), tint = if (warn) c.onTertiaryContainer else c.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (warn) c.onTertiaryContainer else c.onSurfaceVariant)
    }
}
