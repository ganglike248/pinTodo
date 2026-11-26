# 인스타그램 게시물 만들기

`store/insta/이름/1.png…`(1080×1350, 피드 4:5)와 `caption.txt`(게시물 설명)를 만드는 소스.

- 문구·배치: `post.html`의 `posts`(게시물 이름 → 장 배열). 틀은 모든 장이 같음 — 위에 작은 PinTodo·쪽 번호, 제목(핵심 낱말만 파랑), 설명 한 줄, 그림 하나. 글자 크기는 26 / 38 / 84 세 단계만
- 알림창 그림은 직접 그린 것(`noti`), 실제 앱 화면은 `shots/*.png` — 스토어 스크린샷(`store/screenshots/`)에서 폰 화면(700×1504)만 잘라 낸 것. 화면이 바뀌면 `shots-src`로 스토어 스크린샷을 다시 만든 뒤 `(190, 514, 890, 2018)`로 잘라 넣음
- `shots/checklist.png`(장보기 편집 화면)·`list.png`(할 일 목록)는 에뮬레이터에서 직접 찍은 것: `Pixel_9`에 디버그 빌드 설치 → `shots-src/seed.py`로 예시 할 일을 넣고 → 목록에서 장보기를 눌러 `adb exec-out screencap` → 위 130px(상태 표시줄)을 자르고 너비 700으로 줄임. 키보드가 뜨면 `adb shell settings put secure show_ime_with_hard_keyboard 0`. 화면이 검게 찍히면 `adb shell input keyevent 224`(화면 켜기), 날짜가 어긋나면 `adb shell cmd alarm set-timezone Asia/Seoul`

## 순서
1. 폰트(한 번만, 커밋 안 함): `curl -L -o store/insta-src/work/font/Pretendard.woff2 https://cdn.jsdelivr.net/npm/pretendard@1.3.9/dist/web/variable/woff2/PretendardVariable.woff2`
2. 그리기: `bash store/insta-src/render.sh 이름 장수` (예: `01-intro 8`). Chrome·Edge·Playwright Chromium 중 있는 것을 헤드리스로 씀
3. 결과를 꼭 눈으로 확인

## 게시물
| 이름 | 내용 |
|---|---|
| `01-intro` (8장) | 앱 소개: 관심 끌기 → 지워도 다시 뜸 → 알림에서 바로 → 날짜 인식 → 반복 → 위젯 → 이런 분께 → 받기 |
| `02-pin` (6장) | 고정 자세히: '모두 지우기'를 눌러도 안 지워짐 → 보통 알림과 비교 → 다시 뜰 땐 조용히 → 신규 표시 → 할 일마다 끄기 → 받기 |
| `03-snooze` (6장) | 미루기: '나중에'가 '안 함'이 되지 않게 → 알림에서 10분 뒤 → 미루기 시트 → 선택지 고르기 → 위젯 → 받기 |
| `04-write` (5장) | 적기: 날짜 고르기 귀찮음 → 실제 새 할 일 화면 → 알아듣는 말(예시는 `DateParserTest`에 있는 것만) → 어디서든 추가 → 받기 |
| `05-repeat` (7장) | 반복: 오늘 먹었나? → 실제 반복 화면 → 주기 네 가지 → 반복 종료 → 완료하면 다음 날 다시 → 다시 울리기 → 받기 |
| `06-checklist` (7장) | 체크리스트·색 라벨: 장 보러 가서 빼먹음 → 앱에서 체크리스트 작성 → 알림을 펼치면 목록 → 앱 목록의 진행 막대 → 색 라벨 → 위젯 → 받기 |
| `07-free` (6장) | 광고·로그인·결제 없음: 없는 게 많아요 → 없는 것 네 가지 → 내 휴대폰에만 저장 → 백업(실제 설정 화면 `shots/backup.png`) → 왜 무료인지 → 받기 |
| `08-history` (5장) | 기록·되돌리기: 실수로 완료 → 이번 주 통계 → 날짜별 목록·검색 → 되돌리기·지우기 → 받기. 기록 화면(`shots/history.png`)은 `seed_history.py`로 완료한 할 일을 넣고 찍음 |

첫 장 바탕색은 게시물마다 다르게(1 파랑, 2 어두운 남색, 3 노랑, 4 초록, 5 보라, 6 산호색, 7 흰색, 8 분홍) — 프로필 격자에서 구분되도록. 마지막 장(받기)은 `END` 하나를 모든 게시물이 같이 씀.
