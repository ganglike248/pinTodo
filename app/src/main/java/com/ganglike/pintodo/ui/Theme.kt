package com.ganglike.pintodo.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFF4A66E8),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF00105B),
    secondaryContainer = Color(0xFFE0E1F9),
    surface = Color(0xFFFBF8FF),
    surfaceContainerLow = Color(0xFFF4F2FC),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    primaryContainer = Color(0xFF2E47C4),
    onPrimaryContainer = Color(0xFFDDE1FF),
    surface = Color(0xFF121318),
    surfaceContainerLow = Color(0xFF1A1B21),
)

@Composable
fun PinTodoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    // Android 12+ 에서는 배경화면 색에 맞춘 Material You 색상 사용
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colors, content = content)
}
