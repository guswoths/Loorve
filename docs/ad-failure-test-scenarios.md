# 광고 로딩 실패 시 핵심 기능 동작 테스트 시나리오

## 테스트 환경 준비
- 기기: Android API 26+ 실기기 또는 에뮬레이터
- 빌드 타입: debug (에뮬레이터 자동 등록)
- 실기기 테스트 시: Logcat에서 "Use RequestConfiguration.Builder().setTestDeviceIds()"
  로그의 해시 ID를 LoorveApplication.kt의 testDeviceIds에 추가

---

## 시나리오 1: 비행기 모드 ON + 앱 실행

**준비**: 기기를 비행기 모드로 전환 후 앱 실행

| 검증 항목 | 기대 동작 | 확인 방법 |
|---|---|---|
| 배너 광고 노출 | 미노출 (View.GONE) | HomeScreen 배너 영역 확인 |
| 진도 입력 | 정상 동작 | ProgressInputSection 저장 버튼 활성화 및 Snackbar 표시 |
| 복습 체크 | 정상 동작 | ReviewCalendarScreen 체크박스/완료 버튼 동작 |
| AlarmManager 알림 | 정상 스케줄링 | Logcat: "AlarmBroadcastReceiver" 태그 확인 |
| LazyColumn padding | HomeScreen: bottom=16dp, ReviewCalendar: bottom=16dp | 목록 최하단 여백 육안 확인 |

**Logcat 확인 키워드**:
- `BannerAdView: Ad failed to load`
- `BannerAdView: AdView destroyed on dispose`
- `LoorveApp: AdMob initialized` 또는 `AdMob initialization failed (non-fatal)`

---

## 시나리오 2: AdMob 테스트 기기 미등록 (실기기)

**준비**: LoorveApplication.kt의 testDeviceIds에서 실기기 해시 ID 미등록 상태

| 검증 항목 | 기대 동작 |
|---|---|
| 배너 광고 | 테스트 광고 노출 또는 없음 (실 광고 미노출) |
| 핵심 기능 | 모두 정상 동작 |
| Logcat 경고 | "Test mode device ID" 관련 경고 없음 |

---

## 시나리오 3: 광고 타임아웃 (onAdFailed 콜백 발동)

**준비**: 에뮬레이터에서 네트워크를 로드 중 차단하거나, 잘못된 adUnitId로 강제 실패 유발

| 검증 항목 | 기대 동작 |
|---|---|
| BannerAdView | View.GONE → Compose tree에서 제거 |
| HomeScreen LazyColumn bottom padding | 66dp → 16dp 전환 |
| ReviewCalendarScreen contentPadding | 66dp → 16dp 전환 |
| 진도 입력/복습 체크 | 차단 없이 정상 동작 |
| AdView.destroy() | onDispose에서 호출됨 (Logcat: "AdView destroyed on dispose") |

---

## 시나리오 4: 정상 네트워크 + 광고 로드 성공

| 검증 항목 | 기대 동작 |
|---|---|
| BannerAdView | View.VISIBLE, 배너 노출 |
| HomeScreen LazyColumn bottom padding | 66dp 유지 |
| ReviewCalendarScreen contentPadding | 66dp 유지 |
| 진도 입력/복습 체크 | 정상 동작 |
| Logcat | `BannerAdView: Ad loaded successfully` |

---

## 광고 독립성 체크리스트

- [ ] `ProgressInputSection`: `isSaveEnabled`가 `adFailed` 상태와 무관하게 동작
- [ ] `ReviewScheduleItem`: Checkbox/완료 버튼이 별도 Card 레이어로 광고 컨테이너와 독립
- [ ] `AlarmBroadcastReceiver`: 네트워크 없이도 `showNotification()` 실행
- [ ] `BootCompletedReceiver`: `MobileAds.initialize()` 호출 없이 알람 재등록
- [ ] `LoorveApplication.initAdMob()`: try-catch로 앱 크래시 없이 광고 초기화 실패 처리