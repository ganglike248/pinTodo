#!/bin/zsh
# 스토어 스크린샷 원본 촬영 → work/raw/*.png
# 에뮬레이터 Medium_Phone(1080×2400, 시스템 언어 한국어, 키보드 끔, 홈 맨 위에 PinTodo 위젯 가로 꽉·4줄)
# 미리: ./gradlew assembleDebug && adb -e install -r app/build/outputs/apk/debug/app-debug.apk
P=$(cd "$(dirname "$0")" && pwd); source $P/ui.sh
adb -e shell pm grant com.pintodo android.permission.POST_NOTIFICATIONS
adb -e shell appops set com.pintodo SCHEDULE_EXACT_ALARM allow
adb -e shell dumpsys deviceidle whitelist +com.pintodo >/dev/null
demo

# 목록·위젯
APP list; clock; shot list
adb -e shell am start -n com.pintodo/.ui.MainActivity --es new_title "'내일 오후 3시 약국 가기'" >/dev/null 2>&1; sleep 3; clock; shot newtodo   # 새 할 일(날짜 인식)
adb -e shell input keyevent BACK; sleep 1; adb -e shell input keyevent BACK; sleep 1
APP list; adb -e shell am start -n com.pintodo/.ui.MainActivity --ei todo_id 2 >/dev/null 2>&1; sleep 3; clock; shot check   # 장보기 체크리스트 편집
adb -e shell input keyevent BACK; sleep 1
APP list; adb -e shell input swipe 900 1150 100 1150 300; sleep 2; clock; shot snooze   # '엄마한테 전화하기' 밀기 → 미루기 시트
adb -e shell input keyevent BACK; sleep 1
adb -e shell am start -n com.pintodo/.ui.MainActivity --ei todo_id 4 >/dev/null; sleep 3
adb -e shell input swipe 540 1700 540 1150 800; sleep 1.5; clock; shot edit
adb -e shell input keyevent BACK; sleep 1; adb -e shell input keyevent HOME; sleep 2; clock; shot home
