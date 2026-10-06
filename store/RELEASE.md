# Google Play 출시 절차

## 0. 업로드 키 (이미 만들어 둠)
- 위치: `~/.android/pintodo/upload-keystore.jks` (별칭 `upload`, 유효기간 10000일)
- 비밀번호: `~/.android/pintodo/keystore.properties`
- **두 파일을 비밀번호 관리자나 안전한 클라우드에 꼭 따로 백업**할 것. 저장소에는 절대 커밋하지 않음(.gitignore 처리됨)
- 업로드 인증서 SHA-256: `AB:01:5C:FF:14:67:50:7A:39:2C:3D:08:CD:76:A7:4C:43:AC:F8:A4:CC:11:92:CE:B8:3D:AB:B1:C0:CE:57:EA`
- 잃어버려도 Play App Signing을 쓰면 Play Console에서 업로드 키 재설정을 요청할 수 있음 (며칠 걸림)
- 다른 PC에서 빌드하려면 두 파일을 같은 경로에 두거나 `PINTODO_KEYSTORE_PROPERTIES` 환경 변수로 properties 경로 지정

## 1. 빌드
```bash
./gradlew bundleRelease      # → app/build/outputs/bundle/release/app-release.aab (업로드 키 서명, Play에 올리는 파일)
./gradlew assembleSideload   # → app/build/outputs/apk/sideload/app-sideload.apk (개인 폰 직접 설치용, 디버그 키)
```

## 2. Play Console
1. 앱 만들기 → 이름·언어·무료 선택 (`listing.md` 1번)
2. **앱 무결성 → Play 앱 서명**: 기본값(Google에서 생성한 키) 그대로 사용
3. **앱 콘텐츠**: `listing.md` 5번 답변대로 개인정보처리방침·광고·콘텐츠 등급·타겟층·데이터 보안 작성
4. **기본 스토어 등록정보**: 설명·그래픽·스크린샷 업로드 (`listing.md` 2~4번)
5. **테스트 → 비공개 테스트**: AAB 업로드 → 테스터(이메일 목록) 추가
   - 2023년 11월 이후 만든 **개인 개발자 계정은 프로덕션 출시 전에 비공개 테스트 12명 이상, 14일 이상**이 필요함
6. 조건을 채우면 **프로덕션 액세스 신청** → 심사 → 출시

## 3. 버전
- 프로덕션 첫 출시는 `versionName 1.0.0` (CLAUDE.md 규칙), `versionCode`는 계속 증가
- Play에서 받은 앱은 Play 서명이라, 지금 폰에 직접 설치한 앱(디버그 키) 위에 덮어쓸 수 없음
  → 옮길 때: 설정 > 백업 > 백업 파일 만들기 → 앱 삭제 → Play에서 설치 → 파일에서 복원
