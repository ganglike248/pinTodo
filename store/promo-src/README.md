# 홍보 영상 만들기

`store/promo-video.mp4`(1920×1080)와 `store/promo-video-vertical.mp4`(1080×1920)를 만드는 소스. 원본 녹화·프레임·폰트는 `work/`(커밋 안 함)에 생긴다.

구성(45초): 문제 제기 → 보통 알림은 밀면 사라짐 → PinTodo는 밀어도 다시 나타남 → '모두 지우기'에도 남음 → [완료]로만 내림 → 날짜 자동 인식 → 반복(월·수·금 오후 7시) → 다시 울리기 → 체크리스트·색 라벨 → 위젯 → 마무리

## 순서
1. 에뮬레이터 준비 (한 번만): `Medium_Phone`(1080×2400), 시스템 언어 한국어, 키보드 앱 사용 중지, 홈 화면 맨 위에 PinTodo 위젯을 가로 꽉·4줄 높이로
2. 앱 설치: `./gradlew assembleDebug && adb -e install -r app/build/outputs/apk/debug/app-debug.apk` (예시 데이터를 `run-as`로 넣으므로 디버그 빌드), `zsh store/promo-src/reminder-app/build.sh` ('보통 알림' 역할의 작은 앱)
3. 녹화: `zsh store/promo-src/record.sh` → `work/rec/r1~r8.mp4`
4. 프레임 추출: `python3 store/promo-src/prep.py` — 화면이 바뀌는 시각을 출력하니, 지난번과 다르면 `promo.html`의 `S`(장면 `in`, `touch` 시각)를 맞춘다
5. 폰트: `curl -L -o store/promo-src/work/font/Pretendard.woff2 https://cdn.jsdelivr.net/npm/pretendard@1.3.9/dist/web/variable/woff2/PretendardVariable.woff2`
6. 렌더링(Playwright Chromium): `python3 store/promo-src/render.py h store/promo-video.mp4`, `python3 store/promo-src/render.py v store/promo-video-vertical.mp4` (각 4~5분). 확인만 할 때는 세 번째 인자로 시각 목록(`1.5,10,20`)을 주면 그 프레임만 PNG로

장면 문구·확대 위치·터치 표시는 모두 `promo.html`의 `S` 배열과 `CAM`에서 고친다.
