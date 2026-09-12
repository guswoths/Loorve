/**
 * 이 모듈은 시험 준비형 분산 복습 앱을 위한 타입, 검증 유틸리티, LocalDate 계산 유틸리티를 정의한다.
 * 정책 문서의 결정적 규칙을 TypeScript API로 안전하게 옮기기 위한 최소 범위만 포함한다.
 * 실제 복습 일정 생성 알고리즘은 구현하지 않는다. 이 파일은 도메인 모델과 검증기만 제공한다.
 *
 * 핵심 원칙:
 * - 날짜는 LocalDate 문자열(YYYY-MM-DD)만 공개 API로 사용한다.
 * - 내부 달력 연산은 UTC 기반의 안전한 Date.UTC를 사용한다.
 * - 날짜 문자열의 형식과 실제 존재 여부를 엄격하게 검증한다.
 * - 일반 사용자 검증 실패는 throw 대신 ValidationResult 구조로 반환한다.
 */

export type LocalDate = string & { readonly __localDateBrand: unique symbol };

export type Difficulty = 'easy' | 'medium' | 'hard';
export type Importance = 'low' | 'normal' | 'high';
export type ReviewStatus =
  | 'SCHEDULED'
  | 'COMPLETED'
  | 'MISSED'
  | 'RESCHEDULED'
  | 'CRAM_MODE_REQUIRED'
  | 'INSUFFICIENT_WINDOW'
  | 'OVERLOADED_UNRESOLVED';
export type ReviewOutcome = 'EASY' | 'SUCCESS' | 'HARD' | 'FAILED';
export type ExamValidationPolicy =
  | 'BLOCK_IF_ANY_RECORD_INSUFFICIENT'
  | 'WARN_IF_ANY_RECORD_INSUFFICIENT';
export type RescheduleReason =
  | 'EXAM_DATE_CHANGED'
  | 'REVIEW_RESULT'
  | 'USER_OVERRIDE'
  | 'CAPACITY_REBALANCE'
  | 'NONE';

export interface NotificationPlan {
  kind: 'NONE' | 'IN_APP' | 'PUSH';
  hour: number;
  reminder: boolean;
}

export interface Exam {
  id: string;
  title?: string;
  examDate: LocalDate;
  finalReviewBufferDays?: number;
  validationPolicy?: ExamValidationPolicy;
  createdAt: LocalDate;
  updatedAt: LocalDate;
}

export type StudyRecord =
  | {
      id: string;
      examId: string;
      content: string;
      studiedDate: LocalDate;
      subjectId: string;
      subjectName?: never;
      difficulty: Difficulty;
      importance: Importance;
      estimatedReviewMinutes: number;
      initialMastery: 1 | 2 | 3 | 4 | 5;
      optionalMinReviewCount?: number;
      createdAt: LocalDate;
      updatedAt: LocalDate;
    }
  | {
      id: string;
      examId: string;
      content: string;
      studiedDate: LocalDate;
      subjectId?: never;
      subjectName: string;
      difficulty: Difficulty;
      importance: Importance;
      estimatedReviewMinutes: number;
      initialMastery: 1 | 2 | 3 | 4 | 5;
      optionalMinReviewCount?: number;
      createdAt: LocalDate;
      updatedAt: LocalDate;
    };

export interface ReviewSchedule {
  id: string;
  studyRecordId: string;
  reviewIndex: number;
  scheduledDate: LocalDate;
  status: ReviewStatus;
  priorityScore: number;
  estimatedReviewMinutes: number;
  recommendedMethod: string;
  notificationPlan: NotificationPlan;
  dueDaysBeforeExam: number;
  isFinalReview: boolean;
  rescheduleReason?: string;
  createdAt: LocalDate;
  updatedAt: LocalDate;
}

export interface ValidationResult {
  isValid: boolean;
  blockingReasons: string[];
  warnings: string[];
  availableDaysForNewLearning: number;
  earliestAllowedExamDate: LocalDate;
  lastReviewDate: LocalDate;
  userMessage: string;
}

export interface SchedulingResult {
  isValid: boolean;
  validation: ValidationResult;
  schedule: ReviewSchedule[];
  notes: string[];
}

export interface SchedulerConfig {
  timezone: 'Asia/Seoul';
  finalReviewBufferDays: number;
  minimumEffectiveStudyDays: number;
  maxDailyReviewMinutes: number;
  defaultNotificationHour: number;
  reminderNotificationHour: number;
  allowRegularReviewOnDayBeforeExam: boolean;
  examValidationPolicy: ExamValidationPolicy;
  baseIntervals: readonly number[];
  preferredRescheduleRangeDays: number;
}

export const DEFAULT_SCHEDULER_CONFIG: SchedulerConfig = {
  timezone: 'Asia/Seoul',
  finalReviewBufferDays: 1,
  minimumEffectiveStudyDays: 3,
  maxDailyReviewMinutes: 120,
  defaultNotificationHour: 9,
  reminderNotificationHour: 19,
  allowRegularReviewOnDayBeforeExam: false,
  examValidationPolicy: 'BLOCK_IF_ANY_RECORD_INSUFFICIENT',
  baseIntervals: [1, 3, 7, 14, 30, 60, 120],
  preferredRescheduleRangeDays: 2,
};

export function createDefaultSchedulerConfig(
  overrides: Partial<SchedulerConfig> = {},
): SchedulerConfig {
  return {
    ...DEFAULT_SCHEDULER_CONFIG,
    ...overrides,
    baseIntervals: overrides.baseIntervals ?? DEFAULT_SCHEDULER_CONFIG.baseIntervals,
  };
}

/**
 * SchedulerConfig의 값이 정책 범위를 지키는지 검사한다.
 * notificationHour는 0~23 범위, maxDailyReviewMinutes는 0 이상 정수, preferredRescheduleRangeDays는 1 이상 정수로 제한한다.
 */
export function validateSchedulerConfig(
  config: Partial<SchedulerConfig> = {},
): { isValid: boolean; errors: string[]; normalized: SchedulerConfig } {
  const normalized = createDefaultSchedulerConfig(config);
  const errors: string[] = [];

  if (!Number.isInteger(normalized.finalReviewBufferDays) || normalized.finalReviewBufferDays < 0) {
    errors.push('finalReviewBufferDays는 0 이상 정수여야 합니다.');
  }
  if (!Number.isInteger(normalized.minimumEffectiveStudyDays) || normalized.minimumEffectiveStudyDays < 1) {
    errors.push('minimumEffectiveStudyDays는 1 이상 정수여야 합니다.');
  }
  if (!Number.isInteger(normalized.maxDailyReviewMinutes) || normalized.maxDailyReviewMinutes < 0) {
    errors.push('maxDailyReviewMinutes는 0 이상 정수여야 합니다.');
  }
  if (!Number.isInteger(normalized.defaultNotificationHour) || normalized.defaultNotificationHour < 0 || normalized.defaultNotificationHour > 23) {
    errors.push('defaultNotificationHour는 0~23 범위의 정수여야 합니다.');
  }
  if (!Number.isInteger(normalized.reminderNotificationHour) || normalized.reminderNotificationHour < 0 || normalized.reminderNotificationHour > 23) {
    errors.push('reminderNotificationHour는 0~23 범위의 정수여야 합니다.');
  }
  if (!Number.isInteger(normalized.preferredRescheduleRangeDays) || normalized.preferredRescheduleRangeDays < 1) {
    errors.push('preferredRescheduleRangeDays는 1 이상 정수여야 합니다.');
  }
  if (!Array.isArray(normalized.baseIntervals) || normalized.baseIntervals.length === 0) {
    errors.push('baseIntervals는 비어 있지 않은 정수 배열이어야 합니다.');
  } else if (normalized.baseIntervals.some((value) => !Number.isInteger(value) || value < 1)) {
    errors.push('baseIntervals는 1 이상 정수만 포함해야 합니다.');
  }

  return { isValid: errors.length === 0, errors, normalized };
}

/**
 * StudyRecord의 필드 검증을 수행한다.
 * `subjectId`와 `subjectName`은 상호 배타적이며, 최소한 하나는 있어야 한다.
 */
export function validateStudyRecord(record: Partial<StudyRecord>): { isValid: boolean; errors: string[] } {
  const errors: string[] = [];

  if (!record || typeof record !== 'object') {
    return { isValid: false, errors: ['StudyRecord는 객체여야 합니다.'] };
  }

  if (typeof record.content !== 'string' || record.content.trim().length === 0) {
    errors.push('content는 비어 있으면 안 됩니다.');
  }
  if (typeof record.estimatedReviewMinutes !== 'number' || !Number.isInteger(record.estimatedReviewMinutes) || record.estimatedReviewMinutes < 1) {
    errors.push('estimatedReviewMinutes는 1 이상 정수여야 합니다.');
  }
  if (typeof record.initialMastery !== 'number' || !Number.isInteger(record.initialMastery) || record.initialMastery < 1 || record.initialMastery > 5) {
    errors.push('initialMastery는 1~5 범위의 정수여야 합니다.');
  }
  if (record.optionalMinReviewCount !== undefined && (!Number.isInteger(record.optionalMinReviewCount) || record.optionalMinReviewCount < 0)) {
    errors.push('optionalMinReviewCount는 0 이상 정수여야 합니다.');
  }
  if (record.subjectId === undefined && record.subjectName === undefined) {
    errors.push('subjectId 또는 subjectName 중 하나는 필수입니다.');
  }
  if (record.subjectId !== undefined && record.subjectName !== undefined) {
    errors.push('subjectId와 subjectName은 함께 사용할 수 없습니다.');
  }

  return { isValid: errors.length === 0, errors };
}

const DAY_IN_MS = 24 * 60 * 60 * 1000;

/**
 * LocalDate 문자열을 엄격하게 검증한다.
 * 정규식과 최종 Date.UTC 기반 비교를 함께 사용해 2026-02-29, 2026-13-01, 2026-00-10 등을 거절한다.
 */
export function isValidLocalDate(date: string): boolean {
  if (typeof date !== 'string') {
    return false;
  }

  try {
    parseLocalDate(date);
    return true;
  } catch {
    return false;
  }
}

/**
 * 문자열 LocalDate를 {year, month, day} 구조로 파싱한다.
 * 파싱 실패와 불가능한 날짜는 RangeError로 처리한다.
 */
export function parseLocalDate(date: string): { year: number; month: number; day: number } {
  if (typeof date !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    throw new RangeError(`유효하지 않은 LocalDate 문자열입니다: ${String(date)}`);
  }

  const match = /^([0-9]{4})-([0-9]{2})-([0-9]{2})$/.exec(date);
  if (!match) {
    throw new RangeError(`유효하지 않은 LocalDate 문자열입니다: ${date}`);
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);

  if (!Number.isInteger(year) || year < 1) {
    throw new RangeError(`LocalDate의 연도는 1 이상이어야 합니다: ${date}`);
  }
  if (!Number.isInteger(month) || month < 1 || month > 12) {
    throw new RangeError(`LocalDate의 월은 1~12 범위여야 합니다: ${date}`);
  }
  if (!Number.isInteger(day) || day < 1 || day > 31) {
    throw new RangeError(`LocalDate의 일은 1~31 범위여야 합니다: ${date}`);
  }

  const utcDate = new Date(Date.UTC(year, month - 1, day));
  const reconstructedYear = utcDate.getUTCFullYear();
  const reconstructedMonth = utcDate.getUTCMonth() + 1;
  const reconstructedDay = utcDate.getUTCDate();

  if (
    reconstructedYear !== year ||
    reconstructedMonth !== month ||
    reconstructedDay !== day
  ) {
    throw new RangeError(`존재하지 않는 날짜입니다: ${date}`);
  }

  return { year, month, day };
}

/**
 * LocalDate를 문자열로 변환한다. 내부적으로 Date.UTC 기반으로 만들고 UTC getters만 사용한다.
 */
export function formatLocalDate(parts: { year: number; month: number; day: number }): LocalDate {
  const { year, month, day } = parts;
  if (!Number.isInteger(year) || year < 1) {
    throw new RangeError(`LocalDate 형식의 연도는 1 이상이어야 합니다: ${String(year)}`);
  }
  if (!Number.isInteger(month) || month < 1 || month > 12) {
    throw new RangeError(`LocalDate 형식의 월은 1~12 범위여야 합니다: ${String(month)}`);
  }
  if (!Number.isInteger(day) || day < 1 || day > 31) {
    throw new RangeError(`LocalDate 형식의 일은 1~31 범위여야 합니다: ${String(day)}`);
  }

  const normalized = new Date(Date.UTC(year, month - 1, day));
  const normalizedYear = normalized.getUTCFullYear();
  const normalizedMonth = normalized.getUTCMonth() + 1;
  const normalizedDay = normalized.getUTCDate();

  if (
    normalizedYear !== year ||
    normalizedMonth !== month ||
    normalizedDay !== day
  ) {
    throw new RangeError(`유효하지 않은 날짜 조합입니다: ${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`);
  }

  const value = `${normalizedYear}-${String(normalizedMonth).padStart(2, '0')}-${String(normalizedDay).padStart(2, '0')}` as LocalDate;
  return value;
}

/**
 * 입력 값이 정수인지 엄격하게 검사한다.
 */
function requireInteger(value: unknown, fieldName: string): number {
  if (typeof value !== 'number' || !Number.isInteger(value)) {
    throw new RangeError(`${fieldName}은(는) 정수여야 합니다.`);
  }
  return value;
}

/**
 * LocalDate 문자열을 UTC 기준 Date 객체로 변환한다.
 * public API는 문자열만 노출하고, 내부 계산만 Date 객체를 사용한다.
 */
function toUtcDate(date: LocalDate): Date {
  const parts = parseLocalDate(date);
  return new Date(Date.UTC(parts.year, parts.month - 1, parts.day));
}

/**
 * LocalDate에 day 수를 더한다.
 */
export function addDays(date: LocalDate, days: number): LocalDate {
  const safeDays = requireInteger(days, 'days');
  const utcDate = toUtcDate(date);
  utcDate.setUTCDate(utcDate.getUTCDate() + safeDays);

  return formatLocalDate({
    year: utcDate.getUTCFullYear(),
    month: utcDate.getUTCMonth() + 1,
    day: utcDate.getUTCDate(),
  });
}

/**
 * LocalDate에서 day 수를 뺀다.
 */
export function subtractDays(date: LocalDate, days: number): LocalDate {
  return addDays(date, -days);
}

/**
 * LocalDate 비교 결과를 반환한다.
 */
export function compareLocalDates(a: LocalDate, b: LocalDate): -1 | 0 | 1 {
  const delta = toUtcDate(a).getTime() - toUtcDate(b).getTime();
  if (delta < 0) return -1;
  if (delta > 0) return 1;
  return 0;
}

/**
 * 두 날짜 사이의 캘린더 일수를 계산한다.
 * 결과는 startDate에서 endDate까지 경계 기준으로 계산된 signed count다.
 */
export function daysBetween(start: LocalDate, end: LocalDate): number {
  const startMs = toUtcDate(start).getTime();
  const endMs = toUtcDate(end).getTime();
  const diffMs = endMs - startMs;
  const diffDays = diffMs / DAY_IN_MS;

  if (!Number.isInteger(diffDays)) {
    throw new RangeError(`Date difference is not a whole calendar day: ${start} -> ${end}`);
  }

  return diffDays;
}

export function isBefore(a: LocalDate, b: LocalDate): boolean {
  return compareLocalDates(a, b) < 0;
}

export function isAfter(a: LocalDate, b: LocalDate): boolean {
  return compareLocalDates(a, b) > 0;
}

export function isSameOrBefore(a: LocalDate, b: LocalDate): boolean {
  return compareLocalDates(a, b) <= 0;
}

export function isSameOrAfter(a: LocalDate, b: LocalDate): boolean {
  return compareLocalDates(a, b) >= 0;
}

/**
 * 시험일의 마지막 일반 복습 가능일을 계산한다.
 * 정책 요구상 getLastReviewDate(examDate, buffer) = examDate - buffer calendar days다.
 */
export function getLastReviewDate(examDate: LocalDate, finalReviewBufferDays: number): LocalDate {
  if (!Number.isInteger(finalReviewBufferDays) || finalReviewBufferDays < 0) {
    throw new RangeError('finalReviewBufferDays는 0 이상 정수여야 합니다.');
  }

  return subtractDays(examDate, finalReviewBufferDays);
}

/**
 * 학습 시작 직후 첫 번째 가능한 복습 날짜이다. 기본값은 학습일 + 1이다.
 */
export function getReviewStartDate(studiedDate: LocalDate): LocalDate {
  return addDays(studiedDate, 1);
}

/**
 * 학습 기록의 유효 학습일 수를 계산한다.
 * 이 구현은 정책 문서와 기술 요구의 'day after studiedDate' 해석을 사용한다.
 * 즉, 시작일 자체는 포함하지 않고, 학습 다음 날부터 마지막 복습 가능일까지의 날짜 수를 센다.
 */
export function getEffectiveStudyDays(
  studiedDate: LocalDate,
  examDate: LocalDate,
  finalReviewBufferDays: number,
): number {
  const lastReviewDate = getLastReviewDate(examDate, finalReviewBufferDays);
  const raw = daysBetween(studiedDate, lastReviewDate) - 1;
  return Math.max(0, raw);
}

/**
 * 새 학습을 시작할 수 있는 사용 가능한 일수를 계산한다.
 * 정책 요구상 오늘 이후부터 마지막 복습 가능일 전까지의 유효 날짜를 계산한다.
 * 계산은 다음 날부터 마지막 복습 가능일까지 포함하는 의미를 가진다.
 */
export function getAvailableDaysForNewLearning(
  todayDate: LocalDate,
  examDate: LocalDate,
  finalReviewBufferDays: number,
): number {
  const lastReviewDate = getLastReviewDate(examDate, finalReviewBufferDays);
  const raw = daysBetween(todayDate, lastReviewDate) - 1;
  return Math.max(0, raw);
}

/**
 * 가장 빠른 허용 시험일을 계산한다.
 * 요구사항: todayDate + buffer + minimum + 1 calendar days
 */
export function getEarliestAllowedExamDate(
  todayDate: LocalDate,
  finalReviewBufferDays: number,
  minimumEffectiveStudyDays: number,
): LocalDate {
  if (!Number.isInteger(finalReviewBufferDays) || finalReviewBufferDays < 0) {
    throw new RangeError('finalReviewBufferDays는 0 이상 정수여야 합니다.');
  }
  if (!Number.isInteger(minimumEffectiveStudyDays) || minimumEffectiveStudyDays < 1) {
    throw new RangeError('minimumEffectiveStudyDays는 1 이상 정수여야 합니다.');
  }

  return addDays(
    todayDate,
    finalReviewBufferDays + minimumEffectiveStudyDays + 1,
  );
}

/**
 * 시험일을 검증한다.
 * 정상 사용자 입력 실패는 예외 대신 ValidationResult를 반환한다.
 */
export function validateExamDate(
  todayDate: LocalDate,
  examDate: LocalDate,
  config: Partial<SchedulerConfig> = {},
): ValidationResult {
  const effectiveConfig = createDefaultSchedulerConfig(config as Partial<SchedulerConfig>);
  const blockingReasons: string[] = [];
  const warnings: string[] = [];

  let normalizedToday: { year: number; month: number; day: number } | null = null;
  let normalizedExam: { year: number; month: number; day: number } | null = null;

  try {
    normalizedToday = parseLocalDate(todayDate);
  } catch {
    blockingReasons.push('todayDate는 유효한 YYYY-MM-DD 형식이어야 합니다.');
  }

  try {
    normalizedExam = parseLocalDate(examDate);
  } catch {
    blockingReasons.push('examDate는 유효한 YYYY-MM-DD 형식이어야 합니다.');
  }

  if (normalizedToday && normalizedExam && compareLocalDates(examDate, todayDate) <= 0) {
    blockingReasons.push('시험일은 오늘보다 늦어야 합니다.');
  }

  if (!Number.isInteger(effectiveConfig.finalReviewBufferDays) || effectiveConfig.finalReviewBufferDays < 0) {
    blockingReasons.push('finalReviewBufferDays는 0 이상 정수여야 합니다.');
  }
  if (!Number.isInteger(effectiveConfig.minimumEffectiveStudyDays) || effectiveConfig.minimumEffectiveStudyDays < 1) {
    blockingReasons.push('minimumEffectiveStudyDays는 1 이상 정수여야 합니다.');
  }

  const lastReviewDate = (() => {
    try {
      return getLastReviewDate(examDate, effectiveConfig.finalReviewBufferDays);
    } catch {
      return examDate;
    }
  })();

  const availableDaysForNewLearning = (() => {
    try {
      return getAvailableDaysForNewLearning(todayDate, examDate, effectiveConfig.finalReviewBufferDays);
    } catch {
      return 0;
    }
  })();

  const earliestAllowedExamDate = (() => {
    try {
      return getEarliestAllowedExamDate(
        todayDate,
        effectiveConfig.finalReviewBufferDays,
        effectiveConfig.minimumEffectiveStudyDays,
      );
    } catch {
      return todayDate;
    }
  })();

  if (
    availableDaysForNewLearning < effectiveConfig.minimumEffectiveStudyDays
  ) {
    blockingReasons.push(
      `최소 ${effectiveConfig.minimumEffectiveStudyDays}일 이상의 사용 가능한 학습일이 필요합니다. 가장 빠른 허용 시험일은 ${earliestAllowedExamDate}입니다.`,
    );
  }

  if (effectiveConfig.examValidationPolicy === 'WARN_IF_ANY_RECORD_INSUFFICIENT') {
    warnings.push(
      '기록이 일정 부족 상태에 있어도 경고만 표시하고 저장을 막지 않습니다.',
    );
  }

  const isValid = blockingReasons.length === 0;
  const userMessage = isValid
    ? `시험일 ${examDate}은 허용됩니다. 사용 가능한 학습일 수는 ${availableDaysForNewLearning}일입니다.`
    : blockingReasons[0] ?? '시험일 검증에 실패했습니다.';

  return {
    isValid,
    blockingReasons,
    warnings,
    availableDaysForNewLearning,
    earliestAllowedExamDate,
    lastReviewDate,
    userMessage,
  };
}

/**
 * 검증용 단순 assert 함수.
 */
function assert(condition: unknown, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

/**
 * 실행 시점에 검증 예시를 바로 확인할 수 있도록 하는 경량 self-check.
 * 이 파일은 스케줄 생성 알고리즘을 포함하지 않는다. 단순히 유틸리티와 검증 규칙만 실행한다.
 */
export function runSelfChecks(): void {
  const baseToday = '2026-10-01' as LocalDate;

  // 1) 예시 1: 시험일이 너무 빠르므로 차단
  const blockedCase = validateExamDate('2026-10-01' as LocalDate, '2026-10-04' as LocalDate, {
    ...DEFAULT_SCHEDULER_CONFIG,
    finalReviewBufferDays: 1,
    minimumEffectiveStudyDays: 3,
  });
  assert(blockedCase.isValid === false, '예시 1은 차단되어야 합니다.');
  assert(
    blockedCase.blockingReasons.some((reason) => reason.includes('가장 빠른 허용 시험일')),
    '예시 1은 빠른 시험일 메시지를 포함해야 합니다.',
  );

  // 2) 예시 2: 시험일 2026-10-06은 허용
  const allowedCase = validateExamDate('2026-10-01' as LocalDate, '2026-10-06' as LocalDate, {
    ...DEFAULT_SCHEDULER_CONFIG,
    finalReviewBufferDays: 1,
    minimumEffectiveStudyDays: 3,
  });
  assert(allowedCase.isValid === true, '예시 2는 허용되어야 합니다.');
  assert(allowedCase.earliestAllowedExamDate === '2026-10-06' as LocalDate, '가장 빠른 허용 시험일은 2026-10-06이어야 합니다.');

  // 3) 예시 3: buffer 2일이면 최소 유효 창이 부족해 차단
  const blockedBufferCase = validateExamDate('2026-10-01' as LocalDate, '2026-10-06' as LocalDate, {
    ...DEFAULT_SCHEDULER_CONFIG,
    finalReviewBufferDays: 2,
    minimumEffectiveStudyDays: 3,
  });
  assert(blockedBufferCase.isValid === false, '예시 3은 차단되어야 합니다.');
  assert(
    blockedBufferCase.availableDaysForNewLearning === 2,
    'buffer 2일이면 availableDaysForNewLearning은 2여야 합니다.',
  );

  // 4) examDate <= todayDate는 차단
  const pastCase = validateExamDate('2026-10-01' as LocalDate, '2026-10-01' as LocalDate, {
    ...DEFAULT_SCHEDULER_CONFIG,
    finalReviewBufferDays: 1,
    minimumEffectiveStudyDays: 3,
  });
  assert(pastCase.isValid === false, '오늘 또는 과거 시험일은 차단되어야 합니다.');

  // 날짜 유틸리티 기본 확인
  assert(isValidLocalDate('2026-10-01') === true, '유효한 LocalDate는 true여야 합니다.');
  assert(isValidLocalDate('2026-02-29') === false, '불가능한 날짜는 false여야 합니다.');
  assert(daysBetween('2026-10-01' as LocalDate, '2026-10-02' as LocalDate) === 1, 'day difference는 1이어야 합니다.');
  assert(addDays('2026-10-01' as LocalDate, 3) === '2026-10-04' as LocalDate, 'addDays는 3일 후를 계산해야 합니다.');
  assert(
    getEarliestAllowedExamDate('2026-10-01' as LocalDate, 1, 3) === '2026-10-06' as LocalDate,
    '최초 허용 시험일 계산식은 2026-10-06이어야 합니다.',
  );

  console.log('review-domain validation checks passed');
}

if (process.argv[1] && /review-domain\.ts$/u.test(process.argv[1])) {
  runSelfChecks();
}
