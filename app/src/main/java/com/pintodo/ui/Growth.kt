package com.pintodo.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.play.core.review.ReviewManagerFactory
import com.pintodo.data.AlertMode
import com.pintodo.data.LabelColor
import com.pintodo.data.RepeatType
import com.pintodo.data.Todo

/** 처음 쓰는 사람이 바로 '알림창에 고정되는 순간'을 겪게 하는 예시 할 일 */
private class Example(val icon: ImageVector, val title: String, val desc: String, val make: (Context) -> Todo)

private val EXAMPLES = listOf(
    Example(Icons.Rounded.PushPin, "알림 밀어 보기", "지금 알림창에 띄워요. 밀어서 지워도 다시 나타나요") { ctx ->
        newTodo(ctx, "알림을 밀어서 지워 보세요").copy(memo = "다시 나타나요. 다 확인했으면 [완료]를 누르세요")
    },
    Example(Icons.Rounded.WaterDrop, "물 마시기", "완료할 때까지 2시간마다 진동으로 다시 알려요") { ctx ->
        newTodo(ctx, "물 한 잔 마시기").copy(remindEvery = 120, alertMode = AlertMode.VIBRATE, color = LabelColor.BLUE)
    },
    Example(Icons.Rounded.Medication, "매일 약 먹기", "매일 오전 9시부터 오전 10시까지 알림창에 떠요") { ctx ->
        newTodo(ctx, "약 먹기").copy(
            repeatType = RepeatType.WEEKLY, repeatDays = (1..7).toSet(),
            dailyStart = 9 * 60, dailyEnd = 10 * 60, color = LabelColor.PURPLE,
        )
    },
)

/** 할 일이 하나도 없을 때 빈 화면 아래에 */
@Composable
fun ExampleStarter(onPick: (Todo) -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("예시로 바로 시작해 보기")
        EXAMPLES.forEach { ex ->
            Surface(onClick = { onPick(ex.make(ctx)) }, shape = RoundedCornerShape(Dimens.CardRadius), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(ex.icon, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(ex.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * 앱 안 별점 요청 (Google Play In-App Review). 앱에서 할 일을 5개 완료한 만족스러운 순간에 한 번만.
 * 실제로 창을 띄울지는 Play가 정하고(횟수 제한), Play에서 설치하지 않은 앱에서는 아무 일도 없음
 */
object ReviewPrompt {
    private const val KEY_DONE = "done_count"
    private const val KEY_ASKED = "review_asked"
    private const val AFTER = 5

    fun onCompleted(activity: Activity) {
        val p = uiPrefs(activity)
        val n = p.getInt(KEY_DONE, 0) + 1
        p.edit().putInt(KEY_DONE, n).apply()
        if (n < AFTER || p.getBoolean(KEY_ASKED, false)) return
        p.edit().putBoolean(KEY_ASKED, true).apply()
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
        }
    }
}
