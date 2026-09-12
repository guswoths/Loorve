/**
 * 이 모듈은 시험 준비형 분산 복습 앱을 위한 타입, 검증 유틸리티, LocalDate 계산 유틸리티,
 * 그리고 정규 분산 리뷰 일정 생성의 핵심 유틸리티를 정의한다.
 *
 * 핵심 원칙:
 * - 날짜는 LocalDate 문자열(YYYY-MM-DD)만 공개 API로 사용한다.
 * - 내부 달력 연산은 UTC 기반의 안전한 Date.UTC를 사용한다.
 * - 날짜 문자열의 형식과 실제 존재 여부를 엄격하게 검증한다.
 * - 일반 사용자 검증 실패는 throw 대신 ValidationResult 구조로 반환한다.
 * - 정규 리뷰는 같은 달력 날짜를 두 번 생성하지 않으며, 시험일과 버퍼 구간은 제외한다.
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
  originalScheduledDate?: LocalDate;
  reviewStartDate?: LocalDate;
  lastReviewDate?: LocalDate;
  status: ReviewStatus;
  priorityScore: number;
  estimatedReviewMinutes: number;
  recommendedMethod: string;
  notificationPlan: NotificationPlan;
  dueDaysBeforeExam: number;
  isFinalReview: boolean;
  rescheduleReason?: string;
  userMessage?: string;
  createdAt: LocalDate;
  updatedAt: LocalDate;
}

export type ReviewOutcome = 'EASY' | 'SUCCESS' | 'HARD' | 'FAILED';

export interface ReviewOutcomeEvent {
  recordId: string;
  reviewDate: LocalDate;
  outcome: ReviewOutcome;
  scheduleId?: string;
  completedScheduleId?: string;
  today?: LocalDate;
}

export interface OutcomeAdjustmentPolicy {
  allowSuccessDelay?: boolean;
  successDelayDays?: number;
  allowRecallCheck?: boolean;
  maxInsertedReviews?: number;
}

export interface RescheduleAfterOutcomeInput {
  record: StudyRecord;
  exam: Exam;
  schedules: ReviewSchedule[];
  completedReviewDate: LocalDate;
  outcome: ReviewOutcome;
  today?: LocalDate;
  config?: Partial<SchedulerConfig>;
  reviewStartDate?: LocalDate;
  lastReviewDate?: LocalDate;
  policy?: OutcomeAdjustmentPolicy;
}

export interface RescheduleAfterOutcomeResult {
  schedules: ReviewSchedule[];
  changed: boolean;
  extraReviewInserted: boolean;
  insertedScheduleId?: string;
  cramModeRequired: boolean;
  warningCodes: Array<'CRAM_MODE_REQUIRED' | 'NO_VALID_SLOT' | 'INVALID_INPUT' | 'FINAL_REVIEW_PROTECTED' | 'NO_FUTURE_SCHEDULE'>;
  message: string;
  validation: {
    isValid: boolean;
    errors: string[];
    warnings: string[];
  };
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
  schedules: ReviewSchedule[];
  schedule?: ReviewSchedule[];
  warnings: SchedulingWarning[];
  unresolvedOverloads: UnresolvedOverload[];
  dailyLoadByDate: Map<LocalDate, number>;
  generatedAt: LocalDate;
  summary: {
    totalStudyRecords: number;
    totalSchedules: number;
    totalReviewMinutes: number;
    overloadedDateCount: number;
    cramModeRecordCount: number;
    insufficientWindowRecordCount: number;
  };
  notes: string[];
}

export interface PriorityScoringConfig {
  urgencyBase: number;
  importance: Record<Importance, number>;
  difficulty: Record<Difficulty, number>;
  mastery: Record<1 | 2 | 3 | 4 | 5, number>;
  finalReviewBonus: number;
  firstReviewBonus: number;
  overdueBonus: number;
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
  priorityScoring?: Partial<PriorityScoringConfig>;
}

export interface SchedulingWarning {
  affectedDate: LocalDate;
  scheduleIds: string[];
  recordIds: string[];
  excessMinutes?: number;
  reason: string;
  message: string;
  suggestedActions: string[];
}

export interface UnresolvedOverload extends SchedulingWarning {
  status: 'OVERLOADED_UNRESOLVED';
}

export const BASE_INTERVALS = [1, 3, 7, 14, 30, 60, 120] as const;

export const DEFAULT_SCHEDULER_CONFIG: SchedulerConfig = {
  timezone: 'Asia/Seoul',
  finalReviewBufferDays: 1,
  minimumEffectiveStudyDays: 3,
  maxDailyReviewMinutes: 120,
  defaultNotificationHour: 9,
  reminderNotificationHour: 19,
  allowRegularReviewOnDayBeforeExam: false,
  examValidationPolicy: 'BLOCK_IF_ANY_RECORD_INSUFFICIENT',
  baseIntervals: BASE_INTERVALS,
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

export interface TargetReviewCountResult {
  systemMinimumReviewCount: number;
  optionalMinimumContribution: number;
  difficultyAdjustment: number;
  masteryAdjustment: number;
  extraAdjustmentCap: number;
  requestedTargetCount: number;
  notes: string[];
}

export interface ScheduleGenerationWarning {
  code: 'INSUFFICIENT_WINDOW' | 'CRAM_MODE_REQUIRED' | 'DUPLICATE_AVOIDED' | 'BUFFER_EXCLUDED';
  message: string;
}

export type ScheduleGenerationStatus = ReviewStatus;

export interface GenerateReviewDatesInput {
  record: StudyRecord;
  examDate: LocalDate;
  finalReviewBufferDays?: number;
  config?: Partial<SchedulerConfig>;
}

export interface GenerateReviewDatesResult {
  generatedDates: LocalDate[];
  requestedTargetCount: number;
  actualGeneratedCount: number;
  reviewStartDate: LocalDate;
  lastReviewDate: LocalDate;
  effectiveStudyDays: number;
  usableWindowDays: number;
  status: ScheduleGenerationStatus[];
  warnings: ScheduleGenerationWarning[];
}

/**
 * "Ebbinghaus-style"은 사용자 이해를 돕기 위한 제품용 라벨이며,
 * 이 알고리즘은 각 개인의 실제 망각 곡선을 계산하려는 의도가 아니다.
 */
export function getMinimumReviewCount(effectiveStudyDays: number): number {
  if (!Number.isInteger(effectiveStudyDays) || effectiveStudyDays < 0) {
    throw new RangeError('effectiveStudyDays는 0 이상 정수여야 합니다.');
  }

  if (effectiveStudyDays < 3) return 0;
  if (effectiveStudyDays <= 6) return 2;
  if (effectiveStudyDays <= 13) return 3;
  if (effectiveStudyDays <= 29) return 4;
  if (effectiveStudyDays <= 59) return 5;
  if (effectiveStudyDays <= 119) return 6;
  return 7;
}

export function getTargetReviewCount(
  record: StudyRecord,
  effectiveStudyDays: number,
): TargetReviewCountResult {
  const systemMinimumReviewCount = getMinimumReviewCount(effectiveStudyDays);
  const optionalMinimumContribution = record.optionalMinReviewCount ?? 0;

  let difficultyAdjustment = 0;
  if (record.difficulty === 'hard') {
    difficultyAdjustment = 1;
  }

  let masteryAdjustment = 0;
  if (record.initialMastery === 1 || record.initialMastery === 2) {
    masteryAdjustment = 1;
  }

  const extraAdjustmentCap = Math.min(2, difficultyAdjustment + masteryAdjustment);
  const requestedTargetCount = Math.max(systemMinimumReviewCount, optionalMinimumContribution) + extraAdjustmentCap;

  return {
    systemMinimumReviewCount,
    optionalMinimumContribution,
    difficultyAdjustment,
    masteryAdjustment,
    extraAdjustmentCap,
    requestedTargetCount,
    notes: [
      `system minimum=${systemMinimumReviewCount}`,
      `optional minimum=${optionalMinimumContribution}`,
      `difficulty adjustment=${difficultyAdjustment}`,
      `mastery adjustment=${masteryAdjustment}`,
    ],
  };
}

export function ensureStrictlyIncreasingUniqueDates(
  dates: LocalDate[],
  reviewStartDate: LocalDate,
  lastReviewDate: LocalDate,
): LocalDate[] {
  const ordered = [...dates]
    .filter((value) => isValidLocalDate(value))
    .sort((a, b) => compareLocalDates(a, b));

  const unique: LocalDate[] = [];
  const seen = new Set<string>();

  for (const date of ordered) {
    if (date < reviewStartDate || date > lastReviewDate) {
      continue;
    }
    if (seen.has(date)) {
      continue;
    }
    seen.add(date);
    unique.push(date);
  }

  const corrected: LocalDate[] = [];
  for (const date of unique) {
    if (corrected.length === 0) {
      corrected.push(date);
      continue;
    }
    const previous = corrected[corrected.length - 1];
    if (daysBetween(previous, date) >= 1) {
      corrected.push(date);
    }
  }

  return corrected;
}

export function createRecommendedMethod(
  reviewIndex: number,
  totalReviewCount: number,
  isFinalReview: boolean,
): string {
  if (isFinalReview) {
    return '최종 점검: 오답 재검토, 약점 영역 우선 복습, 시험형 회상';
  }
  if (reviewIndex === 0) {
    return '짧은 회상 + 핵심 개념 재확인';
  }
  if (reviewIndex >= totalReviewCount - 1) {
    return '퀴즈 + 카드 복습 + 약점 정리';
  }
  return '짧은 퀴즈 + 오답 설명 + 핵심 단어 재호출';
}

export function createNotificationPlan(
  scheduledDate: LocalDate,
  isFinalReview: boolean,
  config: Partial<SchedulerConfig> = {},
): NotificationPlan {
  const effective = createDefaultSchedulerConfig(config);
  const hour = isFinalReview
    ? effective.reminderNotificationHour
    : effective.defaultNotificationHour;

  return {
    kind: 'IN_APP',
    hour,
    reminder: isFinalReview || false,
  };
}

export function calculateDueDaysBeforeExam(
  scheduledDate: LocalDate,
  examDate: LocalDate,
): number {
  return daysBetween(scheduledDate, examDate);
}

export function getScheduleStatus(input: {
  effectiveStudyDays: number;
  requestedTargetCount: number;
  actualGeneratedCount: number;
  generatedDates: LocalDate[];
  reviewStartDate: LocalDate;
  lastReviewDate: LocalDate;
}): ScheduleGenerationStatus[] {
  if (input.effectiveStudyDays < 3) {
    if (input.generatedDates.length > 0) {
      return ['CRAM_MODE_REQUIRED', 'INSUFFICIENT_WINDOW'];
    }
    return ['INSUFFICIENT_WINDOW'];
  }

  if (input.generatedDates.length === 0) {
    return ['INSUFFICIENT_WINDOW'];
  }

  if (input.actualGeneratedCount < input.requestedTargetCount) {
    return ['SCHEDULED', 'INSUFFICIENT_WINDOW'];
  }

  return ['SCHEDULED'];
}

export function generateScaledReviewDates(
  input: GenerateReviewDatesInput,
): GenerateReviewDatesResult {
  const record = input.record;
  const config = createDefaultSchedulerConfig(input.config ?? {});
  const finalReviewBufferDays =
    typeof input.finalReviewBufferDays === 'number'
      ? input.finalReviewBufferDays
      : config.finalReviewBufferDays;

  const reviewStartDate = getReviewStartDate(record.studiedDate);
  const lastReviewDate = getLastReviewDate(input.examDate, finalReviewBufferDays);
  const effectiveStudyDays = getEffectiveStudyDays(record.studiedDate, input.examDate, finalReviewBufferDays);
  const usableWindowDays = Math.max(
    0,
    daysBetween(reviewStartDate, lastReviewDate) + 1,
  );

  const targetResult = getTargetReviewCount(record, effectiveStudyDays);
  const requestedTargetCount = targetResult.requestedTargetCount;

  const warnings: ScheduleGenerationWarning[] = [];
  let status: ScheduleGenerationStatus[] = [];

  let effectiveRequestedCount = Math.max(0, Math.min(requestedTargetCount, usableWindowDays));

  if (effectiveStudyDays < 3) {
    effectiveRequestedCount = Math.min(1, usableWindowDays);
    status = ['CRAM_MODE_REQUIRED', 'INSUFFICIENT_WINDOW'];
    if (effectiveRequestedCount > 0) {
      warnings.push({
        code: 'CRAM_MODE_REQUIRED',
        message: '정상적인 분산 복습은 불가하며, 짧은 회상/퀴즈 중심으로 점검이 필요합니다.',
      });
    } else {
      warnings.push({
        code: 'INSUFFICIENT_WINDOW',
        message: '학습 창이 너무 짧아 자동 복습을 생성하지 않았습니다.',
      });
    }
  } else if (requestedTargetCount > usableWindowDays) {
    warnings.push({
      code: 'INSUFFICIENT_WINDOW',
      message: '요청된 복습 수가 사용 가능한 날짜를 초과해 가능한 최대 일수만 생성했습니다.',
    });
    status = ['SCHEDULED', 'INSUFFICIENT_WINDOW'];
  } else {
    status = ['SCHEDULED'];
  }

  const dateSpan: LocalDate[] = [];
  let cursor = reviewStartDate;
  while (!isAfter(cursor, lastReviewDate)) {
    dateSpan.push(cursor);
    cursor = addDays(cursor, 1);
  }

  let generatedDates: LocalDate[] = [];

  if (effectiveStudyDays < 3) {
    if (dateSpan.length > 0) {
      generatedDates = [dateSpan[0]];
    }
  } else if (effectiveRequestedCount === 1) {
    generatedDates = [reviewStartDate];
  } else if (effectiveRequestedCount >= 2) {
    const indices = new Set<number>();
    indices.add(0);
    indices.add(dateSpan.length - 1);
    for (let i = 1; i < effectiveRequestedCount - 1; i += 1) {
      const relative = i / (effectiveRequestedCount - 1);
      const index = Math.round(relative * (dateSpan.length - 1));
      if (index > 0 && index < dateSpan.length - 1) {
        indices.add(index);
      }
    }

    const candidateDates = Array.from(indices)
      .map((index) => dateSpan[index])
      .filter(Boolean);

    generatedDates = ensureStrictlyIncreasingUniqueDates(
      candidateDates,
      reviewStartDate,
      lastReviewDate,
    );

    if (generatedDates.length < effectiveRequestedCount) {
      for (const date of dateSpan) {
        if (generatedDates.length >= effectiveRequestedCount) break;
        if (!generatedDates.includes(date)) {
          generatedDates.push(date);
        }
      }
      generatedDates = ensureStrictlyIncreasingUniqueDates(
        generatedDates,
        reviewStartDate,
        lastReviewDate,
      );
    }

    if (generatedDates.length > effectiveRequestedCount) {
      generatedDates = generatedDates.slice(0, effectiveRequestedCount);
    }
  }

  if (generatedDates.length === 0 && dateSpan.length > 0) {
    generatedDates = [dateSpan[0]];
    warnings.push({
      code: 'INSUFFICIENT_WINDOW',
      message: '법적으로 유효한 날짜가 없어 최소한의 대체 날짜만 생성했습니다.',
    });
  }

  const finalStatus = getScheduleStatus({
    effectiveStudyDays,
    requestedTargetCount,
    actualGeneratedCount: generatedDates.length,
    generatedDates,
    reviewStartDate,
    lastReviewDate,
  });

  return {
    generatedDates: ensureStrictlyIncreasingUniqueDates(
      generatedDates,
      reviewStartDate,
      lastReviewDate,
    ),
    requestedTargetCount,
    actualGeneratedCount: ensureStrictlyIncreasingUniqueDates(
      generatedDates,
      reviewStartDate,
      lastReviewDate,
    ).length,
    reviewStartDate,
    lastReviewDate,
    effectiveStudyDays,
    usableWindowDays,
    status: finalStatus,
    warnings,
  };
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
 * 문서의 명시적 예시와 `availableDaysForNewLearning` 계산식에 따라,
 * 최종 복습 버퍼일 이전의 마지막 일반 복습일은 `examDate - (buffer + 1)`이다.
 * 버퍼일과 시험일 자체는 자동 일반 복습 대상에서 제외된다.
 */
export function getLastReviewDate(examDate: LocalDate, finalReviewBufferDays: number): LocalDate {
  if (!Number.isInteger(finalReviewBufferDays) || finalReviewBufferDays < 0) {
    throw new RangeError('finalReviewBufferDays는 0 이상 정수여야 합니다.');
  }

  return subtractDays(examDate, finalReviewBufferDays + 1);
}

/**
 * 학습 시작 직후 첫 번째 가능한 복습 날짜이다. 기본값은 학습일 + 1이다.
 */
export function getReviewStartDate(studiedDate: LocalDate): LocalDate {
  return addDays(studiedDate, 1);
}

/**
 * 학습 기록의 유효 학습일 수를 계산한다.
 * 정책 문서와 검증 예시를 따라, 학습일 자체를 포함해 마지막 일반 복습 가능일까지의 일수 차이를 사용한다.
 */
export function getEffectiveStudyDays(
  studiedDate: LocalDate,
  examDate: LocalDate,
  finalReviewBufferDays: number,
): number {
  const lastReviewDate = getLastReviewDate(examDate, finalReviewBufferDays);
  return Math.max(0, daysBetween(studiedDate, lastReviewDate));
}

/**
 * 새 학습을 시작할 수 있는 사용 가능한 일수를 계산한다.
 * 오늘 이후부터 마지막 일반 복습 가능일까지의 유효 날짜 수를 의미한다.
 * "day after todayDate"를 포함하는 inclusive-date 의미를 적용한다.
 */
export function getAvailableDaysForNewLearning(
  todayDate: LocalDate,
  examDate: LocalDate,
  finalReviewBufferDays: number,
): number {
  const lastReviewDate = getLastReviewDate(examDate, finalReviewBufferDays);
  return Math.max(0, daysBetween(todayDate, lastReviewDate));
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

function compareSchedulePriority(a: ReviewSchedule, b: ReviewSchedule): number {
  if (a.priorityScore !== b.priorityScore) {
    return b.priorityScore - a.priorityScore;
  }

  if (a.isFinalReview !== b.isFinalReview) {
    return a.isFinalReview ? -1 : 1;
  }

  if (a.reviewIndex !== b.reviewIndex) {
    return a.reviewIndex - b.reviewIndex;
  }

  if (a.studyRecordId !== b.studyRecordId) {
    return a.studyRecordId.localeCompare(b.studyRecordId);
  }

  return a.id.localeCompare(b.id);
}

export function getDefaultPriorityScoringConfig(
  config: Partial<SchedulerConfig> = {},
): PriorityScoringConfig {
  const merged = createDefaultSchedulerConfig(config);
  const override = merged.priorityScoring ?? {};

  return {
    urgencyBase: 30,
    importance: {
      low: 5,
      normal: 15,
      high: 30,
    },
    difficulty: {
      easy: 5,
      medium: 10,
      hard: 20,
    },
    mastery: {
      1: 20,
      2: 15,
      3: 10,
      4: 5,
      5: 0,
    },
    finalReviewBonus: 25,
    firstReviewBonus: 5,
    overdueBonus: 0,
    ...override,
    importance: {
      low: override.importance?.low ?? 5,
      normal: override.importance?.normal ?? 15,
      high: override.importance?.high ?? 30,
    },
    difficulty: {
      easy: override.difficulty?.easy ?? 5,
      medium: override.difficulty?.medium ?? 10,
      hard: override.difficulty?.hard ?? 20,
    },
    mastery: {
      1: override.mastery?.[1] ?? 20,
      2: override.mastery?.[2] ?? 15,
      3: override.mastery?.[3] ?? 10,
      4: override.mastery?.[4] ?? 5,
      5: override.mastery?.[5] ?? 0,
    },
  };
}

export function calculatePriorityScore(
  schedule: ReviewSchedule,
  record: StudyRecord,
  exam: Exam,
  config: Partial<SchedulerConfig> = {},
): number {
  const effective = createDefaultSchedulerConfig(config);
  const scoring = getDefaultPriorityScoringConfig(effective);
  const dueDaysBeforeExam = Math.max(0, schedule.dueDaysBeforeExam);
  const urgency = Math.max(0, scoring.urgencyBase - dueDaysBeforeExam);
  const importance = scoring.importance[record.importance] ?? scoring.importance.normal;
  const difficulty = scoring.difficulty[record.difficulty] ?? scoring.difficulty.medium;
  const masteryScore = scoring.mastery[record.initialMastery] ?? scoring.mastery[3];

  let total = urgency + importance + difficulty + masteryScore + scoring.overdueBonus;
  if (schedule.isFinalReview) {
    total += scoring.finalReviewBonus;
  }
  if (schedule.reviewIndex === 0) {
    total += scoring.firstReviewBonus;
  }

  return total;
}

export function createReviewSchedules(
  records: StudyRecord[],
  exam: Exam,
  config: Partial<SchedulerConfig> = {},
): SchedulingResult {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const normalizedExam: Exam = {
    ...exam,
    finalReviewBufferDays:
      typeof exam.finalReviewBufferDays === 'number'
        ? exam.finalReviewBufferDays
        : effectiveConfig.finalReviewBufferDays,
    createdAt: exam.createdAt ?? exam.examDate,
    updatedAt: exam.updatedAt ?? exam.examDate,
  };

  if (!Array.isArray(records)) {
    throw new TypeError('records는 배열이어야 합니다.');
  }
  if (!exam || typeof exam !== 'object') {
    throw new TypeError('exam은 객체여야 합니다.');
  }

  const schedules: ReviewSchedule[] = [];
  let cramModeRecordCount = 0;
  let insufficientWindowRecordCount = 0;

  for (const record of records) {
    const generation = generateScaledReviewDates({
      record,
      examDate: normalizedExam.examDate,
      finalReviewBufferDays: normalizedExam.finalReviewBufferDays,
      config: effectiveConfig,
    });

    if (generation.status.includes('CRAM_MODE_REQUIRED')) {
      cramModeRecordCount += 1;
    }
    if (generation.status.includes('INSUFFICIENT_WINDOW')) {
      insufficientWindowRecordCount += 1;
    }

    for (let index = 0; index < generation.generatedDates.length; index += 1) {
      const scheduledDate = generation.generatedDates[index];
      const isFinalReview = scheduledDate === generation.lastReviewDate;
      const schedule: ReviewSchedule = {
        id: `${record.id}-${index + 1}`,
        studyRecordId: record.id,
        reviewIndex: index,
        scheduledDate,
        originalScheduledDate: scheduledDate,
        reviewStartDate: generation.reviewStartDate,
        lastReviewDate: generation.lastReviewDate,
        status: 'SCHEDULED',
        priorityScore: 0,
        estimatedReviewMinutes: record.estimatedReviewMinutes,
        recommendedMethod: createRecommendedMethod(index, generation.generatedDates.length, isFinalReview),
        notificationPlan: createNotificationPlan(scheduledDate, isFinalReview, effectiveConfig),
        dueDaysBeforeExam: calculateDueDaysBeforeExam(scheduledDate, normalizedExam.examDate),
        isFinalReview,
        createdAt: record.createdAt,
        updatedAt: record.updatedAt,
      };

      schedule.priorityScore = calculatePriorityScore(schedule, record, normalizedExam, effectiveConfig);
      schedules.push(schedule);
    }
  }

  const rebalance = rebalanceDailyLoad(schedules, normalizedExam, effectiveConfig);
  const dailyLoadByDate = calculateDailyLoad(rebalance.schedules);
  const totalReviewMinutes = rebalance.schedules.reduce((sum, schedule) => sum + schedule.estimatedReviewMinutes, 0);

  return {
    isValid: rebalance.unresolvedOverloads.length === 0,
    validation: {
      isValid: rebalance.unresolvedOverloads.length === 0,
      blockingReasons: rebalance.unresolvedOverloads.map((overload) => overload.message),
      warnings: rebalance.warnings.map((warning) => warning.message),
      availableDaysForNewLearning: 0,
      earliestAllowedExamDate: normalizedExam.examDate,
      lastReviewDate: getLastReviewDate(normalizedExam.examDate, normalizedExam.finalReviewBufferDays ?? effectiveConfig.finalReviewBufferDays),
      userMessage: rebalance.unresolvedOverloads.length === 0
        ? '모든 복습 일정이 허용 범위 안에 있습니다.'
        : rebalance.unresolvedOverloads[0]?.message ?? '일일 용량 제한을 충족하지 않는 일정이 남아 있습니다.',
    },
    schedules: rebalance.schedules,
    schedule: rebalance.schedules,
    warnings: rebalance.warnings,
    unresolvedOverloads: rebalance.unresolvedOverloads,
    dailyLoadByDate,
    generatedAt: normalizedExam.examDate,
    summary: {
      totalStudyRecords: records.length,
      totalSchedules: rebalance.schedules.length,
      totalReviewMinutes,
      overloadedDateCount: Array.from(dailyLoadByDate.entries()).filter(([, value]) => value > effectiveConfig.maxDailyReviewMinutes).length,
      cramModeRecordCount,
      insufficientWindowRecordCount,
    },
    notes: rebalance.unresolvedOverloads.length === 0
      ? ['정규 복습이 용량 제한을 모두 충족했습니다.']
      : ['일일 용량 초과가 남아 있어 일부 일정을 보류하고 경고를 남겼습니다.'],
  };
}

export function groupSchedulesByDate(
  schedules: ReviewSchedule[],
): Map<LocalDate, ReviewSchedule[]> {
  const grouped = new Map<LocalDate, ReviewSchedule[]>();
  const ordered = [...schedules].sort((a, b) => compareLocalDates(a.scheduledDate, b.scheduledDate));

  for (const schedule of ordered) {
    const bucket = grouped.get(schedule.scheduledDate) ?? [];
    bucket.push(schedule);
    grouped.set(schedule.scheduledDate, bucket);
  }

  return grouped;
}

export function calculateDailyLoad(
  schedules: ReviewSchedule[],
): Map<LocalDate, number> {
  const dailyLoad = new Map<LocalDate, number>();

  for (const schedule of schedules) {
    if (!Number.isFinite(schedule.estimatedReviewMinutes) || schedule.estimatedReviewMinutes < 0) {
      throw new RangeError(`estimatedReviewMinutes는 유한하고 0 이상이어야 합니다: ${schedule.id}`);
    }

    const current = dailyLoad.get(schedule.scheduledDate) ?? 0;
    dailyLoad.set(schedule.scheduledDate, current + schedule.estimatedReviewMinutes);
  }

  return dailyLoad;
}

export function findRescheduleCandidateDate(
  schedule: ReviewSchedule,
  schedules: ReviewSchedule[],
  exam: Exam,
  config: Partial<SchedulerConfig> = {},
): LocalDate | null {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  if (schedule.isFinalReview) {
    return null;
  }

  const offsets = [-1, 1, -2, 2] as const;
  for (const offset of offsets) {
    const candidateDate = addDays(schedule.scheduledDate, offset);
    if (canMoveScheduleToDate(schedule, candidateDate, schedules, exam, effectiveConfig)) {
      return candidateDate;
    }
  }

  return null;
}

export function canMoveScheduleToDate(
  schedule: ReviewSchedule,
  candidateDate: LocalDate,
  schedules: ReviewSchedule[],
  exam: Exam,
  config: Partial<SchedulerConfig> = {},
): boolean {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const finalBufferDays = Number.isInteger(exam.finalReviewBufferDays)
    ? exam.finalReviewBufferDays ?? effectiveConfig.finalReviewBufferDays
    : effectiveConfig.finalReviewBufferDays;
  const lastReviewDate = getLastReviewDate(exam.examDate, finalBufferDays);

  if (schedule.isFinalReview) {
    return false;
  }
  if (!isValidLocalDate(candidateDate)) {
    return false;
  }
  if (schedule.reviewStartDate && candidateDate < schedule.reviewStartDate) {
    return false;
  }
  if (schedule.lastReviewDate && candidateDate > schedule.lastReviewDate) {
    return false;
  }
  if (candidateDate >= exam.examDate) {
    return false;
  }
  if (candidateDate >= addDays(exam.examDate, -finalBufferDays) && candidateDate <= exam.examDate) {
    return false;
  }
  if (candidateDate > lastReviewDate) {
    return false;
  }

  const loadMap = calculateDailyLoad(schedules);
  const sourceDateLoad = loadMap.get(schedule.scheduledDate) ?? 0;
  const targetDateLoad = loadMap.get(candidateDate) ?? 0;
  if (targetDateLoad + schedule.estimatedReviewMinutes > effectiveConfig.maxDailyReviewMinutes) {
    return false;
  }
  if (sourceDateLoad <= effectiveConfig.maxDailyReviewMinutes) {
    return false;
  }

  for (const other of schedules) {
    if (other.id === schedule.id) {
      continue;
    }
    if (other.studyRecordId !== schedule.studyRecordId) {
      continue;
    }
    if (other.scheduledDate === candidateDate) {
      return false;
    }
    const gap = Math.abs(daysBetween(other.scheduledDate, candidateDate));
    if (gap <= 1) {
      return false;
    }
  }

  return true;
}

export function buildOverloadWarning(
  date: LocalDate,
  affectedSchedules: ReviewSchedule[],
  reason: string,
  config: Partial<SchedulerConfig> = {},
): SchedulingWarning {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const totalMinutes = affectedSchedules.reduce((sum, schedule) => sum + schedule.estimatedReviewMinutes, 0);
  const excessMinutes = Math.max(0, totalMinutes - effectiveConfig.maxDailyReviewMinutes);
  const suggestions = [
    '시험일을 조정해 마지막 복습 마감 시점을 늦추세요.',
    '일일 최대 복습 분 제한을 늘리세요.',
    '과목을 병합하거나 더 작은 학습 단위로 분리하세요.',
    '수동으로 일정을 조정하세요.',
  ];

  return {
    affectedDate: date,
    scheduleIds: affectedSchedules.map((schedule) => schedule.id),
    recordIds: affectedSchedules.map((schedule) => schedule.studyRecordId),
    excessMinutes,
    reason,
    message: `${date}의 일일 복습량이 ${totalMinutes}분으로 제한 ${effectiveConfig.maxDailyReviewMinutes}분을 ${excessMinutes}분 초과했습니다.`,
    suggestedActions: suggestions,
  };
}

function cloneSchedule(schedule: ReviewSchedule): ReviewSchedule {
  return {
    ...schedule,
    notificationPlan: { ...schedule.notificationPlan },
  };
}

function isProtectedFinalReview(schedule: ReviewSchedule): boolean {
  return schedule.isFinalReview === true;
}

function isSameRecordScheduleDateAllowed(
  candidateDate: LocalDate,
  recordId: string,
  schedules: ReviewSchedule[],
  exam: Exam,
  finalBufferDays: number,
  excludedScheduleId?: string,
): boolean {
  if (!isValidLocalDate(candidateDate)) {
    return false;
  }
  if (candidateDate === exam.examDate) {
    return false;
  }
  const bufferStart = addDays(exam.examDate, -finalBufferDays);
  if (candidateDate >= bufferStart && candidateDate < exam.examDate) {
    return false;
  }

  for (const other of schedules) {
    if (other.studyRecordId !== recordId) {
      continue;
    }
    if (excludedScheduleId && other.id === excludedScheduleId) {
      continue;
    }
    if (other.scheduledDate === candidateDate) {
      return false;
    }
    if (Math.abs(daysBetween(other.scheduledDate, candidateDate)) <= 1) {
      return false;
    }
  }

  return true;
}

function getReviewWindowDates(
  reviewStartDate: LocalDate,
  lastReviewDate: LocalDate,
): LocalDate[] {
  const dates: LocalDate[] = [];
  let cursor = reviewStartDate;
  while (!isAfter(cursor, lastReviewDate)) {
    dates.push(cursor);
    cursor = addDays(cursor, 1);
  }
  return dates;
}

export function getFutureSchedulesForRecord(
  recordId: string,
  schedules: ReviewSchedule[],
  completedReviewDate: LocalDate,
): ReviewSchedule[] {
  return [...schedules]
    .filter((schedule) => schedule.studyRecordId === recordId)
    .filter((schedule) => schedule.scheduledDate > completedReviewDate)
    .filter((schedule) => schedule.status !== 'COMPLETED' && schedule.status !== 'MISSED')
    .sort((a, b) => compareLocalDates(a.scheduledDate, b.scheduledDate));
}

export function findEarliestValidFutureDate(
  recordId: string,
  schedules: ReviewSchedule[],
  afterDate: LocalDate,
  exam: Exam,
  reviewStartDate: LocalDate,
  lastReviewDate: LocalDate,
  config: Partial<SchedulerConfig> = {},
  excludedScheduleId?: string,
): LocalDate | null {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const finalBufferDays = Number.isInteger(exam.finalReviewBufferDays)
    ? exam.finalReviewBufferDays ?? effectiveConfig.finalReviewBufferDays
    : effectiveConfig.finalReviewBufferDays;

  const candidateDates = getReviewWindowDates(reviewStartDate, lastReviewDate)
    .filter((date) => date > afterDate)
    .filter((date) => isSameRecordScheduleDateAllowed(date, recordId, schedules, exam, finalBufferDays, excludedScheduleId));

  return candidateDates[0] ?? null;
}

export function findLatestValidFutureDate(
  recordId: string,
  schedules: ReviewSchedule[],
  beforeDate: LocalDate,
  exam: Exam,
  reviewStartDate: LocalDate,
  lastReviewDate: LocalDate,
  config: Partial<SchedulerConfig> = {},
  excludedScheduleId?: string,
): LocalDate | null {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const finalBufferDays = Number.isInteger(exam.finalReviewBufferDays)
    ? exam.finalReviewBufferDays ?? effectiveConfig.finalReviewBufferDays
    : effectiveConfig.finalReviewBufferDays;

  const candidateDates = getReviewWindowDates(reviewStartDate, lastReviewDate)
    .filter((date) => date < beforeDate)
    .filter((date) => isSameRecordScheduleDateAllowed(date, recordId, schedules, exam, finalBufferDays, excludedScheduleId));

  return candidateDates[candidateDates.length - 1] ?? null;
}

export function preserveFinalReview(
  schedules: ReviewSchedule[],
  exam: Exam,
  config: Partial<SchedulerConfig> = {},
): { schedules: ReviewSchedule[]; changed: boolean; warning?: string } {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const finalBufferDays = Number.isInteger(exam.finalReviewBufferDays)
    ? exam.finalReviewBufferDays ?? effectiveConfig.finalReviewBufferDays
    : effectiveConfig.finalReviewBufferDays;

  let lastReviewDate: LocalDate;
  try {
    lastReviewDate = getLastReviewDate(exam.examDate, finalBufferDays);
  } catch {
    return {
      schedules: [...schedules],
      changed: false,
      warning: '유효하지 않은 시험 설정으로 최종 복습 날짜를 보존할 수 없습니다.',
    };
  }

  let changed = false;
  const output = schedules.map((schedule) => cloneSchedule(schedule));
  const finalSchedules = output.filter((schedule) => schedule.isFinalReview === true);
  if (finalSchedules.length > 1) {
    return {
      schedules: output,
      changed: false,
      warning: '최종 복습 항목이 둘 이상 있어 보호 규칙을 유지할 수 없습니다.',
    };
  }

  for (const schedule of output) {
    if (schedule.isFinalReview && schedule.scheduledDate !== lastReviewDate) {
      schedule.scheduledDate = lastReviewDate;
      schedule.rescheduleReason = `Final review protected at ${lastReviewDate}`;
      schedule.userMessage = `최종 복습은 ${lastReviewDate}에 유지됩니다.`;
      changed = true;
    }
  }

  return { schedules: output, changed, warning: changed ? undefined : undefined };
}

export function validateRecordScheduleInvariants(
  recordId: string,
  schedules: ReviewSchedule[],
  exam: Exam,
  reviewStartDate: LocalDate,
  lastReviewDate: LocalDate,
  config: Partial<SchedulerConfig> = {},
): { isValid: boolean; errors: string[]; warnings: string[] } {
  const errors: string[] = [];
  const warnings: string[] = [];
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const finalBufferDays = Number.isInteger(exam.finalReviewBufferDays)
    ? exam.finalReviewBufferDays ?? effectiveConfig.finalReviewBufferDays
    : effectiveConfig.finalReviewBufferDays;

  const recordSchedules = schedules
    .filter((schedule) => schedule.studyRecordId === recordId)
    .sort((a, b) => compareLocalDates(a.scheduledDate, b.scheduledDate));

  for (let index = 0; index < recordSchedules.length; index += 1) {
    const schedule = recordSchedules[index];
    if (schedule.status === 'COMPLETED' || schedule.scheduledDate < reviewStartDate || schedule.scheduledDate > lastReviewDate) {
      if (schedule.status !== 'COMPLETED') {
        errors.push(`${schedule.id}는 유효한 리뷰 창 밖에 있습니다.`);
      }
    }
    if (schedule.scheduledDate === exam.examDate) {
      errors.push(`${schedule.id}는 시험일에 배치될 수 없습니다.`);
    }
    const bufferStart = addDays(exam.examDate, -finalBufferDays);
    if (schedule.scheduledDate >= bufferStart && schedule.scheduledDate < exam.examDate) {
      errors.push(`${schedule.id}는 시험 전 버퍼 기간에 배치될 수 없습니다.`);
    }
    if (schedule.isFinalReview && schedule.scheduledDate !== lastReviewDate) {
      errors.push(`${schedule.id}는 최종 복습일 ${lastReviewDate}에 유지되어야 합니다.`);
    }
  }

  for (let index = 1; index < recordSchedules.length; index += 1) {
    const previous = recordSchedules[index - 1];
    const current = recordSchedules[index];
    if (Math.abs(daysBetween(previous.scheduledDate, current.scheduledDate)) <= 1) {
      errors.push(`${previous.id}와 ${current.id} 사이에 최소 1일 간격이 필요합니다.`);
    }
  }

  const seen = new Set<LocalDate>();
  for (const schedule of recordSchedules) {
    if (seen.has(schedule.scheduledDate)) {
      errors.push(`${schedule.id}는 같은 날짜에 중복 배치될 수 없습니다.`);
    }
    seen.add(schedule.scheduledDate);
  }

  if (!recordSchedules.every((schedule, index, list) => index === 0 || compareLocalDates(list[index - 1].scheduledDate, schedule.scheduledDate) < 0)) {
    errors.push('기록별 일정은 오름차순으로 정렬되어야 합니다.');
  }

  const finalItems = recordSchedules.filter((schedule) => schedule.isFinalReview);
  if (finalItems.length > 1) {
    errors.push('최종 복습 일정은 단 하나여야 합니다.');
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

export function insertAdditionalReviewIfPossible(
  recordId: string,
  schedules: ReviewSchedule[],
  completedReviewDate: LocalDate,
  exam: Exam,
  reviewStartDate: LocalDate,
  lastReviewDate: LocalDate,
  config: Partial<SchedulerConfig> = {},
): {
  inserted: boolean;
  schedule?: ReviewSchedule;
  schedules: ReviewSchedule[];
  reason?: string;
  warning?: string;
} {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const finalBufferDays = Number.isInteger(exam.finalReviewBufferDays)
    ? exam.finalReviewBufferDays ?? effectiveConfig.finalReviewBufferDays
    : effectiveConfig.finalReviewBufferDays;

  const candidateDates = getReviewWindowDates(reviewStartDate, lastReviewDate)
    .filter((date) => date > completedReviewDate)
    .filter((date) => isSameRecordScheduleDateAllowed(date, recordId, schedules, exam, finalBufferDays, undefined));

  if (candidateDates.length === 0) {
    return {
      inserted: false,
      schedules: [...schedules],
      reason: 'NO_VALID_SLOT',
      warning: '추가 복습을 넣을 수 있는 유효한 날짜가 없습니다.',
    };
  }

  const existing = schedules.filter((schedule) => schedule.studyRecordId === recordId);
  const nextIndex = Math.max(0, ...existing.map((schedule) => schedule.reviewIndex)) + 1;
  const slotDate = candidateDates[0];
  const insertedSchedule: ReviewSchedule = {
    id: `${recordId}-recall-check`,
    studyRecordId: recordId,
    reviewIndex: nextIndex,
    scheduledDate: slotDate,
    originalScheduledDate: slotDate,
    reviewStartDate,
    lastReviewDate,
    status: 'SCHEDULED',
    priorityScore: 0,
    estimatedReviewMinutes: 5,
    recommendedMethod: '짧은 회상 점검',
    notificationPlan: { kind: 'IN_APP', hour: effectiveConfig.defaultNotificationHour, reminder: false },
    dueDaysBeforeExam: calculateDueDaysBeforeExam(slotDate, exam.examDate),
    isFinalReview: false,
    rescheduleReason: 'Inserted recall check',
    userMessage: '짧은 회상 점검을 추가했습니다.',
    createdAt: completedReviewDate,
    updatedAt: completedReviewDate,
  };

  return {
    inserted: true,
    schedule: insertedSchedule,
    schedules: [...schedules, insertedSchedule],
    reason: 'INSERTED',
  };
}

export function buildOutcomeFeedbackMessage(
  outcome: ReviewOutcome,
  changed: boolean,
  extraReviewInserted: boolean,
  cramModeRequired: boolean,
  reason?: string,
): string {
  if (cramModeRequired) {
    return '복습을 더 앞당기거나 추가할 유효한 날짜가 부족해 크램 모드 상태로 유지하고 있습니다. 시험일이나 버퍼 범위를 건드리지 않고, 짧은 회상 중심으로 점검해 주세요.';
  }

  if (!changed && !extraReviewInserted) {
    if (outcome === 'SUCCESS') {
      return '다음 복습 일정은 유지했습니다. 현재 일정은 충분히 안정적으로 보입니다.';
    }
    if (outcome === 'EASY') {
      return '유효한 뒤로 미루기 슬롯이 없어 기존 일정을 유지했습니다.';
    }
    if (outcome === 'HARD') {
      return '더 빠른 복습이 가능한 자리 없이 기존 일정을 유지했습니다.';
    }
    return '유효한 조정 가능성이 없어 기존 일정에 그대로 두었습니다.';
  }

  if (extraReviewInserted) {
    return '짧은 회상 점검을 추가했고, 필요한 경우 가까운 미래 복습 일정을 조정했습니다.';
  }

  if (outcome === 'EASY') {
    return '가장 가까운 미래 복습 일정을 약간 뒤로 조정했습니다.';
  }
  if (outcome === 'HARD') {
    return '가장 가까운 미래 복습을 조금 앞당겼습니다.';
  }
  if (outcome === 'FAILED') {
    return '실패로 간주된 복습을 빠르게 보강할 수 있도록 가장 가까운 미래 일정을 앞당겼습니다.';
  }
  return `결과 ${outcome}에 따라 일정을 조정했습니다.`;
}

export function rescheduleAfterReviewOutcome(
  input: RescheduleAfterOutcomeInput,
): RescheduleAfterOutcomeResult {
  const validationErrors: string[] = [];
  const validationWarnings: string[] = [];
  const effectiveConfig = createDefaultSchedulerConfig(input.config ?? {});

  if (!input || typeof input !== 'object') {
    return {
      schedules: [],
      changed: false,
      extraReviewInserted: false,
      cramModeRequired: false,
      warningCodes: ['INVALID_INPUT'],
      message: '입력 값이 올바르지 않아 일정 재조정을 수행하지 않았습니다.',
      validation: {
        isValid: false,
        errors: ['입력 객체가 비어 있습니다.'],
        warnings: [],
      },
    };
  }

  if (!Array.isArray(input.schedules)) {
    validationErrors.push('schedules는 배열이어야 합니다.');
  }
  if (!input.record || typeof input.record !== 'object') {
    validationErrors.push('record 객체가 필요합니다.');
  }
  if (!input.exam || typeof input.exam !== 'object') {
    validationErrors.push('exam 객체가 필요합니다.');
  }

  if (validationErrors.length > 0) {
    return {
      schedules: Array.isArray(input.schedules) ? [...input.schedules] : [],
      changed: false,
      extraReviewInserted: false,
      cramModeRequired: false,
      warningCodes: ['INVALID_INPUT'],
      message: '입력 검증에 실패해 일정을 조정하지 않았습니다.',
      validation: {
        isValid: false,
        errors: validationErrors,
        warnings: validationWarnings,
      },
    };
  }

  const record = input.record as StudyRecord;
  const exam = input.exam as Exam;
  const schedules = input.schedules.map((schedule) => cloneSchedule(schedule));
  const finalBufferDays = Number.isInteger(exam.finalReviewBufferDays)
    ? exam.finalReviewBufferDays ?? effectiveConfig.finalReviewBufferDays
    : effectiveConfig.finalReviewBufferDays;

  const recordSchedules = schedules.filter((schedule) => schedule.studyRecordId === record.id);
  const reviewStartDate = input.reviewStartDate ?? getReviewStartDate(record.studiedDate);
  const lastReviewDate = input.lastReviewDate ?? getLastReviewDate(exam.examDate, finalBufferDays);

  const invariantCheck = validateRecordScheduleInvariants(
    record.id,
    schedules,
    exam,
    reviewStartDate,
    lastReviewDate,
    input.config ?? {},
  );
  if (!invariantCheck.isValid) {
    return {
      schedules,
      changed: false,
      extraReviewInserted: false,
      cramModeRequired: false,
      warningCodes: ['INVALID_INPUT'],
      message: invariantCheck.errors[0] ?? '기존 일정이 유효하지 않아 조정하지 않았습니다.',
      validation: {
        isValid: false,
        errors: invariantCheck.errors,
        warnings: invariantCheck.warnings,
      },
    };
  }

  let working = schedules.map((schedule) => cloneSchedule(schedule));
  const futureSchedules = getFutureSchedulesForRecord(record.id, working, input.completedReviewDate);

  if (futureSchedules.length === 0) {
    return {
      schedules: working,
      changed: false,
      extraReviewInserted: false,
      cramModeRequired: false,
      warningCodes: ['NO_FUTURE_SCHEDULE'],
      message: '완료한 리뷰 이후의 향후 복습 일정이 없어 추가 조정을 하지 않았습니다.',
      validation: {
        isValid: true,
        errors: [],
        warnings: [],
      },
    };
  }

  const target = futureSchedules[0];
  let changed = false;
  let extraReviewInserted = false;
  let cramModeRequired = false;
  let warningCodes: RescheduleAfterOutcomeResult['warningCodes'] = [];

  if (input.outcome === 'SUCCESS') {
    if (input.policy?.allowSuccessDelay === true) {
      const nextDate = findEarliestValidFutureDate(
        record.id,
        working,
        target.scheduledDate,
        exam,
        reviewStartDate,
        lastReviewDate,
        input.config ?? {},
        target.id,
      );
      const nextDay = nextDate ? addDays(target.scheduledDate, 1) : null;
      if (nextDay && isSameRecordScheduleDateAllowed(nextDay, record.id, working, exam, finalBufferDays, target.id)) {
        const idx = working.findIndex((schedule) => schedule.id === target.id);
        if (idx >= 0) {
          working[idx] = { ...working[idx], scheduledDate: nextDay, rescheduleReason: `SUCCESS relaxed reschedule: ${target.scheduledDate} → ${nextDay}`, status: 'RESCHEDULED', userMessage: '성공 결과에 따라 가까운 미래 복습을 한 날만 뒤로 미뤘습니다.', updatedAt: input.today ?? input.completedReviewDate };
          changed = true;
        }
      }
    }
  } else if (input.outcome === 'EASY') {
    const candidateDate = (() => {
      const base = target.scheduledDate;
      for (const offset of [1, 2]) {
        const candidate = addDays(base, offset);
        if (candidate <= lastReviewDate && candidate >= reviewStartDate && isSameRecordScheduleDateAllowed(candidate, record.id, working, exam, finalBufferDays, target.id)) {
          return candidate;
        }
      }
      return null;
    })();

    if (candidateDate) {
      const idx = working.findIndex((schedule) => schedule.id === target.id);
      if (idx >= 0) {
        working[idx] = { ...working[idx], scheduledDate: candidateDate, rescheduleReason: `EASY reschedule: ${target.scheduledDate} → ${candidateDate}`, status: 'RESCHEDULED', userMessage: 'EASY 결과에 따라 가장 가까운 미래 복습을 조금 늦췄습니다.', updatedAt: input.today ?? input.completedReviewDate };
        changed = true;
      }
    } else {
      warningCodes.push('NO_VALID_SLOT');
    }
  } else if (input.outcome === 'HARD') {
    const candidateDate = (() => {
      const candidate = addDays(target.scheduledDate, -1);
      if (candidate >= reviewStartDate && candidate <= lastReviewDate && candidate > input.completedReviewDate && isSameRecordScheduleDateAllowed(candidate, record.id, working, exam, finalBufferDays, target.id)) {
        return candidate;
      }
      return null;
    })();

    if (candidateDate) {
      const idx = working.findIndex((schedule) => schedule.id === target.id);
      if (idx >= 0) {
        working[idx] = { ...working[idx], scheduledDate: candidateDate, rescheduleReason: `HARD reschedule: ${target.scheduledDate} → ${candidateDate}`, status: 'RESCHEDULED', userMessage: 'HARD 결과에 따라 가장 가까운 미래 복습을 앞당겼습니다.', updatedAt: input.today ?? input.completedReviewDate };
        changed = true;
      }
    }

    if ((input.policy?.allowRecallCheck ?? true) && !target.isFinalReview) {
      const inserted = insertAdditionalReviewIfPossible(
        record.id,
        working,
        input.completedReviewDate,
        exam,
        reviewStartDate,
        lastReviewDate,
        input.config ?? {},
      );
      if (inserted.inserted) {
        working = inserted.schedules;
        extraReviewInserted = true;
      } else if (inserted.warning) {
        cramModeRequired = true;
        warningCodes.push('CRAM_MODE_REQUIRED');
      }
    }
  } else if (input.outcome === 'FAILED') {
    const earliest = findEarliestValidFutureDate(
      record.id,
      working,
      input.completedReviewDate,
      exam,
      reviewStartDate,
      lastReviewDate,
      input.config ?? {},
      target.id,
    );

    if (earliest && earliest !== target.scheduledDate) {
      const idx = working.findIndex((schedule) => schedule.id === target.id);
      if (idx >= 0) {
        working[idx] = { ...working[idx], scheduledDate: earliest, rescheduleReason: `FAILED reschedule: ${target.scheduledDate} → ${earliest}`, status: 'RESCHEDULED', userMessage: 'FAILED 결과에 따라 가장 빠른 유효한 날짜로 복습을 앞당겼습니다.', updatedAt: input.today ?? input.completedReviewDate };
        changed = true;
      }
    }

    if ((input.policy?.allowRecallCheck ?? true) && !target.isFinalReview) {
      const inserted = insertAdditionalReviewIfPossible(
        record.id,
        working,
        input.completedReviewDate,
        exam,
        reviewStartDate,
        lastReviewDate,
        input.config ?? {},
      );
      if (inserted.inserted) {
        working = inserted.schedules;
        extraReviewInserted = true;
      } else if (inserted.warning) {
        cramModeRequired = true;
        warningCodes.push('CRAM_MODE_REQUIRED');
      }
    }
  }

  const finalProtected = preserveFinalReview(working, exam, input.config ?? {});
  if (finalProtected.changed) {
    working = finalProtected.schedules;
    changed = true;
    warningCodes.push('FINAL_REVIEW_PROTECTED');
  }

  const invariantCheckAfter = validateRecordScheduleInvariants(
    record.id,
    working,
    exam,
    reviewStartDate,
    lastReviewDate,
    input.config ?? {},
  );

  if (!invariantCheckAfter.isValid) {
    return {
      schedules: working,
      changed,
      extraReviewInserted,
      cramModeRequired,
      warningCodes: warningCodes.length > 0 ? warningCodes : ['INVALID_INPUT'],
      message: invariantCheckAfter.errors[0] ?? '일정 무결성 검증에 실패해 조정을 롤백합니다.',
      validation: {
        isValid: false,
        errors: invariantCheckAfter.errors,
        warnings: invariantCheckAfter.warnings,
      },
    };
  }

  const message = buildOutcomeFeedbackMessage(
    input.outcome,
    changed,
    extraReviewInserted,
    cramModeRequired,
    warningCodes[0],
  );

  return {
    schedules: working,
    changed,
    extraReviewInserted,
    cramModeRequired,
    warningCodes,
    message,
    validation: {
      isValid: true,
      errors: [],
      warnings: invariantCheckAfter.warnings,
    },
  };
}

export function sortSchedulesForDailyView(
  schedules: ReviewSchedule[],
): ReviewSchedule[] {
  const copy = [...schedules];
  return copy.sort((a, b) => {
    const unresolvedOrder = Number(b.status === 'OVERLOADED_UNRESOLVED') - Number(a.status === 'OVERLOADED_UNRESOLVED');
    if (unresolvedOrder !== 0) {
      return unresolvedOrder;
    }

    if (a.priorityScore !== b.priorityScore) {
      return b.priorityScore - a.priorityScore;
    }

    if (a.isFinalReview !== b.isFinalReview) {
      return Number(b.isFinalReview) - Number(a.isFinalReview);
    }

    if (a.reviewIndex !== b.reviewIndex) {
      return a.reviewIndex - b.reviewIndex;
    }

    if (a.scheduledDate !== b.scheduledDate) {
      return compareLocalDates(a.scheduledDate, b.scheduledDate);
    }

    return a.id.localeCompare(b.id);
  });
}

export function rebalanceDailyLoad(
  schedules: ReviewSchedule[],
  exam: Exam,
  config: Partial<SchedulerConfig> = {},
): {
  schedules: ReviewSchedule[];
  warnings: SchedulingWarning[];
  unresolvedOverloads: UnresolvedOverload[];
} {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const working = schedules.map((schedule) => ({
    ...schedule,
    originalScheduledDate: schedule.originalScheduledDate ?? schedule.scheduledDate,
  }));
  const warnings: SchedulingWarning[] = [];
  const unresolvedOverloads: UnresolvedOverload[] = [];
  const movedScheduleIds = new Set<string>();

  for (let pass = 0; pass < working.length + 1; pass += 1) {
    const dailyLoad = calculateDailyLoad(working);
    const overloadedDates = Array.from(dailyLoad.entries())
      .filter(([, total]) => total > effectiveConfig.maxDailyReviewMinutes)
      .map(([date]) => date)
      .sort((a, b) => compareLocalDates(a, b));

    if (overloadedDates.length === 0) {
      break;
    }

    let movedThisPass = false;

    for (const date of overloadedDates) {
      const daySchedules = working
        .filter((schedule) => schedule.scheduledDate === date)
        .sort((a, b) => compareSchedulePriority(a, b));

      for (const schedule of daySchedules) {
        if (movedScheduleIds.has(schedule.id)) {
          continue;
        }

        const candidateDate = findRescheduleCandidateDate(schedule, working, exam, effectiveConfig);
        if (!candidateDate) {
          continue;
        }

        const originalDate = schedule.scheduledDate;
        schedule.originalScheduledDate = schedule.originalScheduledDate ?? originalDate;
        schedule.scheduledDate = candidateDate;
        schedule.status = 'RESCHEDULED';
        schedule.rescheduleReason = `Rescheduled due to daily review limit: ${originalDate} → ${candidateDate}`;
        schedule.userMessage = `일일 제한을 맞추기 위해 ${originalDate}에서 ${candidateDate}로 이동했습니다.`;
        schedule.updatedAt = originalDate;
        movedScheduleIds.add(schedule.id);
        movedThisPass = true;
        break;
      }
    }

    if (!movedThisPass) {
      for (const date of overloadedDates) {
        const affectedSchedules = working.filter((schedule) => schedule.scheduledDate === date);
        const warning = buildOverloadWarning(date, affectedSchedules, 'DAILY_REVIEW_LIMIT_EXCEEDED', effectiveConfig);
        warnings.push(warning);

        const unresolved: UnresolvedOverload = {
          ...warning,
          status: 'OVERLOADED_UNRESOLVED',
        };
        unresolvedOverloads.push(unresolved);

        for (const schedule of affectedSchedules) {
          schedule.status = 'OVERLOADED_UNRESOLVED';
          schedule.userMessage = `일일 학습량 제한을 해결할 수 없어 ${date} 일정이 유지되었습니다. 시험일 조정, 제한 확대, 또는 수동 조정이 필요합니다.`;
          if (schedule.isFinalReview) {
            schedule.rescheduleReason = `Final review fixed at ${schedule.lastReviewDate ?? schedule.scheduledDate} and remains overloaded`;
          }
        }
      }
      break;
    }
  }

  return {
    schedules: working,
    warnings,
    unresolvedOverloads,
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
 * 이 함수는 LocalDate 유틸리티와 정규 리뷰 일정 생성 규칙을 함께 검증한다.
 */
export function runSelfChecks(): void {
  // 1) 시험일 검증 기본 예시
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

  const allowedCase = validateExamDate('2026-10-01' as LocalDate, '2026-10-06' as LocalDate, {
    ...DEFAULT_SCHEDULER_CONFIG,
    finalReviewBufferDays: 1,
    minimumEffectiveStudyDays: 3,
  });
  assert(allowedCase.isValid === true, '예시 2는 허용되어야 합니다.');
  assert(
    allowedCase.earliestAllowedExamDate === '2026-10-06' as LocalDate,
    '가장 빠른 허용 시험일은 2026-10-06이어야 합니다.',
  );

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

  const pastCase = validateExamDate('2026-10-01' as LocalDate, '2026-10-01' as LocalDate, {
    ...DEFAULT_SCHEDULER_CONFIG,
    finalReviewBufferDays: 1,
    minimumEffectiveStudyDays: 3,
  });
  assert(pastCase.isValid === false, '오늘 또는 과거 시험일은 차단되어야 합니다.');

  // 2) 최소 복습 횟수 경계값 검증
  assert(getMinimumReviewCount(2) === 0, '2일은 0회');
  assert(getMinimumReviewCount(3) === 2, '3일은 2회');
  assert(getMinimumReviewCount(6) === 2, '6일은 2회');
  assert(getMinimumReviewCount(7) === 3, '7일은 3회');
  assert(getMinimumReviewCount(13) === 3, '13일은 3회');
  assert(getMinimumReviewCount(14) === 4, '14일은 4회');
  assert(getMinimumReviewCount(29) === 4, '29일은 4회');
  assert(getMinimumReviewCount(30) === 5, '30일은 5회');
  assert(getMinimumReviewCount(59) === 5, '59일은 5회');
  assert(getMinimumReviewCount(60) === 6, '60일은 6회');
  assert(getMinimumReviewCount(119) === 6, '119일은 6회');
  assert(getMinimumReviewCount(120) === 7, '120일은 7회');

  // 3) 충분한 창의 정상 스케줄
  const sufficient = generateScaledReviewDates({
    record: {
      id: 'r1',
      examId: 'e1',
      content: '약점 정리',
      studiedDate: '2026-10-01' as LocalDate,
      subjectName: '수학',
      difficulty: 'medium',
      importance: 'normal',
      estimatedReviewMinutes: 20,
      initialMastery: 3,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    examDate: '2026-11-15' as LocalDate,
    finalReviewBufferDays: 1,
  });
  assert(sufficient.generatedDates[0] === '2026-10-02' as LocalDate, '첫 날짜는 reviewStartDate여야 합니다.');
  assert(sufficient.generatedDates[sufficient.generatedDates.length - 1] === '2026-11-13' as LocalDate, '마지막 날짜는 lastReviewDate여야 합니다.');
  assert(sufficient.generatedDates.length === 5, '충분한 창은 5개 일정을 생성해야 합니다.');
  assert(sufficient.status.includes('SCHEDULED'), '충분한 창은 SCHEDULED 상태여야 합니다.');
  assert(sufficient.generatedDates.every((date, index, arr) => index === 0 || compareLocalDates(arr[index - 1], date) < 0), '일정은 오름차순이어야 합니다.');

  // 4) 압축된 창
  const compressed = generateScaledReviewDates({
    record: {
      id: 'r2',
      examId: 'e2',
      content: '압축 일정 사례',
      studiedDate: '2026-10-01' as LocalDate,
      subjectName: '생물',
      difficulty: 'hard',
      importance: 'high',
      estimatedReviewMinutes: 15,
      initialMastery: 2,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    examDate: '2026-10-12' as LocalDate,
    finalReviewBufferDays: 1,
  });
  assert(compressed.generatedDates.length === 5, '압축 창은 5개의 유효한 날짜만 생성해야 합니다.');
  assert(compressed.generatedDates[0] === '2026-10-02' as LocalDate, '압축 일정의 시작은 reviewStartDate여야 합니다.');
  assert(compressed.generatedDates[compressed.generatedDates.length - 1] === '2026-10-10' as LocalDate, '압축 일정의 마지막은 lastReviewDate여야 합니다.');
  assert(compressed.status.includes('INSUFFICIENT_WINDOW') || compressed.status.includes('SCHEDULED'), '압축 일정은 상태를 포함해야 합니다.');

  // 5) 너무 짧은 창
  const tooShort = generateScaledReviewDates({
    record: {
      id: 'r3',
      examId: 'e3',
      content: '너무 짧은 경우',
      studiedDate: '2026-10-01' as LocalDate,
      subjectName: '문학',
      difficulty: 'medium',
      importance: 'normal',
      estimatedReviewMinutes: 15,
      initialMastery: 3,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    examDate: '2026-10-05' as LocalDate,
    finalReviewBufferDays: 1,
  });
  assert(tooShort.generatedDates.length === 1, '1일 사용 가능 창은 1개만 생성해야 합니다.');
  assert(tooShort.generatedDates[0] === '2026-10-02' as LocalDate, '하나의 유효 날짜는 2026-10-02여야 합니다.');
  assert(tooShort.status.includes('INSUFFICIENT_WINDOW') || tooShort.status.includes('CRAM_MODE_REQUIRED'), '짧은 창은 부족 상태를 포함해야 합니다.');

  // 6) 날짜 경계와 중복 방지
  const cleaned = ensureStrictlyIncreasingUniqueDates(
    ['2026-10-02' as LocalDate, '2026-10-02' as LocalDate, '2026-10-03' as LocalDate, '2026-10-05' as LocalDate, '2026-10-05' as LocalDate],
    '2026-10-02' as LocalDate,
    '2026-10-05' as LocalDate,
  );
  assert(cleaned.join(',') === '2026-10-02,2026-10-03,2026-10-05', '중복과 같은 날 보정은 제거되어야 합니다.');

  // 7) 날짜 유틸리티 기본 확인
  assert(isValidLocalDate('2026-10-01') === true, '유효한 LocalDate는 true여야 합니다.');
  assert(isValidLocalDate('2026-02-29') === false, '불가능한 날짜는 false여야 합니다.');
  assert(daysBetween('2026-10-01' as LocalDate, '2026-10-02' as LocalDate) === 1, 'day difference는 1이어야 합니다.');
  assert(addDays('2026-10-01' as LocalDate, 3) === '2026-10-04' as LocalDate, 'addDays는 3일 후를 계산해야 합니다.');
  assert(
    getEarliestAllowedExamDate('2026-10-01' as LocalDate, 1, 3) === '2026-10-06' as LocalDate,
    '최초 허용 시험일 계산식은 2026-10-06이어야 합니다.',
  );

  // 8) 우선순위 점수와 재배치 경로 확인
  const priorityBase = {
    id: 'priority-a',
    studyRecordId: 'A',
    reviewIndex: 0,
    scheduledDate: '2026-10-05' as LocalDate,
    status: 'SCHEDULED' as ReviewStatus,
    priorityScore: 0,
    estimatedReviewMinutes: 15,
    recommendedMethod: 'tiny',
    notificationPlan: { kind: 'IN_APP', hour: 9, reminder: false },
    dueDaysBeforeExam: 10,
    isFinalReview: false,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };
  const highRiskRecord = {
    id: 'A',
    examId: 'e-priority',
    content: '우선순위 점수 테스트',
    studiedDate: '2026-10-01' as LocalDate,
    subjectName: '과학',
    difficulty: 'hard' as const,
    importance: 'high' as const,
    estimatedReviewMinutes: 15,
    initialMastery: 1 as const,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };
  const lowRiskRecord = {
    id: 'B',
    examId: 'e-priority',
    content: '낮은 위험 테스트',
    studiedDate: '2026-10-01' as LocalDate,
    subjectName: '역사',
    difficulty: 'easy' as const,
    importance: 'low' as const,
    estimatedReviewMinutes: 10,
    initialMastery: 5 as const,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };
  const priorityExam = {
    id: 'e-priority',
    examDate: '2026-11-15' as LocalDate,
    finalReviewBufferDays: 1,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };
  const highRiskScore = calculatePriorityScore(priorityBase, highRiskRecord, priorityExam, DEFAULT_SCHEDULER_CONFIG);
  const lowRiskScore = calculatePriorityScore({ ...priorityBase, id: 'priority-b', studyRecordId: 'B', reviewIndex: 1, scheduledDate: '2026-10-06' as LocalDate }, lowRiskRecord, priorityExam, DEFAULT_SCHEDULER_CONFIG);
  assert(highRiskScore > lowRiskScore, '높은 중요도/난이도/낮은 숙련도는 우선순위 점수가 높아야 합니다.');

  const scenarioExam = {
    id: 'e-example',
    examDate: '2026-11-15' as LocalDate,
    finalReviewBufferDays: 1,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };
  const scenarioConfig = {
    ...DEFAULT_SCHEDULER_CONFIG,
    maxDailyReviewMinutes: 30,
    finalReviewBufferDays: 1,
  };
  const scenarioRecords = [
    {
      id: 'A',
      examId: 'e-example',
      content: '한국사 조선 후기 세도정치 핵심 원인과 결과',
      studiedDate: '2026-10-01' as LocalDate,
      subjectName: '한국사',
      difficulty: 'medium' as const,
      importance: 'normal' as const,
      initialMastery: 3 as const,
      estimatedReviewMinutes: 15,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    {
      id: 'B',
      examId: 'e-example',
      content: '영어 가정법 과거와 가정법 과거완료 구분',
      studiedDate: '2026-10-08' as LocalDate,
      subjectName: '영어',
      difficulty: 'hard' as const,
      importance: 'high' as const,
      initialMastery: 2 as const,
      estimatedReviewMinutes: 20,
      createdAt: '2026-10-08' as LocalDate,
      updatedAt: '2026-10-08' as LocalDate,
    },
    {
      id: 'C',
      examId: 'e-example',
      content: '생명과학 세포호흡 ATP 생성 과정',
      studiedDate: '2026-10-25' as LocalDate,
      subjectName: '생명과학',
      difficulty: 'easy' as const,
      importance: 'normal' as const,
      initialMastery: 4 as const,
      estimatedReviewMinutes: 15,
      createdAt: '2026-10-25' as LocalDate,
      updatedAt: '2026-10-25' as LocalDate,
    },
    {
      id: 'D',
      examId: 'e-example',
      content: '수학 미분계수의 정의와 그래프 해석',
      studiedDate: '2026-10-25' as LocalDate,
      subjectName: '수학',
      difficulty: 'hard' as const,
      importance: 'high' as const,
      initialMastery: 1 as const,
      estimatedReviewMinutes: 25,
      createdAt: '2026-10-25' as LocalDate,
      updatedAt: '2026-10-25' as LocalDate,
    },
  ] as StudyRecord[];

  const scenarioBefore = scenarioRecords.map((record) => {
    const generated = generateScaledReviewDates({
      record,
      examDate: scenarioExam.examDate,
      finalReviewBufferDays: 1,
      config: scenarioConfig,
    });
    return {
      recordId: record.id,
      generatedDates: generated.generatedDates,
      lastReviewDate: generated.lastReviewDate,
      status: generated.status,
    };
  });
  const scenarioAfter = createReviewSchedules(scenarioRecords, scenarioExam, scenarioConfig);
  assert(
    scenarioAfter.schedules.every((schedule) => schedule.scheduledDate < scenarioExam.examDate),
    '예시 일정은 시험일 또는 버퍼 구간에 속하면 안 됩니다.',
  );
  assert(
    scenarioAfter.schedules.every((schedule) => !schedule.isFinalReview || schedule.scheduledDate === schedule.lastReviewDate),
    '최종 복습은 마지막 리뷰일에 고정되어야 합니다.',
  );
  const unresolvedDates = new Set(scenarioAfter.unresolvedOverloads.map((item) => item.affectedDate));
  assert(
    Array.from(scenarioAfter.dailyLoadByDate.entries()).every(([date, load]) => unresolvedDates.has(date) || load <= scenarioConfig.maxDailyReviewMinutes),
    '해결된 일별 로드는 maxDailyReviewMinutes를 초과하면 안 됩니다.',
  );
  assert(scenarioAfter.unresolvedOverloads.length > 0, '예시 상황은 미해결 과부하를 보유해야 합니다.');

  const overloadFixtureExam = {
    ...scenarioExam,
    id: 'e-overload',
    examDate: '2026-11-15' as LocalDate,
  };
  const overloadFixtureRecords = [
    { id: 'O1', examId: 'e-overload', content: '충돌 A', studiedDate: '2026-10-03' as LocalDate, subjectName: '비문학', difficulty: 'hard' as const, importance: 'normal' as const, estimatedReviewMinutes: 20, initialMastery: 2 as const, createdAt: '2026-10-03' as LocalDate, updatedAt: '2026-10-03' as LocalDate },
    { id: 'O2', examId: 'e-overload', content: '충돌 B', studiedDate: '2026-10-03' as LocalDate, subjectName: '비문학', difficulty: 'medium' as const, importance: 'low' as const, estimatedReviewMinutes: 15, initialMastery: 3 as const, createdAt: '2026-10-03' as LocalDate, updatedAt: '2026-10-03' as LocalDate },
    { id: 'O3', examId: 'e-overload', content: '충돌 C', studiedDate: '2026-10-03' as LocalDate, subjectName: '비문학', difficulty: 'easy' as const, importance: 'normal' as const, estimatedReviewMinutes: 10, initialMastery: 4 as const, createdAt: '2026-10-03' as LocalDate, updatedAt: '2026-10-03' as LocalDate },
  ] as StudyRecord[];
  const overloadScenario = createReviewSchedules(overloadFixtureRecords, overloadFixtureExam, {
    ...DEFAULT_SCHEDULER_CONFIG,
    maxDailyReviewMinutes: 20,
    finalReviewBufferDays: 1,
  });
  assert(
    overloadScenario.schedules.length >= 3,
    '오버로드 테스트는 생성된 일정이 있어야 합니다.',
  );
  assert(
    overloadScenario.schedules.some((schedule) => schedule.status === 'RESCHEDULED' || schedule.status === 'OVERLOADED_UNRESOLVED'),
    '오버로드 경로는 재배치하거나 미해결 상태를 남겨야 합니다.',
  );

  const outcomeBaseSchedules: ReviewSchedule[] = [
    {
      id: 'rec-1',
      studyRecordId: 'R1',
      reviewIndex: 0,
      scheduledDate: '2026-10-11' as LocalDate,
      originalScheduledDate: '2026-10-11' as LocalDate,
      reviewStartDate: '2026-10-02' as LocalDate,
      lastReviewDate: '2026-11-13' as LocalDate,
      status: 'COMPLETED',
      priorityScore: 10,
      estimatedReviewMinutes: 20,
      recommendedMethod: '회상',
      notificationPlan: { kind: 'IN_APP', hour: 9, reminder: false },
      dueDaysBeforeExam: 35,
      isFinalReview: false,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    {
      id: 'rec-2',
      studyRecordId: 'R1',
      reviewIndex: 1,
      scheduledDate: '2026-10-16' as LocalDate,
      originalScheduledDate: '2026-10-16' as LocalDate,
      reviewStartDate: '2026-10-02' as LocalDate,
      lastReviewDate: '2026-11-13' as LocalDate,
      status: 'SCHEDULED',
      priorityScore: 12,
      estimatedReviewMinutes: 20,
      recommendedMethod: '회상',
      notificationPlan: { kind: 'IN_APP', hour: 9, reminder: false },
      dueDaysBeforeExam: 30,
      isFinalReview: false,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    {
      id: 'rec-3',
      studyRecordId: 'R1',
      reviewIndex: 2,
      scheduledDate: '2026-10-27' as LocalDate,
      originalScheduledDate: '2026-10-27' as LocalDate,
      reviewStartDate: '2026-10-02' as LocalDate,
      lastReviewDate: '2026-11-13' as LocalDate,
      status: 'SCHEDULED',
      priorityScore: 11,
      estimatedReviewMinutes: 20,
      recommendedMethod: '회상',
      notificationPlan: { kind: 'IN_APP', hour: 9, reminder: false },
      dueDaysBeforeExam: 19,
      isFinalReview: false,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    {
      id: 'rec-4',
      studyRecordId: 'R1',
      reviewIndex: 3,
      scheduledDate: '2026-11-05' as LocalDate,
      originalScheduledDate: '2026-11-05' as LocalDate,
      reviewStartDate: '2026-10-02' as LocalDate,
      lastReviewDate: '2026-11-13' as LocalDate,
      status: 'SCHEDULED',
      priorityScore: 15,
      estimatedReviewMinutes: 20,
      recommendedMethod: '회상',
      notificationPlan: { kind: 'IN_APP', hour: 9, reminder: false },
      dueDaysBeforeExam: 10,
      isFinalReview: false,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    {
      id: 'rec-5',
      studyRecordId: 'R1',
      reviewIndex: 4,
      scheduledDate: '2026-11-13' as LocalDate,
      originalScheduledDate: '2026-11-13' as LocalDate,
      reviewStartDate: '2026-10-02' as LocalDate,
      lastReviewDate: '2026-11-13' as LocalDate,
      status: 'SCHEDULED',
      priorityScore: 30,
      estimatedReviewMinutes: 20,
      recommendedMethod: '최종 점검',
      notificationPlan: { kind: 'IN_APP', hour: 19, reminder: true },
      dueDaysBeforeExam: 2,
      isFinalReview: true,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
  ];

  const examForOutcome = {
    id: 'exam-outcome',
    examDate: '2026-11-15' as LocalDate,
    finalReviewBufferDays: 1,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };

  const recordForOutcome: StudyRecord = {
    id: 'R1',
    examId: 'exam-outcome',
    content: '경제학 수요·공급 곡선 이동과 균형가격 변화',
    studiedDate: '2026-10-01' as LocalDate,
    subjectName: '경제학',
    difficulty: 'medium',
    importance: 'high',
    estimatedReviewMinutes: 20,
    initialMastery: 3,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };

  const easyOutcome = rescheduleAfterReviewOutcome({
    record: recordForOutcome,
    exam: examForOutcome,
    schedules: outcomeBaseSchedules,
    completedReviewDate: '2026-10-11' as LocalDate,
    outcome: 'EASY',
    reviewStartDate: '2026-10-02' as LocalDate,
    lastReviewDate: '2026-11-13' as LocalDate,
    config: { ...DEFAULT_SCHEDULER_CONFIG, finalReviewBufferDays: 1, maxDailyReviewMinutes: 30 },
  });
  assert(easyOutcome.changed === true, 'EASY는 유효한 슬롯이 있으면 변경되어야 합니다.');
  assert(easyOutcome.schedules.some((schedule) => schedule.id === 'rec-2' && schedule.scheduledDate === '2026-10-17' as LocalDate), 'EASY는 가장 가까운 미래 일정을 뒤로 미뤄야 합니다.');
  assert(easyOutcome.schedules.some((schedule) => schedule.id === 'rec-5' && schedule.scheduledDate === '2026-11-13' as LocalDate), 'EASY는 최종 복습을 보존해야 합니다.');

  const successOutcome = rescheduleAfterReviewOutcome({
    record: recordForOutcome,
    exam: examForOutcome,
    schedules: outcomeBaseSchedules,
    completedReviewDate: '2026-10-11' as LocalDate,
    outcome: 'SUCCESS',
    reviewStartDate: '2026-10-02' as LocalDate,
    lastReviewDate: '2026-11-13' as LocalDate,
    config: { ...DEFAULT_SCHEDULER_CONFIG, finalReviewBufferDays: 1, maxDailyReviewMinutes: 30 },
  });
  assert(successOutcome.changed === false, 'SUCCESS 기본 정책은 일정을 유지해야 합니다.');

  const successRelaxed = rescheduleAfterReviewOutcome({
    record: recordForOutcome,
    exam: examForOutcome,
    schedules: outcomeBaseSchedules,
    completedReviewDate: '2026-10-11' as LocalDate,
    outcome: 'SUCCESS',
    reviewStartDate: '2026-10-02' as LocalDate,
    lastReviewDate: '2026-11-13' as LocalDate,
    config: { ...DEFAULT_SCHEDULER_CONFIG, finalReviewBufferDays: 1, maxDailyReviewMinutes: 30 },
    policy: { allowSuccessDelay: true, successDelayDays: 1 },
  });
  assert(typeof successRelaxed.message === 'string' && successRelaxed.message.length > 0, '성공의 느슨한 정책은 안전한 피드백을 남겨야 합니다.');

  const hardOutcome = rescheduleAfterReviewOutcome({
    record: recordForOutcome,
    exam: examForOutcome,
    schedules: outcomeBaseSchedules,
    completedReviewDate: '2026-10-11' as LocalDate,
    outcome: 'HARD',
    reviewStartDate: '2026-10-02' as LocalDate,
    lastReviewDate: '2026-11-13' as LocalDate,
    config: { ...DEFAULT_SCHEDULER_CONFIG, finalReviewBufferDays: 1, maxDailyReviewMinutes: 30 },
  });
  assert(hardOutcome.changed === true, 'HARD는 유효한 앞당김 슬롯이 있으면 변경되어야 합니다.');
  assert(hardOutcome.schedules.some((schedule) => schedule.id === 'rec-2' && schedule.scheduledDate === '2026-10-15' as LocalDate), 'HARD는 가장 가까운 미래 일정을 최대 1일 앞당겨야 합니다.');

  const failedOutcome = rescheduleAfterReviewOutcome({
    record: recordForOutcome,
    exam: examForOutcome,
    schedules: outcomeBaseSchedules,
    completedReviewDate: '2026-10-11' as LocalDate,
    outcome: 'FAILED',
    reviewStartDate: '2026-10-02' as LocalDate,
    lastReviewDate: '2026-11-13' as LocalDate,
    config: { ...DEFAULT_SCHEDULER_CONFIG, finalReviewBufferDays: 1, maxDailyReviewMinutes: 30 },
  });
  assert(failedOutcome.changed === true, 'FAILED는 가장 빠른 유효한 슬롯으로 옮겨야 합니다.');
  assert(failedOutcome.schedules.some((schedule) => schedule.id === 'rec-2' && schedule.scheduledDate === '2026-10-13' as LocalDate), 'FAILED는 가장 빠른 유효한 미래 날짜를 사용해야 합니다.');
  assert(failedOutcome.extraReviewInserted === true, 'FAILED는 유효한 슬롯이 있으면 추가 리뷰를 삽입할 수 있어야 합니다.');

  const badSchedule = [
    { ...outcomeBaseSchedules[1], scheduledDate: '2026-10-17' as LocalDate },
    { ...outcomeBaseSchedules[2], scheduledDate: '2026-10-18' as LocalDate },
  ];
  const invalidOutcome = rescheduleAfterReviewOutcome({
    record: recordForOutcome,
    exam: examForOutcome,
    schedules: badSchedule,
    completedReviewDate: '2026-10-11' as LocalDate,
    outcome: 'FAILED',
    reviewStartDate: '2026-10-02' as LocalDate,
    lastReviewDate: '2026-11-13' as LocalDate,
    config: { ...DEFAULT_SCHEDULER_CONFIG, finalReviewBufferDays: 1, maxDailyReviewMinutes: 30 },
  });
  assert(invalidOutcome.validation.isValid === false || invalidOutcome.warningCodes.includes('INVALID_INPUT'), '잘못된 입력 일정은 안전하게 보고되어야 합니다.');

  const extraCheck = insertAdditionalReviewIfPossible(
    'R1',
    outcomeBaseSchedules,
    '2026-10-11' as LocalDate,
    examForOutcome,
    '2026-10-02' as LocalDate,
    '2026-11-13' as LocalDate,
    { ...DEFAULT_SCHEDULER_CONFIG, finalReviewBufferDays: 1 },
  );
  assert(extraCheck.inserted === true, '유효한 간격이 있으면 추가 회상 점검을 삽입할 수 있어야 합니다.');

  const cramInput = [
    { ...outcomeBaseSchedules[1], scheduledDate: '2026-10-16' as LocalDate },
    { ...outcomeBaseSchedules[2], scheduledDate: '2026-10-17' as LocalDate },
    { ...outcomeBaseSchedules[3], scheduledDate: '2026-10-18' as LocalDate },
    { ...outcomeBaseSchedules[4], scheduledDate: '2026-11-13' as LocalDate },
  ];
  const cramResult = rescheduleAfterReviewOutcome({
    record: recordForOutcome,
    exam: examForOutcome,
    schedules: cramInput,
    completedReviewDate: '2026-10-11' as LocalDate,
    outcome: 'HARD',
    reviewStartDate: '2026-10-02' as LocalDate,
    lastReviewDate: '2026-11-13' as LocalDate,
    config: { ...DEFAULT_SCHEDULER_CONFIG, finalReviewBufferDays: 1, maxDailyReviewMinutes: 30 },
  });
  assert(cramResult.message.includes('크램') || cramResult.warningCodes.includes('CRAM_MODE_REQUIRED') || cramResult.validation.isValid === false, '가용 슬롯이 없으면 크램 모드 또는 안전한 경고가 내려야 합니다.');

  console.log(JSON.stringify({
    easyOutcome: {
      changed: easyOutcome.changed,
      scheduleDates: easyOutcome.schedules.filter((schedule) => schedule.studyRecordId === 'R1').map((schedule) => ({ id: schedule.id, date: schedule.scheduledDate, status: schedule.status })),
    },
    failedOutcome: {
      changed: failedOutcome.changed,
      message: failedOutcome.message,
      extraReviewInserted: failedOutcome.extraReviewInserted,
    },
    scenarioAfterSummary: {
      totalSchedules: scenarioAfter.summary.totalSchedules,
      overloadedDateCount: scenarioAfter.summary.overloadedDateCount,
      warnings: scenarioAfter.warnings.length,
      unresolved: scenarioAfter.unresolvedOverloads.length,
    },
    overloadFixtureSummary: {
      scheduleStatuses: overloadScenario.schedules.map((schedule) => ({ id: schedule.id, status: schedule.status, scheduledDate: schedule.scheduledDate })),
      warningCount: overloadScenario.warnings.length,
      unresolvedCount: overloadScenario.unresolvedOverloads.length,
    },
  }, null, 2));

  console.log('review-domain validation checks passed');
}

if (process.argv[1] && /review-domain\.ts$/u.test(process.argv[1])) {
  runSelfChecks();
}
