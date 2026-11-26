# 인스타그램 게시물 만들기

`store/insta/이름/1.png…`(1080×1350, 피드 4:5)와 `caption.txt`(게시물 설명)를 만드는 소스.

- 문구·배치: `post.html`의 `posts`(게시물 이름 → 장 배열). 틀은 모든 장이 같음 — 위에 작은 PinTodo·쪽 번호, 제목(핵심 낱말만 파랑), 설명 한 줄, 그림 하나. 글자 크기는 26 / 38 / 84 세 단계만
- 알림창 그림은 직접 그린 것(`noti`), 실제 앱 화면은 `shots/*.png` — 스토어 스크린샷(`store/screenshots/`)에서 폰 화면(700×1504)만 잘라 낸 것. 화면이 바뀌면 `shots-src`로 스토어 스크린샷을 다시 만든 뒤 `(190, 514, 890, 2018)`로 잘라 넣음

## 순서
1. 폰트(한 번만, 커밋 안 함): `curl -L -o store/insta-src/work/font/Pretendard.woff2 https://cdn.jsdelivr.net/npm/pretendard@1.3.9/dist/web/variable/woff2/PretendardVariable.woff2`
2. 그리기: `bash store/insta-src/render.sh 이름 장수` (예: `01-intro 8`). Chrome·Edge·Playwright Chromium 중 있는 것을 헤드리스로 씀
3. 결과를 꼭 눈으로 확인

## 게시물
| 이름 | 내용 |
|---|---|
| `01-intro` (8장) | 앱 소개: 관심 끌기 → 지워도 다시 뜸 → 알림에서 바로 → 날짜 인식 → 반복 → 위젯 → 이런 분께 → 받기 |
| `02-pin` (6장) | 고정 자세히: '모두 지우기'를 눌러도 안 지워짐 → 보통 알림과 비교 → 다시 뜰 땐 조용히 → 신규 표시 → 할 일마다 끄기 → 받기 |

마지막 장(받기)은 `END` 하나를 모든 게시물이 같이 씀.
