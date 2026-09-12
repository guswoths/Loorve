# 리뷰 일정 정책 명세서

본 문서는 사용자가 하루 1건의 자기 작성 텍스트 학습기록을 등록하는 앱에서, 각 학습기록을 독립적인 복습 단위로 취급하고 시험일을 기준으로 자동 생성되는 복습 일정을 정의하는 제품 정책 명세서이다. 본 문서는 구현 설계서가 아니라 제품 정책의 판단 기준과 결정 규칙을 기술한다.

## 일반 학습 원칙

- 에빙하우스 망각곡선은 개인별 학습 속도와 학습 내용의 차이를 절대적으로 반영하는 예측 공식으로 취급하지 않는다. 본 정책은 개인차와 내용차를 인정한다.
- 제품 정책의 원칙은 다음과 같다.
  - 초기에는 짧은 간격의 복습을 우선한다.
  - 이후에는 점점 긴 간격으로 확장한다.
  - 남은 시험일이 짧을수록 복습 간격을 더 짧게 조정한다.
- 학습 기록은 사용자 입력 텍스트 1건이며, 각 기록은 독립된 복습 단위이다.
- 제안 행동은 적극적 회상, 짧은 퀴즈, 플래시카드, 연습 문제 풀이, 오답 설명이 우선이며, 단순한 반복 독서는 보조적이다.
- 본 정책은 개인의 정확한 기억률을 과학적으로 예측하는 공식으로 사용하지 않는다.

## 앱의 제품 정책

- 모든 날짜 계산은 LocalDate 기준으로 수행한다. 값 표현은 YYYY-MM-DD 형식이며, 시간대, DST, UTC 변환, 장치 시계 시간, 24시간 지속시간 기반 산술을 사용하지 않는다.
- 기본 표기 및 비즈니스 시간대는 Asia/Seoul 이다.
- 캘린더 일은 1 LocalDate를 의미하고, 24시간 duration으로 환산하지 않는다.
- 자동 생성되는 일반 복습 일정은 `examDate`와 버퍼 구간을 제외하고, 자동 생성되는 일상 복습 과제는 `examDate`에 배정되지 않는다.
- 본 정책에서 정의하는 규칙은 결정적(deterministic)이며, 구현은 이 규칙을 정적으로 준수해야 한다.

## 1. 정책의 목적과 비적용 범위

### 1.1 목적

- 사용자가 일일 학습기록을 작성할 때, 각 학습기록을 독립적인 복습 단위로 관리한다.
- 시험일을 기준으로 일정의 시작과 종료 범위를 명확히 정의한다.
- 복습 횟수, 간격, 우선순위, 상태 전이, 사용자 메시지를 일관되게 결정한다.
- 시간대 문제 없이 LocalDate 기준으로 동작하는 공정한 자동 스케줄링 정책을 보장한다.

### 1.2 비적용 범위

- 본 정책은 “하루 1개 자기 작성 텍스트 기록”이라는 단일 학습 기록 모델에 적용한다.
- 본 정책은 사용자 입력 텍스트를 기준으로 한 복습 단위이며, 이미 생성된 수업 자료, 문서 전체, 과목 전체 단위의 일정 정책을 대체하지 않는다.
- 본 정책은 시험일 변경, 학습기록 생성, 복습 결과 반영, 사용자 상태 표시를 다룬다. 그러나 과목별 글로벌 작업량 상한, 연동된 LMS 또는 외부 알림 서버, 금전적 결제 정책, 계정 권한 정책은 별도 정책으로 처리한다.
- 본 정책은 의학적, 심리학적, 과학적 개인화 예측을 설정하지 않는다.
- 본 정책은 일반 학습 원칙과 제품 정책을 구분하여 운영한다. “일반 학습 원칙”은 비결정적 권고사항이며, “앱의 제품 정책”은 구현이 따라야 하는 결정적 규칙이다.

## 2. 날짜 계산 정의

### 2.1 LocalDate 기반 날짜 원칙

- 모든 날짜 값은 `YYYY-MM-DD` 형식의 LocalDate이다.
- 날짜 계산 시 시간대 변환, DST 보정, UTC 변환, 장치 로컬 시간, 타임스탬프 환산을 사용하지 않는다.
- “캘린더 day”는 1 LocalDate를 의미하고, 24시간 기간으로 환산하지 않는다.
- 기본 비즈니스 시간대는 `Asia/Seoul`이며, 화면 표시와 사업 규칙은 이 시간대를 기준으로 한다.

### 2.2 daysBetween(startDate, endDate)

`daysBetween(startDate, endDate)`는 `startDate`부터 `endDate`까지의 경계 기준 일수 차이를 의미한다. 값은 정수이며, `startDate`와 `endDate`는 모두 LocalDate이다.

정의:

- `daysBetween(startDate, endDate) = (endDate - startDate) in calendar days`
- 예시:
  - `daysBetween(2026-10-01, 2026-10-01) = 0`
  - `daysBetween(2026-10-01, 2026-10-02) = 1`
  - `daysBetween(2026-10-02, 2026-10-01) = -1`
- 이 함수는 signed count이며, 덧셈/뺄셈 순서가 다르면 부호가 달라진다.
- 구현은 `LocalDate`의 날짜 차이 연산만 사용하고, 시각 또는 타임존 변환을 사용하지 않는다.

### 2.3 날짜 계산 예시

- `todayDate = 2026-10-01`
- `examDate = 2026-10-06`
- `daysBetween(2026-10-01, 2026-10-06) = 5`
- `daysBetween(2026-10-06, 2026-10-01) = -5`

### 2.4 유효하지 않은 날짜 입력

- 날짜 문자열이 `YYYY-MM-DD` 형식이 아니거나, 실제 존재하지 않는 날짜면 유효하지 않은 값으로 처리한다.
- 구현은 이 값을 reject하고 사용자에게 명시적인 오류를 표시해야 한다.
- `todayDate`는 앱의 현재 로컬 날짜를 기준으로 계산하되, 별도의 타임스탬프 기반 시간 계산을 허용하지 않는다.

## 3. 시험일 설정 가능/차단 규칙

### 3.1 시험일 기본 정의

- `examDate`는 시험일이다.
- `finalReviewBufferDays`는 최종 복습 버퍼 일수이며 기본값은 `1`이다.
- 시험일과 버퍼일은 자동 일반 복습 스케줄에서 제외한다.

### 3.2 버퍼와 마지막 일반 복습 가능일

- 자동 일반 복습은 `examDate`와 `examDate` 직전의 buffer date를 제외한다.
- 일반적으로 `finalReviewBufferDays = 1`이면, `examDate` 이전 1일인 `examDate - 1`은 buffer date이며, 자동 일반 리뷰는 배정되지 않는다.
- 마지막 일반 복습 가능일은 다음과 같이 정의한다.

`lastReviewDate = examDate - (finalReviewBufferDays + 1 calendar day)`

- 예시:
  - `examDate = 2026-11-15`
  - `finalReviewBufferDays = 1`
  - `lastReviewDate = 2026-11-13`

### 3.3 시험일 및 버퍼일의 제외 규칙

- `examDate`는 자동 생성 일반 복습의 대상이 아니다.
- `finalReviewBufferDays`가 1일 경우, `examDate - 1`도 자동 생성 일반 복습의 대상이 아니다.
- 일반적으로 buffer 구간은 아래와 같이 정의한다.

`bufferDates = { examDate - finalReviewBufferDays, ..., examDate - 1, examDate }`

- 위 구간은 자동 일반 복습 배정 대상에서 제외된다.
- 사용자가 보게 되는 최종 준비 안내, 재확인 알림, 시험 전 안내 문구는 별도 사용자 메시지로 표시할 수 있으나, 이 날짜들에는 자동 생성된 “일반 복습 과제”가 없다.
- 권장 기본값: 시험일 및 버퍼일에는 자동 생성 일반 복습 작업을 생성하지 않고, “최종 준비” 안내만 비스케줄링 형태로 보여준다.

### 3.4 최소 시험일 자격 조건

- 시험일 저장 시, 오늘부터 시험일 전까지 최소 3개의 정상 학습/복습 가능 캘린더 날짜가 있어야 한다.
- 계산식:

`availableDaysForNewLearning = daysBetween(todayDate, examDate) - finalReviewBufferDays - 1`

- `availableDaysForNewLearning < 3`이면 시험일 저장을 차단한다.
- 예시:
  - `todayDate = 2026-10-01`
  - `finalReviewBufferDays = 1`
  - `daysBetween(2026-10-01, 2026-10-06) = 5`
  - `availableDaysForNewLearning = 5 - 1 - 1 = 3`
  - 2026-10-02, 2026-10-03, 2026-10-04는 최소 3개의 사용 가능한 학습/복습일이다.
  - 2026-10-05는 시험 전 버퍼일이다.
  - 2026-10-06은 가장 빠른 허용 시험일이다.

### 3.5 가장 빠른 허용 시험일

- 가장 빠른 허용 시험일은 다음 식으로 결정한다.

`daysBetween(todayDate, examDate) >= 3 + finalReviewBufferDays + 1`

- 같은 의미로:

`earliestAllowedExamDate = todayDate + (3 + finalReviewBufferDays + 1 calendar day)`

- 예시:
  - `todayDate = 2026-10-01`
  - `finalReviewBufferDays = 1`
  - `earliestAllowedExamDate = 2026-10-06`

### 3.6 검증 규칙: invalid or missing setting

- `finalReviewBufferDays`는 정수여야 한다.
- 허용 범위는 `0` 이상 정수만 허용한다.
- 값이 음수이거나 정수가 아니거나 `null`/undefined/NaN/무효 문자열이면 reject한다.
- 설정이 없으면 기본값 `1`을 사용한다.
- 권장 기본값: 유효하지 않은 값은 입력을 거절하고, 설정이 누락된 경우 `1`을 적용한다.
- 변경된 시험일이 최소 자격 조건을 만족하지 않으면 저장을 차단하고, 기존의 저장된 상태는 유지한다.

## 4. 학습기록별 복습 횟수 결정 표

### 4.1 effectiveStudyDays

- 각 학습기록에는 `studyDate`와 `lastReviewDate`가 있다.
- 학습기록의 유효 기준 학습 일수는 아래와 같이 정의한다.

`effectiveStudyDays = max(0, daysBetween(studyDate, lastReviewDate))`

- `studyDate`는 학습기록의 최초 작성일이다.
- `lastReviewDate`는 시험일 직전 마지막 일반 복습 가능일이다.
- `studyDate`가 `lastReviewDate` 이후로 생성된 경우, `effectiveStudyDays = 0`이며 해당 기록은 부족한 일정 창 상태로 처리한다.

### 4.2 기본 복습 횟수 표

아래 표는 학습기록별 최소 자동 일반 복습 수량과 상태를 정의한다. “최소 일반 복습 수”는 자동으로 생성되는 일반 복습만 의미한다. 학습 기록 자체는 복습으로 계산하지 않는다.

| effectiveStudyDays 범위 | 최소 일반 복습 수 | 상태 또는 안내 | 비고 |
|---|---:|---|---|
| 0일 이하 또는 `studyDate > lastReviewDate` | 0 | `INSUFFICIENT_WINDOW` 또는 `CRAM_MODE_REQUIRED` | 적어도 3개 유효일이 필요 |
| 3 ~ 6일 | 2 | 정상 스케줄 허용 | 최소 2회 |
| 7 ~ 13일 | 3 | 정상 스케줄 허용 | 최소 3회 |
| 14 ~ 29일 | 4 | 정상 스케줄 허용 | 최소 4회 |
| 30 ~ 59일 | 5 | 정상 스케줄 허용 | 최소 5회 |
| 60 ~ 119일 | 6 | 정상 스케줄 허용 | 최소 6회 |
| 120일 이상 | 7 | 정상 스케줄 허용 | 최소 7회 |

### 4.3 추가 규칙

- `studyDate` 자신은 복습 대상이 아니다.
- 복습은 `studyDate + 1`일 이후에서 `lastReviewDate`까지의 유효 날짜에만 생성할 수 있다.
- 같은 학습기록에 대해 같은 날짜에 복습을 두 번 이상 생성하지 않는다.
- “수량 충족을 위해 같은 날을 복제한다”는 방식은 금지된다.
- 규칙상, `effectiveStudyDays < 3`인 기록은 일반 복습 일정의 최소 수량을 만족할 수 없으므로, 다음 기준으로 상태를 분리한다.
  - `effectiveStudyDays = 0`: `INSUFFICIENT_WINDOW`로 처리한다.
  - `1 <= effectiveStudyDays < 3` 또는 최소 가용일이 부족하지만 일정이 남아 있는 경우: `CRAM_MODE_REQUIRED`로 처리한다.
- 권장 기본값: `effectiveStudyDays < 3`이면 `CRAM_MODE_REQUIRED` 또는 `INSUFFICIENT_WINDOW`를 사용하되, 둘 사이를 불명확하게 두지 않는다.

## 5. 일정 압축 및 부족 상태 정의

### 5.1 기본 간격 배열

- 기본 간격 배열은 다음과 같다.

`[1, 3, 7, 14, 30, 60, 120]` calendar days

- 각 값은 복습 간격의 기준이며, 모든 값은 LocalDate 기준의 일수 차이이다.
- 구현 시 값은 정수이며, 시간 단위나 타임스탬프 차이가 아니다.

### 5.2 기본 규칙

- 첫 복습은 보통 `studyDate + 1 calendar day`에 배치한다.
- 마지막 일반 복습은 가능하면 `lastReviewDate`에 배치한다.
- 같은 학습기록에 대해 중복 날짜 생성은 금지한다.
- 같은 학습기록의 연속 복습 간격은 최소 1 calendar day 이상이어야 한다.
- 다음 날짜는 자동 스케줄에서 제외한다.
  - `studyDate`
  - `lastReviewDate` 이후의 날짜
  - 모든 buffer date
  - `examDate`
  - 이미 동일 학습기록에 사용된 날짜
- 복습은 반드시 단조 증가하는 LocalDate 순서로 생성해야 한다.

### 5.3 일정 압축(Compression) 기본 전략

- 일정이 짧아 기본 간격 배열을 그대로 맞출 수 없으면, 복습 간격을 압축하여 다시 배치한다.
- 압축의 목표는 의미 있는 초기 짧음/후기 길음 경향을 유지하되, 날짜는 반드시 서로 다르고, 순서는 유지하며, 1일 최소 간격을 지키는 것이다.
- 권장 기본 방식은 아래와 같다.

1. 유효한 복습 후보 날짜를 목록화한다.
   - `candidateDates = [d | studyDate + 1 <= d <= lastReviewDate, d not in bufferDates, d != examDate]`
   - `candidateDates`는 오름차순이며 중복이 없다.
2. 마지막 복습을 보장한다.
   - `lastReviewDate`가 후보에 포함되고, 최소 1개의 유효한 날짜가 추가로 존재하면, 마지막 복습을 `lastReviewDate`로 고정한다.
   - 이후 남은 리뷰는 `studyDate + 1`부터 `lastReviewDate - 1` 사이의 후보 날짜로 채운다.
3. 남은 복습 날짜를 선택한다.
   - 기본 간격을 각 후보 구간의 상대 위치로 매핑한다.
   - 예: 후보 날짜 수가 `N`, 목표 리뷰 수가 `T`, 마지막 날짜 고정 시 남은 리뷰 수는 `T - 1`이다.
   - 각 남은 리뷰는 `slotIndex`를 기준으로 위치를 계산한다.
   - `rawPosition = roundHalfUp(slotIndex * (N - 1) / max(1, (T - 2)))` 또는 동등한 구현의 정수화 방식으로 결정한다.
   - 가능한 구현은 “비례 위치 배치”를 사용하되, 최종 결과는 정수 인덱스의 날짜로 변환한다.
4. 충돌 처리.
   - 서로 다른 리뷰가 같은 후보 날짜에 매핑되면, 해당 리뷰는 순서대로 다음 규칙으로 해결한다.
   - 우선순위: 가장 이른 날짜부터 정렬한 후, 각 항목은 먼저 가장 가까운 이전 가능한 날짜로 이동한다.
   - 이전 가능한 날짜가 없으면 가장 가까운 이후의 가능한 날짜로 이동한다.
   - 이때 `studyDate`, 버퍼일, `examDate`, 이미 사용된 날짜, `lastReviewDate` 이후 날짜를 제외한다.
   - 순서와 1일 이상 간격을 깨지 않는 범위에서만 이동한다.
5. 타이 브레이킹.
   - 같은 거리의 후보가 여러 개일 경우, 더 이른 날짜를 우선한다.
   - 그 이후에도 동점이면 더 작은 인덱스를 우선한다.
   - 예: `2026-09-10`과 `2026-09-11`이 같은 거리라면 `2026-09-10`을 먼저 선택한다.
6. 집계 규칙.
   - 목표 리뷰 수보다 후보 날짜가 적으면, 가능한 최대 개수만 생성하고 상태를 `INSUFFICIENT_WINDOW`로 한다.
   - 같은 학습기록에 동일한 날짜를 두 번 생성하는 것은 허용하지 않는다.
   - 유효한 후보 날짜 수가 부족하면, 마지막 복습을 고정하더라도 남는 자리가 없으면 조기 종료한다.

### 5.4 압축된 일정 예시

- `studyDate = 2026-10-01`
- `examDate = 2026-10-15`
- `finalReviewBufferDays = 1`
- `lastReviewDate = 2026-10-13`
- `candidateDates = 2026-10-02, 2026-10-03, ..., 2026-10-13` 중 `2026-10-14`, `2026-10-15` 제외
- 목표가 5회라면, 일반적으로는 기본 간격 배열에 가깝게 배치하되, 후보 수가 부족하면 마지막 날짜를 `2026-10-13`에 고정하고 나머지 4개를 앞쪽에서 가능한 위치에 배치한다.
- “No duplicate same-day review” 규칙을 지키며 간격은 최소 1일이다.

### 5.5 부족 상태 정의

- `INSUFFICIENT_WINDOW`는 “유효한 연속 복습 창이 너무 짧아 표준 일정이 기하급수적 또는 법적 기준상 불가함”을 의미한다.
- `CRAM_MODE_REQUIRED`는 “정상 복습은 불가하지만, 짧은 자극형 복습/퀴즈/최종 복습 안내를 제공해야 할 때”를 의미한다.
- `INSUFFICIENT_WINDOW`과 `CRAM_MODE_REQUIRED`의 의미가 겹치지 않도록 다음 기본 규칙을 둔다.
  - `effectiveStudyDays = 0`: `INSUFFICIENT_WINDOW`
  - `1 <= effectiveStudyDays < 3`: `CRAM_MODE_REQUIRED`
- 둘 다 일반 보통 자동 스케줄을 생성하지 않으며, 자동 알림은 제한적으로 제공한다.

## 6. 난이도·중요도·숙련도 보정 규칙

### 6.1 난이도(difficulty)

- `difficulty = hard`: 기본 목표 복습 수에 +1을 적용한다.
- `difficulty = easy`: 최소 요구 복습 수를 줄이지 않는다. 보통은 추가 복습을 생성하지 않는다.
- `difficulty` 값이 유효하지 않은 경우 기본값은 `normal` 또는 `medium`로 처리한다. 이 문서에서 기본값을 `normal`로 정한다.
- 허용 값 범위는 `easy`, `normal`, `hard`로 둔다. 유효하지 않은 값은 `normal`로 보정한다.

### 6.2 중요도(importance)

- `importance = high`: 일정 과부하나 용량 충돌 시, 최종 복습 날짜 보장 우선순위가 더 높다.
- `importance = normal` 또는 `low`: 기본값은 `normal`로 처리한다.
- `importance = high`가 아니면 기본적으로 자동 스케줄의 우선순위를 낮게 유지한다.
- 중요도는 자동 스케줄 생성 시 우선순위의 기준이 되지만, 최소 복습 수와 시험일 규칙을 우회하지 않는다.

### 6.3 초기 숙련도(initialMastery)

- `initialMastery` 값 범위는 `1`에서 `5`까지로 제한한다.
- `initialMastery 1 ~ 2`: 추가 복습 필요를 반영하되, `hard`와 중복 시 최대 1개 추가 복습만 허용한다.
- `initialMastery 4 ~ 5`: 최소 복습 수는 유지하고, 보통은 추가 복습을 생성하지 않는다.
- `initialMastery = 3`: 중립값이다. 추가 복습 없이 최소 요구 수만 유지한다.
- 잘못된 값이나 비정상 범위는 `3`으로 보정한다.

### 6.4 조정 우선순위

- 난이도와 숙련도는 모두 “추가 리뷰 필요”를 의미하는 입력일 수 있지만, 구현은 다음 우선순위를 따른다.
- 기본 우선순위:
  1. 최종 복습 보장 여부
  2. 최소 요구 복습 수 충족 여부
  3. `hard` 또는 `initialMastery 1~2`로 인한 추가 보정
  4. `easy` 또는 `initialMastery 4~5`로 인한 추가 보정 방지
- 권장 기본값: `hard` + mastery 1~2가 동시에 존재하면, 추가 복습을 1회로 제한한다. 두 조건을 합쳐 2회 추가를 만들지 않는다.
- 권장 기본값: `easy` 또는 mastery 4~5는 추가 복습을 “막는” 역할만 하며, 최소 복습 수를 줄이지 않는다.

### 6.5 리뷰 결과에 따른 다음 리뷰 이동

- 리뷰 결과가 `failed` 또는 `difficult`이면, 다음 예정 복습을 1 calendar day 앞당긴다.
- 리뷰 결과가 `successful` 또는 `easy`이면, 다음 예정 복습을 1 calendar day 늦춘다.
- 보정은 다음 원칙을 따른다.
  - 보정 계산 결과가 `lastReviewDate` 이후가 되면 보정하지 않는다.
  - 보정 결과가 동일 날짜이면 보정하지 않는다.
  - 보정 결과가 같은 학습기록의 다른 리뷰와 중복되면 보정하지 않는다.
  - 보정은 이미 완료된 리뷰를 이동시키지 않는다.
  - 과거 완료 결과를 수정하지 않는다.
  - 보정은 보류 중인 다음 리뷰만 변경한다.
- 유효한 날짜가 없으면 원래 날짜를 유지한다.
- `failed/difficult`의 조정 방향이 유효하지 않으면, 가장 가까운 유효 날짜를 요청 방향으로 이동시킨다. 예: 1일 앞당기려 했는데 해당 날짜가 이미 사용되거나 buffer date면 그 이전 가능한 날짜를 찾고, 없으면 원래 날짜를 유지한다.
- `successful/easy`의 조정 방향이 유효하지 않으면 가장 가까운 유효 날짜를 뒤로 이동한다.

### 6.6 예시: 난이도 및 숙련도 조정

- `difficulty = hard`, `initialMastery = 2`, 기본 요구 수 = 4회, 현재 수 = 4회
- 보정 규칙: 추가 1회만 허용한다.
- 결과: 정상 계획의 목표 수는 5회로 증가할 수 있으나, 동일 학습기록에 대해 2회 추가가 되지 않는다.
- `difficulty = easy`, `initialMastery = 5`, 목표 수 = 4회
- 보정 규칙: 추가 복습을 만들지 않고, 최소 수 4회를 유지한다.

### 6.7 용량 충돌

- 여러 학습기록이 같은 일정 안에서 충돌할 때는 다음을 우선한다.
  1. 시험일과 마지막 복습 보장 우선순위
  2. 높은 중요도 학습기록
  3. 더 많은 남은 유효 날짜가 있는 경우
  4. 더 늦게 생성된 기록
- 자원/용량 제한은 별도 정책으로 정의한다. 본 정책은 개인 학습기록의 일정 보장 규칙을 다루며, 전역 일일 작업량 상한을 자동으로 부과하지 않는다.
- “전역 일일 작업량 상한”은 별도 설정이 있는 경우에만 적용되며, 이는 본 정책의 기본 규칙이 아니다.

## 7. 상태값 목록과 상태 전이 규칙

### 7.1 상태값 목록

| 상태값 | 의미 | 기본 진입 조건 |
|---|---|---|
| `DRAFT` 또는 `UNSCHEDULED` | 아직 스케줄이 확정되지 않은 기록 | 학습기록 생성 직후, 스케줄 계산 전 |
| `SCHEDULED` | 정상 일반 복습 일정이 생성됨 | 유효한 시험일과 충분한 일정 창 |
| `INSUFFICIENT_WINDOW` | 일정 창이 너무 짧아 정상 스케줄을 만들 수 없음 | `effectiveStudyDays = 0` 또는 후보 날짜 부족 |
| `CRAM_MODE_REQUIRED` | 정상 스케줄은 불가하지만 짧은 약식 복습 안내 필요 | `1 <= effectiveStudyDays < 3` |
| `COMPLETED` | 학습기록이 완료됨 | 사용자가 완료하거나 일정이 종료됨 |
| `EXPIRED` 또는 `MISSED` | 지연된 미완료 복습 | 예정 리뷰 날짜가 지났지만 아직 완료되지 않음 |
| `CANCELLED` 또는 `ARCHIVED` | 사용자가 숨기거나 비활성화 | 사용자 동작 또는 정책상 보관 |

### 7.2 상태 전이 규칙

- `DRAFT` 또는 `UNSCHEDULED` → `SCHEDULED`
  - 조건: 시험일이 유효하고, `availableDaysForNewLearning >= 3`, `effectiveStudyDays >= 3`이며, 정상 스케줄이 생성 가능하다.
  - 일반 복습 일정 생성 가능.
  - 자동 알림 허용.
- `DRAFT` 또는 `UNSCHEDULED` → `INSUFFICIENT_WINDOW`
  - 조건: `effectiveStudyDays = 0` 또는 후보 날짜 수가 목표 리뷰 수를 충족하지 못한다.
  - 일반 복습 생성 없음.
  - 자동 알림은 보통 제한적이며, 부족 메시지만 표시.
- `DRAFT` 또는 `UNSCHEDULED` → `CRAM_MODE_REQUIRED`
  - 조건: `1 <= effectiveStudyDays < 3`이며 시험일은 유효하나 정상 복습 수를 확보할 수 없다.
  - 일반 복습 생성 없음.
  - 짧은 퀴즈/회상/최종 준비 안내를 표시할 수 있다.
- `SCHEDULED` → `COMPLETED`
  - 조건: 모든 예정 리뷰가 완료되었거나 사용자가 학습기록을 완료 처리했다.
  - 완료된 리뷰는 다시 이동하거나 재배치하지 않는다.
- `SCHEDULED` → `EXPIRED` 또는 `MISSED`
  - 조건: 특정 리뷰 날짜를 지났지만 아직 완료 처리되지 않았다.
  - 미완료 상태를 “만료”로 표시하되, 과거 결과를 보존한다.
  - 이후 재계산은 “그 이후의 미래 리뷰”만 적용한다.
- `SCHEDULED` 또는 `COMPLETED` → `CANCELLED` 또는 `ARCHIVED`
  - 조건: 사용자가 수동으로 숨김, 보관, 취소.
  - 기록 자체는 보관되지만, 자동 알림은 중지된다.
- `COMPLETED` → `SCHEDULED`
  - 완료 상태에서 다시 스케줄을 재생성하지 않는다. 다시 학습을 시작하는 별도 기록 생성이 필요하다.

### 7.3 재계산 규칙

- 시험일 또는 버퍼 값이 변경되면, 영향받는 학습기록의 “미래 보류 중인 리뷰”만 재계산한다.
- 이미 완료된 리뷰와 그 결과 기록은 보존한다.
- 완료된 리뷰의 날짜를 이동시키지 않는다.
- 기록의 텍스트 내용을 수정해도 기본 원칙은 `studyDate`를 재계산하거나 변경하지 않는다. 추천 기본값: `studyDate`는 생성 후 불변으로 유지된다.

### 7.4 오늘 날짜가 지나간 일정 처리

- `todayDate`가 예정 복습 날짜를 초과했지만 해당 리뷰가 완료되지 않은 경우, `MISSED` 또는 `EXPIRED`로 표시한다.
- 이미 완료된 리뷰는 `MISSED`로 되돌리지 않는다.
- 과거 완료된 리뷰는 내부 기록으로 유지되며, 결과 히스토리는 변경하지 않는다.
- 미완료 리뷰가 있는 경우, 다음 유효 날짜를 재계산할 수 있으나, 과거의 완료 결과와 이미 수행한 리뷰의 날짜는 변경하지 않는다.

### 7.5 사용자 수동 완료/제외/보관

- 추천 기본값: 사용자는 리뷰를 수동으로 완료할 수 있다.
- 사용자가 “건너뛰기”를 선택하면 해당 리뷰 인스턴스를 `ARCHIVED` 또는 `DISMISSED`로 표시할 수 있다.
- 단, `ARCHIVED`는 개별 리뷰 인스턴스나 기록 단위 중 하나를 의미할 수 있지만, 자동 스케줄은 그 날짜를 새로 채우지 않는다.
- 사용자 수동 조정은 과거 완료 결과를 수정하지 않는다.

## 8. 사용자에게 보여줄 차단·경고 문구 초안

아래 문구는 사용자 노출용 초안이다. 메시지는 과학적 예측을 주장하지 않고, 정책 기반 안내를 제공한다.

### 8.1 시험일 차단

- “시험일이 너무 빠릅니다. 최소 3일 이상의 정상 학습/복습 가능 날짜가 필요합니다. 시험일을 늦추거나, 시험 전 버퍼를 줄여서 다시 설정해 주세요.”
- “가장 빠른 허용 시험일은 2026-10-06입니다. 오늘은 2026-10-01이며, 시험일과 버퍼 구간을 제외하면 최소 3개의 사용 가능한 학습일이 필요합니다.”

### 8.2 가장 빠른 허용 시험일 표시

- “최소 학습 가능 기간을 충족하는 가장 빠른 시험일은 2026-10-06입니다.”
- “시험일과 시험 전 버퍼일은 자동 일반 복습 대상에서 제외됩니다.”

### 8.3 학습기록의 충분하지 않은 유효 학습일

- “이 기록은 마지막 일반 복습 가능일 기준으로 유효 학습일이 2일뿐이라, 정상 복습 스케줄을 만들 수 없습니다. 최소 3일이 필요합니다.”
- “이 기록은 시험일이 너무 가까워 자동 일반 복습을 만들 수 없으며, 짧은 회상 또는 최종 점검이 필요합니다.”

### 8.4 CRAM 모드 설명

- “이 기록은 시험 전에 짧은 시간 안에 정리해야 하는 상태입니다. 자동 일반 복습 대신 짧은 회상, 퀴즈, 오답 설명, 최종 점검을 우선하세요.”
- “CRAM 모드는 과학적 기억 예측을 의미하지 않고, 시험 직전에 사용할 수 있는 짧은 점검 모드입니다.”

### 8.5 시험일/버퍼일 제외 안내

- “시험일과 시험 전 버퍼일에는 자동 일반 복습이 생성되지 않습니다. 이 날짜는 최종 준비와 점검용으로만 사용됩니다.”
- “시험일 및 버퍼 구간은 자동 일정에서 제외됩니다.”

### 8.6 압축 일정 안내

- “시험이 가까워서 일정이 압축되었습니다. 복습은 짧은 간격으로 앞쪽에 배치되고, 마지막 복습은 시험 직전에 유지됩니다.”
- “일정이 짧아 기본 간격을 그대로 유지할 수 없어 가장 가까운 유효 날짜에 배치했습니다.”

### 8.7 부족 창 안내

- “이 기록은 일정이 너무 짧아 정상 복습 회차를 만들 수 없습니다. 같은 날에 여러 번 복습을 생성하지 않고, 짧은 회상 또는 종합 점검으로 보완하세요.”
- “일정 창이 부족하여 자동 스케줄을 만들지 않았습니다. 같은 날짜를 복제하여 수량을 맞추지 않습니다.”

### 8.8 리뷰 결과에 따른 이동 안내

- “틀리거나 어려웠던 회상은 다음 복습을 앞당깁니다.”
- “맞히거나 쉬웠던 회상은 다음 복습을 약간 늦춥니다.”
- “복습 결과는 과거 완료 내역을 변경하지 않고, 다음 보류 중인 리뷰만 조정합니다.”

## 9. 정책상 모호할 수 있는 부분과 권장 기본값

아래는 제품 결정이 필요한 항목이다. 구현 전 결정이 필요한 경우, 아래 기본값을 우선 적용한다.

| 항목 | 모호함 | 권장 기본값 | 구현 및 사용자 경험 영향 |
|---|---|---|---|
| 미래 날짜 학습기록 허용 여부 | `studyDate`를 미래로 넣을 수 있는가 | `studyDate`는 미래 날짜를 허용하지 않는다. 과거/오늘만 허용한다. | 구현 단순화, 일정 계산 예측 가능성 향상 |
| 사용자가 자동 일정 수정 가능 여부 | 사용자가 개별 복습 일자를 직접 변경할 수 있는가 | 자동으로 생성된 리뷰 날짜는 기본적으로 수정 불가. 사용자 수동 변경은 별도 정책으로 제한한다. | 자동화 일관성 유지 |
| 같은 날 완료와 다른 리뷰의 동일 날짜 허용 | 이후 리뷰가 같은 날에 생성될 수 있는가 | 같은 학습기록에 같은 날짜 중복 금지. 다른 기록과는 별개로 처리한다. | 중복 알림 회피 |
| 전역 일일 작업량 상한 | 전체 유저의 일일 작업량 제한이 필요한가 | 기본값 없음. 별도 설정이 없으면 비활성화. | 사용자 경험 과부하 방지 |
| 여러 시험/과목 관계 | 같은 기록이 여러 시험에 연결될 수 있는가 | 하나의 학습기록은 하나의 `examDate`와 하나의 최종 복습 컨텍스트를 갖는다. | 구현 복잡성 감소 |
| 시험일 변경 시 재생성 범위 | 과거에 CRAM 또는 이미 생성된 계획도 재생성이 필요한가 | 변경된 시험일에 영향을 받는 미래 보류 상태만 재계산. 완료 이력 보존. | 사용자가 기존 결과를 잃지 않음 |
| `CRAM_MODE_REQUIRED`와 `INSUFFICIENT_WINDOW` 통합 여부 | 둘을 하나로 합칠지 여부 | 둘을 별도 상태로 유지하되, `effectiveStudyDays = 0`이면 `INSUFFICIENT_WINDOW`, `1~2`이면 `CRAM_MODE_REQUIRED` | 상태 해석 명확성 |
| 미완료 리뷰 알림 | 누락 시 어떤 알림을 보낼지 | 미완료 리뷰는 `MISSED`로 표시하고, 과거 완료 이력은 유지. 보관 기록에는 알림 중지 | 사용자가 혼란 없이 상태 파악 |
| 난이도/중요도/숙련도 범위 기본값 | 허용 값과 기본값이 무엇인지 | `difficulty`: {easy, normal, hard}, 기본값 `normal`; `importance`: {low, normal, high}, 기본값 `normal`; `initialMastery`: [1,5], 기본값 3; `finalReviewBufferDays`: 0 이상 정수, 기본값 1 | 구현 명확성 |

### 9.1 추가 정책 결정 포인트

- `studyDate`는 생성 후 변경 불가를 권장한다. 텍스트 변경은 허용하되, `studyDate`는 불변으로 유지한다.
- 자동 생성된 리뷰 날짜는 사용자 무시 권한이 있더라도, 기본 정책은 “수동 오버라이드”를 허용하되 자동 생성 리뷰를 삭제하는 것은 `SCHEDULED` 상태에서만 가능하도록 제한한다.
- 학습기록과 시험일의 연결은 1:1 관계를 유지한다. 다른 과목, 다른 시험, 중복 사용을 위한 범용적 다중 시험 정책은 별도 확장 정책에서 정의한다.
- `finalReviewBufferDays`의 범위는 `0` 이상의 정수만 허용하고, 값이 없으면 1로 보정한다.

## 10. 이후 TypeScript 구현 시 필요한 함수 목록과 함수별 책임

아래 목록은 이후 TypeScript 구현에서 필요한 함수들의 개념적 명세이며, 구현 코드는 본 문서에 포함하지 않는다. 각 함수는 입력, 출력, 오류 처리, 불변식, 테스트 핵심 경계값을 명확히 정의해야 한다.

### 10.1 `validateExamDate`

- 책임: 시험일이 유효한지 검증하고, 규칙에 맞는지 확인한다.
- 입력: `examDate`, `todayDate`, `finalReviewBufferDays`
- 출력: `{ valid: boolean, reason?: string, earliestAllowedExamDate?: LocalDate }`
- 오류 처리: `examDate` 형식이 잘못되었거나, `finalReviewBufferDays`가 음수/비정수/누락이면 reject.
- 불변식: `examDate`는 `LocalDate`이며, `finalReviewBufferDays >= 0`이어야 한다.
- 테스트 핵심 경계: `finalReviewBufferDays = -1`, `null`, `NaN`, `0`, `1`, `examDate`가 오늘 이후인지, 최소 자격 조건 미충족 여부.

### 10.2 `getEarliestAllowedExamDate`

- 책임: 현재 기준에서 가장 빠른 허용 시험일을 계산한다.
- 입력: `todayDate`, `finalReviewBufferDays`
- 출력: `LocalDate`
- 오류 처리: `todayDate`가 유효하지 않거나 `finalReviewBufferDays`가 유효하지 않으면 에러를 발생시킨다.
- 불변식: 결과는 `todayDate` 이후이며, `daysBetween(todayDate, result) >= 3 + finalReviewBufferDays + 1`이다.
- 테스트 핵심 경계: `finalReviewBufferDays = 0`, `1`, `2`; `todayDate`가 월 경계, 윤년일 때.

### 10.3 `calculateLastReviewDate`

- 책임: 마지막 일반 복습 가능일을 계산한다.
- 입력: `examDate`, `finalReviewBufferDays`
- 출력: `LocalDate`
- 오류 처리: `examDate` 또는 `finalReviewBufferDays`가 유효하지 않으면 reject.
- 불변식: `lastReviewDate < examDate`이며, `daysBetween(lastReviewDate, examDate) = finalReviewBufferDays + 1`.
- 테스트 핵심 경계: `finalReviewBufferDays = 0`, `1`, `2`; 마지막 복습 가능일이 월 경계와 일치하는 경우.

### 10.4 `calculateAvailableDaysForNewLearning`

- 책임: 시험일까지 새 학습을 시작할 수 있는 사용 가능한 일수를 계산한다.
- 입력: `todayDate`, `examDate`, `finalReviewBufferDays`
- 출력: 정수
- 오류 처리: invalid date or negative buffer => error.
- 불변식: 결과는 `daysBetween(todayDate, examDate) - finalReviewBufferDays - 1`이며, 음수 허용하지 않는다. 구현은 음수면 0으로 clamp할지, 원래 값 그대로 반환할지는 정책으로 정해야 한다. 권장 기본값: 음수는 검증 단계에서 차단하고, 계산 결과값은 명시적 검증 이후 반환한다.
- 테스트 핵심 경계: `examDate == todayDate`, `todayDate`와 `examDate`가 3일 차이, 유효/무효 buffer 값.

### 10.5 `calculateEffectiveStudyDays`

- 책임: 각 학습기록의 유효 학습일을 계산한다.
- 입력: `studyDate`, `lastReviewDate`
- 출력: 정수, 최소 0
- 오류 처리: `studyDate` 또는 `lastReviewDate`가 유효하지 않은 경우 reject.
- 불변식: 결과는 `max(0, daysBetween(studyDate, lastReviewDate))`이다.
- 테스트 핵심 경계: `studyDate == lastReviewDate`, `studyDate`가 이후인 경우, 월 경계, 윤년 처리.

### 10.6 `determineMinimumReviewCount`

- 책임: 학습기록의 최소 일반 복습 수량을 결정한다.
- 입력: `effectiveStudyDays`
- 출력: 정수
- 오류 처리: 음수 입력 또는 NaN이면 에러.
- 불변식: 표를 벗어나는 값은 범위에 따라 가장 인접한 행에 매핑되며, 너무 짧은 값은 0을 반환한다.
- 테스트 핵심 경계: 경계 값 2, 3, 6, 7, 13, 14, 29, 30, 59, 60, 119, 120.

### 10.7 `determineTargetReviewCount`

- 책임: 복습 수량 목표를 난이도, 숙련도, 중요도, 일정 창을 종합해 계산한다.
- 입력: `baseMinimumReviewCount`, `difficulty`, `initialMastery`, `importance`, `effectiveStudyDays`, `candidateDateCount`
- 출력: 정수
- 오류 처리: 유효하지 않은 값은 기본값으로 보정한다.
- 불변식: `targetReviewCount >= minimumReviewCount`, `targetReviewCount <= maxFeasibleReviewCount`.
- 테스트 핵심 경계: `hard` + mastery 1~2, `easy` + mastery 4~5, 높은 중요도, 일정 부족, candidate count 초과.

### 10.8 `generateReviewSchedule`

- 책임: 학습기록에 대한 일반 복습 스케줄을 생성한다.
- 입력: `studyDate`, `examDate`, `finalReviewBufferDays`, `targetReviewCount`, `importance`, `difficulty`, `initialMastery`
- 출력: ordered list of review dates, each date unique and sorted
- 오류 처리: 유효하지 않은 날짜, `targetReviewCount <= 0`는 빈 목록 반환 또는 `INSUFFICIENT_WINDOW` 상태를 생성.
- 불변식: 각 날짜는 `studyDate < date <= lastReviewDate`, `date` not in bufferDates, `date != examDate`, no duplicates, 1-day minimum gap.
- 테스트 핵심 경계: 목표 수가 충분한 경우, 마지막 날짜 보장, 압축될 때, 중복 방지, 충돌 처리.

### 10.9 `compressReviewSchedule`

- 책임: 기본 간격이 맞지 않을 때 일정을 압축한다.
- 입력: `candidateDates`, `targetReviewCount`, `finalReviewDate`
- 출력: ordered list, or empty list if impossible
- 오류 처리: 후보 날짜가 부족하면 최대 가능한 수만 반환하고 해당 상태를 외부에 전달.
- 불변식: 오름차순 유지, 중복 금지, 각 연속 간격 >= 1 day, 마지막 리뷰를 보장할 수 있으면 최종 값을 `lastReviewDate`로 유지.
- 테스트 핵심 경계: 마지막 날짜 고정, 동일한 날짜 충돌, 이전/이후 경로 선택, 경계값에서의 이동.

### 10.10 `validateReviewSchedule`

- 책임: 생성된 리뷰 일정이 도메인 규칙을 만족하는지 검증한다.
- 입력: review schedule, `studyDate`, `lastReviewDate`, `examDate`, `finalReviewBufferDays`
- 출력: `{ valid: boolean, errors: string[] }`
- 오류 처리: 중복 날짜, 비정상 순서, buffer 일 포함, 1일 이상 간격 위반, 마지막 리뷰 범위 violation 등을 보고한다.
- 불변식: 스케줄은 오름차순 고유 LocalDate 배열이어야 한다.
- 테스트 핵심 경계: 대표적인 invalid 스케줄, 마지막 리뷰가 허용 범위를 넘는 경우, studyDate 포함 여부.

### 10.11 `adjustNextPendingReviewAfterOutcome`

- 책임: 리뷰 결과를 반영해 다음 보류 중인 리뷰 날짜를 조정한다.
- 입력: `record`, `completedReviewDate`, `outcome`, `lastReviewDate`, `finalReviewBufferDays`
- 출력: 기존 예정 리뷰 목록에서 다음 보류 날짜의 조정 결과
- 오류 처리: 결과 값이 유효하지 않으면 기본값 처리 후 원본 유지.
- 불변식: 이미 완료된 리뷰 미변경, 다음 보류 리뷰만 수정, `lastReviewDate` 후로 이동 금지, 중복 날짜 금지.
- 테스트 핵심 경계: `failed`/`difficult` 앞당김, `successful`/`easy` 늦춤, 유효 날짜 없음, 경계일 변화.

### 10.12 `recalculatePendingReviewsAfterExamChange`

- 책임: 시험일/버퍼 변경 뒤 영향받는 미래 보류 중인 리뷰만 재계산한다.
- 입력: `record`, `oldExamDate`, `newExamDate`, `finalReviewBufferDays`, review history
- 출력: 재계산된 미래 일정
- 오류 처리: exam date invalid => reject.
- 불변식: 완료 리뷰와 이전 결과는 유지, 미래만 변경.
- 테스트 핵심 경계: 시험일 앞당김, 늦춤, buffer 명시적 변경, 기존 완료 리뷰 보존.

### 10.13 `deriveRecordSchedulingStatus`

- 책임: 학습기록의 상태를 계산한다.
- 입력: `studyDate`, `lastReviewDate`, `effectiveStudyDays`, `candidateDates`, `completedReviews`, `examDate`, `finalReviewBufferDays`
- 출력: 상태값 enum
- 오류 처리: invalid date or invalid review count -> reject with descriptive message.
- 불변식: `effectiveStudyDays < 3`는 `CRAM_MODE_REQUIRED` 또는 `INSUFFICIENT_WINDOW`로 전환되며, 정상 일정은 `SCHEDULED`만 허용.
- 테스트 핵심 경계: 충분한 창, 부족 창, CRAM, 완료, missed.

### 10.14 `buildUserFacingSchedulingMessage`

- 책임: 상태와 상황을 사용자에게 설명하는 문구를 생성한다.
- 입력: 상태, 날짜 값, 효과, 정책 관련 변수들
- 출력: 사용자 메시지 문자열
- 오류 처리: 필수 파라미터 누락 시 기본 원인 메시지 생성.
- 불변식: 과학적 예측을 주장하지 않음; 구현은 정책 기반 설명만 제공.
- 테스트 핵심 경계: 시험일 차단, 가장 빠른 허용 시험일, 압축 일정, CRAM 모드, 실패/성공 결과 반영.

## 결론

본 정책의 핵심 원칙은 명확하다. 각 학습기록은 독립적이고, 일정은 시험일과 버퍼를 제외한 LocalDate 기준으로 계산되며, 자동 복습은 결정적이고 재현 가능한 규칙에 따라 생성된다. 일반적인 학습 원칙은 권고사항이며, 앱의 제품 정책은 구현이 반드시 따르는 명시적 규칙이다. 이 문서에서 정의한 규칙은 기술 구현과 사용자 메시지의 기준이 되며, 구현 과정에서 값이 모호해지면 본 문서의 기본값을 우선 적용한다.
