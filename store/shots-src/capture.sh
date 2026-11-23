#!/bin/zsh
# 스토어 스크린샷 원본 촬영 → work/raw/*.png
# 에뮬레이터 Medium_Phone(1080×2400, 시스템 언어 한국어, 키보드 끔, 홈 맨 위에 PinTodo 위젯 가로 꽉·4줄)
# 미리: ./gradlew assembleDebug && adb -e install -r app/build/outputs/apk/debug/app-debug.apk
#       zsh store/promo-src/reminder-app/build.sh ('보통 알림' 역할의 작은 앱, 1번 비교 장면용)
P=$(cd "$(dirname "$0")" && pwd); source $P/ui.sh
adb -e shell pm grant com.pintodo android.permission.POST_NOTIFICATIONS
adb -e shell appops set com.pintodo SCHEDULE_EXACT_ALARM allow
adb -e shell dumpsys deviceidle whitelist +com.pintodo >/dev/null
demo
CLEAN(){ SHADE; tap 'content-desc="무음 알림 모두 삭제"' >/dev/null; CLOSE; }

# 목록·위젯
APP list; clock; shot list
adb -e shell am start -n com.pintodo/.ui.QuickAddActivity -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "'내일 오후 3시 약국 가기'" >/dev/null; sleep 3; clock; shot quick
adb -e shell input keyevent BACK; sleep 1
APP list; adb -e shell input swipe 900 1150 100 1150 300; sleep 2; clock; shot snooze   # '엄마한테 전화하기' 밀기 → 미루기 시트
adb -e shell input keyevent BACK; sleep 1
adb -e shell am start -n com.pintodo/.ui.MainActivity --ei todo_id 4 >/dev/null; sleep 3
adb -e shell input swipe 540 1700 540 1150 800; sleep 1.5; clock; shot edit
adb -e shell input keyevent BACK; sleep 1; adb -e shell input keyevent HOME; sleep 2; clock; shot home

# 알림 3개 묶음 → 펼치고 맨 위(신규 · 장보기 체크리스트) 펼치기
APP noti; adb -e shell input keyevent HOME; sleep 1; CLEAN; SHADE; sleep 1
adb -e shell input tap 950 610; sleep 1.5; adb -e shell input tap 961 802; sleep 1.5; shot noti

# 1번 비교: 보통 알림 + PinTodo 알림
CLOSE; APP one; adb -e shell input keyevent HOME; sleep 1; CLEAN
adb -e shell am start -n com.demo.remind/.Post --es t "'택배 반품 신청'" --es x "'오늘 자정까지 수거 신청하기'" --ei id 11 >/dev/null; sleep 1.5
adb -e shell input keyevent HOME; sleep 1; SHADE; sleep 1.5; tap 'content-desc="접기"' >/dev/null; sleep 1.2; shot cmp
# '모두 지우기' → 보통 알림만 사라지고 PinTodo는 다시 나타남
tap 'text="모두 지우기"' >/dev/null; sleep 3; SHADE; sleep 1.5; shot kept
CLOSE
