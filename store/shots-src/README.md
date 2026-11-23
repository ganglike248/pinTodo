# 스토어 스크린샷·그래픽 이미지 만들기

`store/screenshots/01~08.png`(1080×2160)와 `store/feature-1024x500.png`를 만드는 소스. 원본 화면·폰트는 `work/`(커밋 안 함)에 생긴다.

구성: 1 공감(직접 그린 알림 카드: 보통 알림은 밀면 사라짐 ↔ PinTodo는 남음) → 2 앱 목록 → 3 빠른 추가(날짜 인식) → 4 알림에서 바로 완료(체크리스트 확대) → 5 미루기 → 6 반복 → 7 위젯 → 8 광고·로그인 없음

그래픽 이미지는 `compose.html`의 `#feature2`(예전 구성: 아이콘·이름·문구·주요 기능 + 목록 화면)를 2배로 그린 뒤 줄여서 저장. `#feature`는 쓰지 않는 시안

## 순서
1. 에뮬레이터 준비 (한 번만): `Medium_Phone`(1080×2400), 시스템 언어 한국어, 키보드 앱 사용 중지, 홈 화면 맨 위에 PinTodo 위젯을 가로 꽉·4줄 높이로
2. 앱 설치: `./gradlew assembleDebug && adb -e install -r app/build/outputs/apk/debug/app-debug.apk` (예시 데이터를 `run-as`로 넣으므로 디버그 빌드), `zsh store/promo-src/reminder-app/build.sh` (1번의 '보통 알림' 역할)
3. 촬영: `zsh store/shots-src/capture.sh` → `work/raw/*.png` (합성할 때 상태 표시줄 80px는 잘라냄). 결과를 꼭 눈으로 확인 — 앱이 늦게 뜨면 시작 화면이 찍힘
4. 폰트: `curl -L -o store/shots-src/work/font/Pretendard.woff2 https://cdn.jsdelivr.net/npm/pretendard@1.3.9/dist/web/variable/woff2/PretendardVariable.woff2`
5. 합성(Playwright Chromium): `python3 store/shots-src/render.py` → `store/` 에 저장. 확인만 할 때는 출력 폴더를 인자로

예시 할 일은 `seed.py`, 문구·배치는 `compose.html`의 `slides` 배열에서 고친다.
