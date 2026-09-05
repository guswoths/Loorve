\# Loorve 작업 규칙



\- 이 프로젝트는 Android Kotlin + Jetpack Compose 앱이다.

\- 코드 변경 전 현재 구조, 관련 파일, 영향 범위와 수정 계획을 한국어로 먼저 설명한다.

\- 사용자의 승인 전에는 파일을 수정, 삭제, 이동하거나 Git 명령으로 커밋·푸시하지 않는다.

\- `google-services.json`, Firebase 서비스 계정 JSON, `.env`, `.jks`, `.keystore`, API 키, 토큰, 비밀번호를 읽어 출력하거나 수정하거나 Git에 추가하지 않는다.

\- Firestore 접근 코드는 사용자 UID 기준 데이터 분리와 Firestore 보안 규칙을 우선 검토한다.

\- 변경 후 관련 단위 테스트를 만들거나 실행하고, `gradlew.bat test`, `gradlew.bat lint`, `gradlew.bat assembleDebug` 실행 결과를 보고한다.

\- Android 앱의 날짜 및 알림은 사용자의 로컬 시간대와 시험일 경계를 고려한다.

\- 파일 변경은 작은 단위로 하고, 변경 전후 diff와 되돌리는 방법을 제공한다.



