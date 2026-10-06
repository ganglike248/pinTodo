# PinTodo (패키지명 com.pintodo)

완료할 때까지 알림창에 고정되는 안드로이드 투두 앱. **Google Play 출시 예정** (2026-10-02 결정, 아직 Play Console 등록 전). Kotlin + Jetpack Compose(Material 3), 외부 서버 없음.

- **현재 버전**: v0.4.4 (스토어 출시 전이라 0.x, 출시 시 1.0.0) | GitHub: https://github.com/ganglike248/pinTodo (main 브랜치)
- 사용 기기: 갤럭시 S26 울트라 (One UI / Android 16)

## 버전 관리 규칙 (필수)
버전을 올릴 때는 아래를 **동시에** 업데이트
- `version.txt`
- `app/build.gradle.kts`의 `versionName`(= version.txt와 동일), `versionCode`(+1, 절대 낮추면 안 됨 — 폰에 덮어쓰기 설치가 막힘)
- 이 파일의 "현재 버전"
- `ui/Changelog.kt`의 `RELEASES` 맨 위에 새 버전 추가 — **사용자가 체감하는 변화만**(새 기능/개선/변경(위치·동작이 바뀐 것)/수정), 내부 리팩터링은 제외. **버전당 3~5줄, 한 줄은 짧고 쉬운 말로**(기술 용어·세부 설명 X). 앱의 설정 > 버전에서 보임
- README의 버전 표

커밋 메시지 형식: `vX.Y.Z - type: 요약` (type: feat / fix / refactor / docs / release), 본문은 `-` 목록으로 변경 내용 정리.
커밋할 때는 항상 모든 파일 포함(`git add .`).

## Google Play 출시 준비 (절차는 `store/RELEASE.md`, 등록 문구·답변은 `store/listing.md`)
- [x] 업로드 키스토어: `~/.android/pintodo/`(저장소 밖, 커밋 금지). release = 업로드 키, sideload = 디버그 키 (v0.4.4)
- [x] `./gradlew bundleRelease`로 AAB (업로드 키 서명)
- [x] ~~`USE_EXACT_ALARM`~~ → `SCHEDULE_EXACT_ALARM` + 배너/설정에서 허용 화면 이동 + 미허용 시 `setAndAllowWhileIdle` 대체 + 허용 시 `SystemReceiver`가 Sync (v0.4.2)
- [x] ~~`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`~~ 제거 → 앱 정보 화면(배터리 > 제한 없음)으로 안내 (v0.4.2)
- [x] 개인정보처리방침 `docs/privacy-policy.md` → GitHub Pages(main /docs) 켜면 https://ganglike248.github.io/pinTodo/privacy-policy
- [x] 스토어 그래픽 `store/`(아이콘 512, 그래픽 1024×500, 스크린샷 1080×2160 — Play는 긴 변이 짧은 변의 2배 이하만 허용이라 화면을 자르지 말고 설명 문구와 함께 틀 안에 축소해 넣음)
- [ ] Play Console 등록 → 비공개 테스트(개인 계정은 12명·14일) → 프로덕션
- [ ] 정식 출시 시 버전 1.0.0, 이후 versionCode는 계속 증가

## 빌드 / 설치
```bash
./gradlew assembleSideload                     # 개인 폰용 → app/build/outputs/apk/sideload/app-sideload.apk
adb install -r app/build/outputs/apk/sideload/app-sideload.apk
./gradlew bundleRelease                        # Play 업로드용 AAB
```
- 폰에 설치된 앱은 디버그 키(`~/.android/debug.keystore`) 서명 → 폰 업데이트는 반드시 `sideload`로. `release`(업로드 키)는 서명이 달라 덮어쓰기 불가
- 다른 PC에서 빌드하면 디버그 키가 달라 덮어쓰기 불가

## 핵심 설계 (수정 시 주의)
- 앱 표시 이름은 **PinTodo**(`app_name`), 패키지명(applicationId·namespace)은 **`com.pintodo`**. Play에 한 번 올리면 패키지명은 영구 변경 불가
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
- 알림 버튼은 최대 3개: [완료][설정의 quickSnooze][미루기…(SnoozeActivity)]. 워치용 `WearableExtender`에는 [완료][quickSnooze]만
- **스마트워치**: 워치는 ongoing 알림을 넘겨받지 않음 → `AppSettings.wearable`(기본 켬)이면 `setOngoing(false)` + deleteIntent 재게시로 고정. 밀어서 지운 회차는 `Todo.wearDismissedKey`에 기록해 `setLocalOnly(true)`로 재게시(워치에서 사라지고 휴대폰에만 남음, 워치↔휴대폰 재게시 무한 반복 방지)
- 알림 본문은 `Format.notificationSchedule`(절대 날짜)로 항상 일정 표시 — 알림은 자정에 다시 그려지지 않으므로 '오늘/내일' 쓰지 말 것
- 편집 화면은 '자주 쓰는 것만 펼치기': 제목 + (메모·체크리스트는 `+` 칩으로 추가) + 라벨, 일정 탭 [한 번|반복|알림 없이](= notify 끄기), 알림 방식(고정·소리)은 한 줄 요약 → `AlertSheet`. 설정의 미루기 선택지도 시트로. 화면에 선택지를 더 늘릴 때는 시트/접기로
- 체크리스트 `Todo.items`(`CheckItem`), 색 라벨 `Todo.color`(`LabelColor`): 목록 카드(점·펼치는 체크리스트), 알림(펼치면 ☑/☐ 목록, 알림 색), 위젯(점·진행) 모두 반영
- 반복 종료 `repeatUntil`(epochDay)·`repeatCount`: `Todo.endDate`(lazy)로 마지막 날 계산 → `occursOn`에서 제외. 횟수는 `repeatAnchor`(규칙·횟수를 바꾸면 오늘)부터 셈
- 자동 백업: `res/xml/data_extraction_rules.xml`·`backup_rules.xml`에 `todos.xml`·`settings.xml`만 (ui.xml은 제외 → 새 폰에서 첫 실행 안내 다시 표시). 복원 후 알림은 앱을 처음 열 때 `Sync.run()`으로 다시 게시
- 일정 계산(`Todo`)·날짜 인식(`DateParser`)은 `app/src/test`의 JUnit 테스트로 검증: `./gradlew testDebugUnitTest`
- 시각 표시는 전부 `Format.time`(오전/오후 12시간제)을 거칠 것. 휠도 `TimeWheel`(오전·오후/시/분) 하나만 사용
- 반복은 `Todo.repeatType`(NONE/WEEKLY/MONTHLY/EVERY_DAYS) + `repeatInterval`(주·일 간격) + `monthDay`(31=말일) + `repeatAnchor`(격주·n일마다 기준일, epochDay). 회차 판정은 `occursOn()` 하나로. v0.4.3 이전 데이터는 `repeatDays`가 있으면 WEEKLY
- 편집 화면의 일정 휠은 시트가 아니라 화면에 펼쳐 둠(`InlineDateTime`/`InlineTime`). `WheelPicker`는 바깥 값으로 이동하는 동안(`syncing`) 지나가는 값을 알리지 않음 — 안 그러면 중간 값으로 되돌아감
- 제목·메모 자동 일정: 사용자가 일정을 직접 만지면(`manualSchedule`) 더 이상 덮어쓰지 않음. 기존 할 일은 처음 글에 있던 날짜로는 덮어쓰지 않음
- `QuickAddActivity`는 공유(SEND text/plain) 대상이라 exported=true
- 위젯은 Android 12+에서 크기별 `RemoteViews(Map<SizeF, RemoteViews>)`: 150×130dp 이상 목록형, 그보다 작으면 요약형(`widget_compact`)
- 빠른 추가/미루기 시트는 투명 액티비티 위의 ModalBottomSheet. 시트 안 입력창 포커스는 시트 창이 붙은 뒤(delay) 요청해야 키보드가 올라옴
- 휠 피커(`WheelPicker`)는 같은 값을 여러 번 반복한 LazyColumn + 가운데 스냅. 휴대폰 모드별 실제 울림(진동 모드에서 소리 채널 → 진동)은 에뮬레이터 `notification_alert` 로그로 검증함
