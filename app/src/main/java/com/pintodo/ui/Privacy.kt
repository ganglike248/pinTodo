package com.pintodo.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 문의·의견 받는 메일 (GitHub은 일반 사용자에게 낯설어서 메일로) */
const val CONTACT_EMAIL = "business9498@gmail.com"

/**
 * 메일 앱으로 의견 보내기. 답장에 도움이 되도록 앱 버전·기기를 본문 아래에 넣음.
 * 메일 앱이 없으면 주소를 복사해 둠
 */
fun sendFeedback(ctx: Context) {
    val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "-"
    val body = "\n\n\n---\nPinTodo $version · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, "[PinTodo] 의견")
        .putExtra(Intent.EXTRA_TEXT, body)
    runCatching { ctx.startActivity(intent) }.onFailure { copyEmail(ctx) }
}

private fun copyEmail(ctx: Context) {
    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("PinTodo 문의 메일", CONTACT_EMAIL))
    Toast.makeText(ctx, "메일 주소를 복사했어요: $CONTACT_EMAIL", Toast.LENGTH_LONG).show()
}

private class PolicySection(val title: String, val body: List<String>)

/** docs/privacy-policy.md(스토어 등록용 웹 페이지)와 같은 내용을 앱 안에서 보여 줌. 바꿀 때 두 곳을 함께 */
private const val POLICY_DATE = "2026년 11월 18일"
private val POLICY = listOf(
    PolicySection("1. 수집하는 정보", listOf(
        "앱은 이름, 연락처, 위치, 기기 식별자, 사용 기록 등 어떠한 개인정보도 수집하거나 외부로 보내지 않아요.",
        "앱에는 광고, 분석(통계), 로그인, 외부 서버가 없어요.",
    )),
    PolicySection("2. 기기에 저장되는 정보", listOf(
        "입력한 할 일(제목, 메모, 체크리스트, 일정)과 앱 설정은 휴대폰 안에만 저장돼요.",
        "개발자를 포함한 누구도 이 정보를 볼 수 없고, 앱을 삭제하면 함께 지워져요.",
    )),
    PolicySection("3. 백업", listOf(
        "Android 자동 백업: 휴대폰의 Google 백업이 켜져 있으면, Android가 할 일과 설정을 내 Google 계정에 암호화해 백업해요. Google이 관리하며 개발자는 볼 수 없어요. 휴대폰 설정에서 끌 수 있어요.",
        "백업 파일: 설정 > 백업에서 직접 만든 파일은 내가 고른 위치에만 저장돼요.",
    )),
)
private val PERMISSIONS = listOf(
    "알림" to "할 일을 알림창에 표시",
    "알람 및 리마인더" to "정한 시각에 알림을 띄우고 내리기",
    "부팅 완료 수신" to "휴대폰을 다시 켠 뒤 알림 복원",
    "진동" to "할 일이 처음 뜰 때 진동",
)
private val POLICY_AFTER = listOf(
    PolicySection("5. 아동의 개인정보", listOf("앱은 누구의 정보도 수집하지 않으므로 아동의 정보도 수집하지 않아요.")),
    PolicySection("6. 방침의 변경", listOf("방침이 바뀌면 앱과 스토어의 개인정보처리방침에 바뀐 내용과 시행일을 알려요.")),
)

@Composable
fun PrivacyDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    FullPageDialog("개인정보처리방침", onClose) {
        item {
            GroupCard {
                Column(Modifier.padding(20.dp)) {
                    Text("PinTodo는 개인정보를 수집하지 않아요", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("시행일: $POLICY_DATE", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        POLICY.forEach { item { PolicyCard(it) } }
        item {
            // 표 대신 줄마다 권한 · 용도 (좁은 화면에서도 깨지지 않게)
            GroupCard {
                Column(Modifier.padding(20.dp)) {
                    Text("4. 사용하는 권한", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    PERMISSIONS.forEachIndexed { i, (name, use) ->
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row {
                            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(120.dp))
                            Text(use, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("권한은 위 기능에만 쓰이며, 어떤 정보도 밖으로 보내지 않아요.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        POLICY_AFTER.forEach { item { PolicyCard(it) } }
        item {
            GroupCard {
                Column(Modifier.fillMaxWidth().clickable(onClickLabel = "메일 보내기") { sendFeedback(ctx) }.padding(20.dp)) {
                    Text("7. 문의", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(CONTACT_EMAIL, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PolicyCard(section: PolicySection) {
    GroupCard {
        Column(Modifier.padding(20.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleMedium)
            section.body.forEach {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
