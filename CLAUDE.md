# PickTodo (저장소·패키지명은 pinTodo / com.ganglike.pintodo)

완료할 때까지 알림창에 고정되는 개인용 안드로이드 투두 앱. Kotlin + Jetpack Compose(Material 3), 외부 서버 없음.

- **현재 버전**: v0.4.0 (스토어 출시 전이라 0.x, 출시 시 1.0.0) | GitHub: https://github.com/ganglike248/pinTodo (main 브랜치)
- 사용 기기: 갤럭시 S26 울트라 (One UI / Android 16)

## 버전 관리 규칙 (필수)
버전을 올릴 때는 아래를 **동시에** 업데이트
- `version.txt`
- `app/build.gradle.kts`의 `versionName`(= version.txt와 동일), `versionCode`(+1, 절대 낮추면 안 됨 — 폰에 덮어쓰기 설치가 막힘)
- 이 파일의 "현재 버전"
- `ui/Changelog.kt`의 `RELEASES` 맨 위에 새 버전 추가 — **사용자가 체감하는 변화만**(새 기능/개선/변경(위치·동작이 바뀐 것)/수정), 내부 리팩터링은 제외. 앱의 설정 > 버전에서 보임
- README의 버전 표

커밋 메시지 형식: `vX.Y.Z - type: 요약` (type: feat / fix / refactor / docs / release), 본문은 `-` 목록으로 변경 내용 정리.
커밋할 때는 항상 모든 파일 포함(`git add .`).

## 빌드 / 설치
```bash
./gradlew assembleRelease                      # → app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```
- release도 디버그 키(`~/.android/debug.keystore`)로 서명 — 다른 PC에서 빌드하면 서명이 달라 기존 설치본 위에 덮어쓰기 불가(삭제 후 재설치 필요, 데이터 사라짐)

## 핵심 설계 (수정 시 주의)
- 앱 표시 이름은 **PickTodo**(`app_name`). 패키지명 `com.ganglike.pintodo`는 바꾸면 다른 앱으로 인식돼 기존 설치·데이터가 이어지지 않으므로 변경 금지
- **Android 14+는 ongoing 알림도 스와이프로 지워짐** → `deleteIntent`로 감지해 `ActionReceiver`에서 즉시 재게시. 그룹째 지우면 알림마다 호출되므로 전체 Sync가 아니라 해당 알림만 재게시
- **소리·진동 1회**: 처음 게시는 알림 방식별 HIGH 채널, 재게시는 `quiet` 채널. `Todo.alertedKey`(이미 울린 회차 키)와 `alertKey(now)`(회차 시작 또는 미루기 종료 시각)를 비교해 결정. 알림 채널 설정은 생성 후 변경 불가 → 바꾸려면 새 채널 ID 사용
- **모든 상태 반영은 `Sync.run()` 하나로**: 할 일 표시/숨김 + 다음 상태 변경 시각(`Todo.nextChange`)에 정확한 알람 예약
- 목록 카드의 스와이프 상태는 `rememberSwipeToDismissBoxState`(saveable) 대신 `remember`로 생성 — saveable이면 실행 취소로 카드가 돌아올 때 '밀린 상태'로 복원되어 완료가 재실행되는 버그 있었음
- 시스템 자동 그룹 요약 알림(tag 있음)은 `Notifier.activeIds`에서 제외
- v1 데이터(`{id, text}`)는 `TodoStore.fromJson`에서 호환 처리
- 디자인 토큰은 `ui/Theme.kt`(TDS 기반). 페이지 배경=`background`, 카드=`surface`, 칩·입력 채움=`surfaceVariant` 규칙 유지. Material You 사용 시 `asAppScheme()`으로 같은 규칙에 맞춤
- 위젯은 RemoteViews + `AppWidgetManager.updateAppWidget`로 즉시 갱신. Glance는 WorkManager를 거쳐 그려서 삼성 기기에서 갱신이 지연·누락됐음 → 다시 Glance로 바꾸지 말 것. 위젯 클래스명 `widget.TodoWidgetReceiver`는 이미 놓인 위젯 유지를 위해 변경 금지
- `Todo.notify=false`(알림 없는 할 일)는 상태 `NO_ALERT`, 알림·알람 없음. 저장 시 일정 필드는 비움
- 위젯·타일 갱신은 `Sync.run()` 끝에서 함께 처리 — 할 일을 바꾸는 경로는 반드시 `Sync.run()`을 거칠 것
- 알림 버튼은 최대 3개: [완료][설정의 quickSnooze][미루기…(SnoozeActivity)]
- 빠른 추가/미루기 시트는 투명 액티비티 위의 ModalBottomSheet. 시트 안 입력창 포커스는 시트 창이 붙은 뒤(delay) 요청해야 키보드가 올라옴
- 휠 피커(`WheelPicker`)는 같은 값을 여러 번 반복한 LazyColumn + 가운데 스냅. 휴대폰 모드별 실제 울림(진동 모드에서 소리 채널 → 진동)은 에뮬레이터 `notification_alert` 로그로 검증함
