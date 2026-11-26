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
| `09-everywhere` (6장) | 어디서든 보임: 깔아 놓고 안 여는 앱 → 상단바 → 위젯 → 스마트워치(직접 그림) → 잠금화면(직접 그림) → 받기 |
| `10-pills` (6장) | 약 챙기기 따라 하기: 세 단계 → 한 줄 적기 → 요일·시간 → 다시 울리기(2~4장은 에뮬레이터에서 찍은 `shots/edit-top.png`·`edit-time.png`·`alert.png`) → 먹었으면 완료 → 받기 |
| `11-student` (6장) | 학생 쓰임새: 마감 당일에 생각남 → '금요일까지'로 적기 → 남은 시간 → 시험 범위 체크리스트 → 수업 중엔 무음 → 받기. 화면은 `seed_student.py`로 예시를 넣고 찍음(`shots/student-new·student-list·alert-silent.png`), 새 할 일 화면은 `adb shell am start -n com.pintodo/.ui.MainActivity --es new_title "'금요일까지 과제 제출'"` |
| `12-work` (6장) | 직장인 쓰임새: 메일 보내야지 하고 퇴근 → 빠른 추가 → 시간에 맞춰 뜸 → 알림 없이 목록에만 → 내일 아침으로 미루기 → 받기. 예시는 `seed_work.py`, 빠른 추가 화면(`shots/quickadd.png`)은 `adb shell "am start -n com.pintodo/.ui.QuickAddActivity -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT '3시 회의 자료 출력'"`로 열어 시트 부분만 자름 |

첫 장 바탕색은 게시물마다 다르게(1 파랑, 2 어두운 남색, 3 노랑, 4 초록, 5 보라, 6 산호색, 7 흰색, 8 분홍, 9 청록, 10 주황, 11 하늘색, 12 연두) — 프로필 격자에서 구분되도록. 마지막 장(받기)은 `END` 하나를 모든 게시물이 같이 씀.

## 릴스 영상
`store/insta/reel-이름.mp4`(1080×1920, 30fps, 소리 없음 — 음악은 인스타에서 고름)와 설명 `reel-이름-caption.txt`.

| 이름 | 길이 | 구성 |
|---|---|---|
| `intro` | 30.5초 | 소개: 관심 끌기(알림을 밀어 지움) → 공감(말풍선 세 개, "기억력 탓이 아니에요") → 해결(밀어도 다시 뜸, 완료를 눌러야 사라짐) → 실제 화면 세 개(한 장면 3.4초씩) → 광고·로그인 없음 → 받기 |
| `day` | 27.8초 | 같은 하루, 다른 결과: 영양제 알림 — 보통 알림은 오전 9시에 밀고 밤 11시 40분에야 생각남 / PinTodo는 밀어도 다시 뜨고 오후 1시 20분에도 그대로 있어서 먹고 완료 → "의지가 아니라 알림이 문제였어요" → 받기 |

- 장면·문구·시각은 `reel.html`의 `reels`에 있음(영상마다 장면 시작 시각을 맨 위 상수로 둠). 새 영상은 `reels`에 항목을 추가
- 모든 움직임은 CSS 애니메이션이고, `render_reel.py`가 시각을 직접 넣어 한 프레임씩 찍은 뒤 묶음. 한 프레임을 6번 나눠 찍어 평균을 내서(`SUB`) 움직이는 부분에 잔상(모션 블러)이 생김
- 글자는 위 250px·아래 420px(릴스 화면의 계정·설명·버튼 자리)을 피해서 둠. 첫 프레임이 미리보기로 쓰이므로 0초에 제목이 이미 보이게 함
- 장면 전환은 새 장면이 아래에서 올라와 덮는 방식(겹쳐서 흐려지게 하면 글자끼리 겹쳐 보임)
- 준비(한 번): `python -m venv venv && venv/Scripts/python -m pip install playwright imageio-ffmpeg` (브라우저는 이미 있는 Chrome·Playwright Chromium을 씀)
- 렌더링: `python store/insta-src/render_reel.py intro` (영상 하나에 7~9분). 확인만 할 때는 `python store/insta-src/render_reel.py intro 2.1,9.5,20` 처럼 시각(초)을 주면 그 프레임만 `work/reel/`에 PNG로
