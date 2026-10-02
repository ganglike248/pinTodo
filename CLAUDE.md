# pinTodo (고정 투두)

완료할 때까지 알림창에 고정되는 개인용 안드로이드 투두 앱. Kotlin + Jetpack Compose(Material 3), 외부 서버 없음.

- **현재 버전**: v0.1.0 (스토어 출시 전이라 0.x, 출시 시 1.0.0) | GitHub: https://github.com/ganglike248/pinTodo (main 브랜치)
- 사용 기기: 갤럭시 S26 울트라 (One UI / Android 16)

## 버전 관리 규칙 (필수)
버전을 올릴 때는 아래를 **동시에** 업데이트
- `version.txt`
- `app/build.gradle.kts`의 `versionName`(= version.txt와 동일), `versionCode`(+1, 절대 낮추면 안 됨 — 폰에 덮어쓰기 설치가 막힘)
- 이 파일의 "현재 버전"

커밋 메시지 형식: `vX.Y.Z - type: 요약` (type: feat / fix / refactor / docs / release), 본문은 `-` 목록으로 변경 내용 정리.
커밋할 때는 항상 모든 파일 포함(`git add .`).

## 빌드 / 설치
```bash
./gradlew assembleRelease                      # → app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```
- release도 디버그 키(`~/.android/debug.keystore`)로 서명 — 다른 PC에서 빌드하면 서명이 달라 기존 설치본 위에 덮어쓰기 불가(삭제 후 재설치 필요, 데이터 사라짐)

## 핵심 설계 (수정 시 주의)
- **Android 14+는 ongoing 알림도 스와이프로 지워짐** → `deleteIntent`로 감지해 `ActionReceiver`에서 즉시 재게시. 그룹째 지우면 알림마다 호출되므로 전체 Sync가 아니라 해당 알림만 재게시
- **소리·진동 1회**: 처음 게시는 알림 방식별 HIGH 채널, 재게시는 `quiet` 채널. `Todo.alertedKey`(이미 울린 회차 키)와 `alertKey(now)`(회차 시작 또는 미루기 종료 시각)를 비교해 결정. 알림 채널 설정은 생성 후 변경 불가 → 바꾸려면 새 채널 ID 사용
- **모든 상태 반영은 `Sync.run()` 하나로**: 할 일 표시/숨김 + 다음 상태 변경 시각(`Todo.nextChange`)에 정확한 알람 예약
- 목록 카드의 스와이프 상태는 `rememberSwipeToDismissBoxState`(saveable) 대신 `remember`로 생성 — saveable이면 실행 취소로 카드가 돌아올 때 '밀린 상태'로 복원되어 완료가 재실행되는 버그 있었음
- 시스템 자동 그룹 요약 알림(tag 있음)은 `Notifier.activeIds`에서 제외
- v1 데이터(`{id, text}`)는 `TodoStore.fromJson`에서 호환 처리
