# 스토어 스크린샷·그래픽 이미지 만들기

`store/screenshots/01~08.png`(1080×2160)와 `store/feature-1024x500.png`를 만드는 소스. 원본 화면·폰트는 `work/`(커밋 안 함)에 생긴다.

구성: 1 상단바에 고정된 할 일 → 2 보통 알림(밀면 사라짐) vs PinTodo(다시 나타남) → 3 상단바에서 바로 완료(펼친 체크리스트) → 4 새 할 일(날짜 인식) → 5 반복 → 6 미루기 → 7 위젯 → 8 광고·로그인 없음

- 1~3번과 그래픽 이미지의 알림창은 **캡처가 아니라 `compose.html`에서 직접 그린 밝은 갤럭시(One UI) 느낌 화면**(`gshade`/`gcard`). 에뮬레이터는 구글 기본 UI라 갤럭시 사용자에게 낯설고 눈에 덜 띄어서. 빠른 설정은 일부러 연하게(아래 알림에 눈이 가도록), 삼성 로고 등 상표는 넣지 않음
- 4~7번은 에뮬레이터 실제 화면. 상태 표시줄(위 80px)은 잘라냄
- 모든 장 위에 PinTodo 배지(8번은 가운데 큰 로고가 있어 뺌)

## 순서
1. 에뮬레이터 준비 (한 번만): `Medium_Phone`(1080×2400), 시스템 언어 한국어, 키보드 앱 사용 중지, 홈 화면 맨 위에 PinTodo 위젯을 가로 꽉·4줄 높이로
2. 앱 설치: `./gradlew assembleDebug && adb -e install -r app/build/outputs/apk/debug/app-debug.apk` (예시 데이터를 `run-as`로 넣으므로 디버그 빌드)
3. 촬영: `zsh store/shots-src/capture.sh` → `work/raw/*.png`. 결과를 꼭 눈으로 확인 — 앱이 늦게 뜨면 시작 화면이 찍힘
4. 폰트: `curl -L -o store/shots-src/work/font/Pretendard.woff2 https://cdn.jsdelivr.net/npm/pretendard@1.3.9/dist/web/variable/woff2/PretendardVariable.woff2`
5. 합성(Playwright Chromium): `python3 store/shots-src/render.py` → `store/`에 저장(그래픽 이미지는 2배로 그린 뒤 줄여서 선명하게). 확인만 할 때는 출력 폴더를 인자로

예시 할 일은 `seed.py`, 문구·배치는 `compose.html`의 `slides` 배열, 그래픽 이미지는 `#feature2`(`#feature`는 쓰지 않는 시안)에서 고친다.
