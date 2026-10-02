package com.ganglike.pintodo.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganglike.pintodo.data.SettingsStore

/*
 * 디자인 기준: 토스 디자인 시스템(TDS)의 회색 단계 + 파란 포인트 색
 * - 페이지 배경은 연한 회색, 카드는 테두리 없는 흰색 (background / surface)
 * - 칩·입력창 채움은 surfaceVariant, 보조 문구는 onSurfaceVariant / outline
 */
private object Tds {
    val Blue500 = Color(0xFF3182F6)
    val Blue50 = Color(0xFFE8F3FF)
    val Blue700 = Color(0xFF1B64DA)
    val Grey50 = Color(0xFFF9FAFB)
    val Grey100 = Color(0xFFF2F4F6)
    val Grey200 = Color(0xFFE5E8EB)
    val Grey300 = Color(0xFFD1D6DB)
    val Grey500 = Color(0xFF8B95A1)
    val Grey600 = Color(0xFF6B7684)
    val Grey700 = Color(0xFF4E5968)
    val Grey900 = Color(0xFF191F28)
    val Red500 = Color(0xFFF04452)
    val Orange500 = Color(0xFFFE9800)
}

private val Light = lightColorScheme(
    primary = Tds.Blue500,
    onPrimary = Color.White,
    primaryContainer = Tds.Blue50,
    onPrimaryContainer = Tds.Blue700,
    secondaryContainer = Tds.Blue50,
    onSecondaryContainer = Tds.Blue700,
    tertiary = Tds.Orange500,
    tertiaryContainer = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFFB36A00),
    background = Tds.Grey100,
    onBackground = Tds.Grey900,
    surface = Color.White,
    onSurface = Tds.Grey900,
    surfaceVariant = Tds.Grey100,
    onSurfaceVariant = Tds.Grey600,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Tds.Grey100,
    outline = Tds.Grey500,
    outlineVariant = Tds.Grey200,
    error = Tds.Red500,
    errorContainer = Color(0xFFFFEEEE),
    onErrorContainer = Color(0xFFD22030),
    inverseSurface = Tds.Grey900,
    inverseOnSurface = Tds.Grey50,
    inversePrimary = Color(0xFF90C2FF),
    scrim = Color(0xFF000000),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF4B96FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1C2E4A),
    onPrimaryContainer = Color(0xFF9DC4FF),
    secondaryContainer = Color(0xFF1C2E4A),
    onSecondaryContainer = Color(0xFF9DC4FF),
    tertiary = Color(0xFFFFA733),
    tertiaryContainer = Color(0xFF3A2A12),
    onTertiaryContainer = Color(0xFFFFC266),
    background = Color(0xFF17171C),
    onBackground = Color(0xFFE4E4E5),
    surface = Color(0xFF202027),
    onSurface = Color(0xFFE4E4E5),
    surfaceVariant = Color(0xFF2C2C35),
    onSurfaceVariant = Color(0xFF9E9EA4),
    surfaceContainerLowest = Color(0xFF202027),
    surfaceContainerLow = Color(0xFF202027),
    surfaceContainer = Color(0xFF202027),
    surfaceContainerHigh = Color(0xFF26262E),
    surfaceContainerHighest = Color(0xFF2C2C35),
    outline = Color(0xFF6D6D78),
    outlineVariant = Color(0xFF2C2C35),
    error = Color(0xFFFF6B75),
    errorContainer = Color(0xFF3D1C20),
    onErrorContainer = Color(0xFFFF9AA1),
)

/** Material You(배경화면 색) 사용 시: 배경/카드가 구분되도록 단계만 맞춤 */
private fun ColorScheme.asAppScheme() = copy(
    background = surfaceContainer,
    surface = surfaceContainerLowest,
    surfaceVariant = surfaceContainerHighest,
    surfaceContainerLow = surfaceContainerLowest,
    surfaceContainerHigh = surfaceContainerLowest,
)

private val AppTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
        titleLarge = titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
        titleMedium = titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
        titleSmall = titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
        bodyMedium = bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
        bodySmall = bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
        labelLarge = labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        labelMedium = labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 화면 간격 등 디자인 값 */
object Dimens {
    val ScreenPadding = 20.dp
    val CardRadius = 20.dp
    val ButtonHeight = 56.dp
}

@Composable
fun PinTodoTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val settings by SettingsStore.flow(ctx).collectAsStateWithLifecycle()
    val dark = isSystemInDarkTheme()
    val colors = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            (if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)).asAppScheme()
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
}

/** 미루기 등 주의 색 */
val ColorScheme.warning get() = tertiary
