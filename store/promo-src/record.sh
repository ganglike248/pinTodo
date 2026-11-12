#!/bin/zsh
# 홍보 영상 원본 녹화 (에뮬레이터 Medium_Phone 1080×2400, 시스템 언어 한국어, 키보드 끔, 홈에 PinTodo 위젯 4×4)
# 미리: ./gradlew assembleDebug && adb -e install -r app/build/outputs/apk/debug/app-debug.apk, reminder-app/build.sh
# 아래 sleep 간격은 promo.html의 장면 시각(in, touch)과 맞물려 있으니 바꾸면 html도 함께 고칠 것
P=$(cd "$(dirname "$0")" && pwd); W=$P/work; source $P/ui.sh
adb -e shell pm grant com.pintodo android.permission.POST_NOTIFICATIONS
adb -e shell appops set com.pintodo SCHEDULE_EXACT_ALARM allow
adb -e shell dumpsys deviceidle whitelist +com.pintodo >/dev/null
SHADE(){ adb -e shell cmd statusbar expand-notifications; sleep 1.5; }
CLOSE(){ adb -e shell cmd statusbar collapse; sleep 0.5; }
CLEAR(){ SHADE; tap 'text="모두 지우기"' >/dev/null; sleep 1.5; CLOSE; }
REMIND(){ adb -e shell am start -n com.demo.remind/.Post --es t "'$1'" --es x "'$2'" --ei id $3 >/dev/null; sleep 1; }
NOTI(){ CLOSE; seed noti; adb -e shell am start -n com.pintodo/.ui.MainActivity >/dev/null; sleep 2; adb -e shell input keyevent HOME; sleep 1; CLEAR; }
APP(){ CLOSE; seed; adb -e shell am start -n com.pintodo/.ui.MainActivity >/dev/null; sleep 2.5; CLOSE; }
NEW(){ adb -e shell "am start -n com.pintodo/.ui.MainActivity --es new_title '$1'" >/dev/null 2>&1; }

# r1 보통 알림은 밀면 사라짐
adb -e shell am force-stop com.pintodo; adb -e shell input keyevent HOME; CLEAR
REMIND '치과 예약 전화하기' '오후 2시 전에 꼭!' 11; SHADE
rec r1 6; sleep 1.3; adb -e shell input swipe 300 673 1060 673 400; sleep 4; pull r1

# r2 PinTodo 알림은 밀어도 다시 나타남
NOTI; SHADE
rec r2 8; sleep 1.0; adb -e shell input swipe 300 673 1060 673 400; sleep 5; pull r2

# r3 모두 지우기에도 남음
NOTI; REMIND '택배가 도착했어요' '문 앞에 두고 갑니다' 12; adb -e shell input keyevent HOME; sleep 0.5; SHADE
rec r3 8; sleep 1.0; adb -e shell input tap 540 1111; sleep 4; stoprec; pull r3

# r4 완료로 내림
NOTI; SHADE; c=$(center 'text="완료"')
rec r4 7; sleep 1.2; adb -e shell input tap $c; sleep 5; pull r4

# r5 날짜 자동 인식
APP; sleep 4
rec r5 7; sleep 0.8; NEW '내일 오후 3시 팀 미팅'; sleep 5; pull r5

# r6 반복(월수금 오후 7시) → 알림 방식 시트 → 1시간마다 다시 울리기 → 확인 → 추가
APP; sleep 1; NEW '헬스장 가기'; sleep 2.5
rec r6 30; sleep 0.8
adb -e shell input tap 540 948; sleep 1.2            # 반복 탭
adb -e shell input tap 274 1501; sleep 0.7           # 화 끄기
adb -e shell input tap 538 1501; sleep 1.0           # 목 끄기
adb -e shell input swipe 540 1800 540 900 500; sleep 1.2
adb -e shell input swipe 350 1281 350 1155 600; sleep 0.8   # 오전 → 오후
adb -e shell input swipe 540 1281 540 1533 700; sleep 1.8   # 9시 → 7시
adb -e shell input tap 310 1890; sleep 1.8           # 알림 방식
adb -e shell input tap 570 1860; sleep 1.5           # 1시간마다
adb -e shell input tap 540 2229; sleep 1.5           # 확인
adb -e shell input tap 540 2229; sleep 3             # 추가하기
stoprec; pull r6

# r7 체크리스트 펼쳐 체크 → 목록에서 완료
APP; sleep 3
rec r7 20; sleep 1.2
adb -e shell input tap 355 888; sleep 1.3
adb -e shell input tap 292 1156; sleep 0.8
adb -e shell input tap 292 1241; sleep 1.5
adb -e shell input tap 114 504; sleep 3
stoprec; pull r7

# r8 위젯에서 완료
APP; adb -e shell input keyevent HOME; sleep 2
rec r8 12; sleep 1.5; adb -e shell input tap 135 597; sleep 3.5; stoprec; pull r8
