/**
 * 시험 준비형 분산 복습 앱의 날짜/정책 도메인.
 *
 * LocalDate 계산은 사용자 시계, UTC 변환, DST 경계값을 도메인 계약에 섞지 않고,
 * 캘린더 날짜 기준으로만 동작하게 하여 시간대 경계 버그를 방지한다.
 */

export type LocalDateString = string & { readonly __localDateBrand: unique symbol };
export type LocalDate = LocalDateString;

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
  minute?: number;
  reminder: boolean;
  timezone?: string;
  reminderTimes?: Array<{ hour: number; minute: number }>;
  secondaryReminderHour?: number;
  secondaryReminderMinute?: number;
  requiresFollowUpReminder?: boolean;
}

export interface Exam {
  id: string;
  title?: string;
  examDate: LocalDate;
  timezone: string;
  finalReviewBufferDays: number;
  maxDailyReviewMinutes: number;
  allowRegularReviewOnDayBeforeExam: boolean;
  createdAt: LocalDate;
  updatedAt: LocalDate;
  validationPolicy?: ExamValidationPolicy;
}

export interface StudyRecord {
  id: string;
  examId: string;
  subjectId?: string;
  title: string;
  content: string;
  studiedAt: LocalDate;
  difficulty: Difficulty;
  importance: Importance;
  initialMastery: 1 | 2 | 3 | 4 | 5;
  estimatedReviewMinutes: number;
  optionalMinReviewCount?: number;
  isCompleted: boolean;
  createdAt: LocalDate;
  updatedAt: LocalDate;
}

export interface ReviewSchedule {
  id: string;
  examId: string;
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
  originalScheduledDate?: LocalDate;
  reviewStartDate?: LocalDate;
  lastReviewDate?: LocalDate;
  userMessage?: string;
  outcome?: ReviewOutcome;
  completedAt?: LocalDate;
}

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

export interface NotificationRescheduleRequest {
  reviewScheduleId: string;
  studyRecordId: string;
  action: 'CANCEL_AND_RESCHEDULE' | 'CREATE';
  previousScheduledDate?: LocalDate;
  scheduledDate: LocalDate;
  notificationPlan: NotificationPlan;
  reason: string;
}

export interface ReviewOutcomeRescheduleInput {
  completedReview: ReviewSchedule;
  outcome: ReviewOutcome;
  today: LocalDate;
  allSchedules: ReviewSchedule[];
  studyRecord: StudyRecord;
  exam: Exam;
  config: SchedulerConfig;
}

export interface ReviewOutcomeRescheduleResult {
  updatedSchedules: ReviewSchedule[];
  changedScheduleIds: string[];
  warnings: string[];
  reason: string;
  notificationRescheduleRequests: NotificationRescheduleRequest[];
}

export interface CompleteReviewScheduleResult {
  completedSchedule: ReviewSchedule;
  rescheduling: ReviewOutcomeRescheduleResult;
}

export interface ValidationResult {
  isValid: boolean;
  blockingReason?: string;
  blockingReasons: string[];
  warnings: string[];
  availableDaysForNewLearning: number;
  recommendedEarliestExamDate: LocalDate;
  recordsRequiringCramMode: string[];
  earliestAllowedExamDate?: LocalDate;
  lastReviewDate: LocalDate;
  userMessage: string;
}

export interface SchedulingResult {
  schedules: ReviewSchedule[];
  warnings: string[];
  status: ReviewStatus;
  effectiveStudyDays: number;
  usableWindowDays: number;
  targetReviewCount: number;
  generatedReviewCount: number;
  isValid?: boolean;
  validation?: ValidationResult;
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
  timezone: string;
  finalReviewBufferDays: number;
  minEffectiveStudyDays: number;
  minimumEffectiveStudyDays?: number;
  maxDailyReviewMinutes: number;
  baseIntervals: readonly number[];
  allowExamDateWithCramRequiredRecords: boolean;
  strictExistingRecordValidation: boolean;
  rescheduleSearchRangeDays: number;
  reminderHour: number;
  reminderMinute: number;
  eveningReminderHour: number;
  eveningReminderMinute: number;
  allowRegularReviewOnDayBeforeExam?: boolean;
  defaultNotificationHour?: number;
  reminderNotificationHour?: number;
  examValidationPolicy?: ExamValidationPolicy;
  preferredRescheduleRangeDays?: number;
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

export interface PriorityScoreBreakdown {
  importance: number;
  difficulty: number;
  initialMastery: number;
  examProximity: number;
  finalReview: number;
  reviewPosition: number;
  dueDaysBeforeExam: number;
  proximityCap: number;
}

export interface PriorityScoreResult {
  score: number;
  breakdown: PriorityScoreBreakdown;
}

export interface DailyReviewLoad {
  scheduledDate: LocalDate;
  scheduleCount: number;
  totalEstimatedReviewMinutes: number;
  maxDailyReviewMinutes: number;
  isOverloaded: boolean;
  overloadMinutes: number;
  scheduleIds: string[];
}

export interface StudyRecordScheduleBundle {
  record: StudyRecord;
  schedules: ReviewSchedule[];
  generation?: GenerateReviewDatesResult;
}

export interface RebalanceMove {
  scheduleId: string;
  originalDate: LocalDate;
  newDate: LocalDate;
  originalLoad: number;
  priority: PriorityScoreResult;
  reason: string;
}

export interface RebalanceDailyLoadInput {
  schedules?: ReviewSchedule[];
  bundles?: StudyRecordScheduleBundle[];
  exam: Exam;
  finalReviewBufferDays?: number;
  maxDailyReviewMinutes?: number;
}

export interface RebalanceDailyLoadResult {
  schedules: ReviewSchedule[];
  dailyLoads: DailyReviewLoad[];
  warnings: string[];
  unresolvedOverloadDates: LocalDate[];
  moves: RebalanceMove[];
  unresolvedOverloads: UnresolvedOverload[];
}

export interface ExamDateValidationInput {
  todayDate: LocalDate;
  examDate: LocalDate;
  finalReviewBufferDays?: number;
  minEffectiveStudyDays?: number;
  records?: StudyRecord[];
  config?: Partial<SchedulerConfig>;
}

export interface ExamDateValidationViewModel {
  canSaveExamDate: boolean;
  primaryMessage?: string;
  helperMessages: string[];
  recommendedEarliestExamDate?: LocalDate;
  blockingReason?: string;
  warningCount: number;
}

export const DEFAULT_TIMEZONE = 'Asia/Seoul' as const;
export const DEFAULT_MIN_EFFECTIVE_STUDY_DAYS = 3 as const;
export const DEFAULT_FINAL_REVIEW_BUFFER_DAYS = 1 as const;
export const DEFAULT_BASE_INTERVALS = [1, 3, 7, 14, 30, 60, 120] as const;
export const DEFAULT_MAX_DAILY_REVIEW_MINUTES = 120 as const;
export const DEFAULT_RESCHEDULE_SEARCH_RANGE_DAYS = 2 as const;

export const BASE_INTERVALS = DEFAULT_BASE_INTERVALS;

export const DEFAULT_SCHEDULER_CONFIG: SchedulerConfig = {
  timezone: DEFAULT_TIMEZONE,
  finalReviewBufferDays: DEFAULT_FINAL_REVIEW_BUFFER_DAYS,
  minEffectiveStudyDays: DEFAULT_MIN_EFFECTIVE_STUDY_DAYS,
  minimumEffectiveStudyDays: DEFAULT_MIN_EFFECTIVE_STUDY_DAYS,
  maxDailyReviewMinutes: DEFAULT_MAX_DAILY_REVIEW_MINUTES,
  defaultNotificationHour: 9,
  reminderNotificationHour: 19,
  allowRegularReviewOnDayBeforeExam: false,
  examValidationPolicy: 'BLOCK_IF_ANY_RECORD_INSUFFICIENT',
  baseIntervals: DEFAULT_BASE_INTERVALS,
  preferredRescheduleRangeDays: DEFAULT_RESCHEDULE_SEARCH_RANGE_DAYS,
  allowExamDateWithCramRequiredRecords: true,
  strictExistingRecordValidation: false,
  rescheduleSearchRangeDays: DEFAULT_RESCHEDULE_SEARCH_RANGE_DAYS,
  reminderHour: 9,
  reminderMinute: 0,
  eveningReminderHour: 19,
  eveningReminderMinute: 0,
};

export function createDefaultSchedulerConfig(
  overrides: Partial<SchedulerConfig> = {},
): SchedulerConfig {
  return {
    ...DEFAULT_SCHEDULER_CONFIG,
    ...overrides,
    allowExamDateWithCramRequiredRecords:
      overrides.allowExamDateWithCramRequiredRecords ?? DEFAULT_SCHEDULER_CONFIG.allowExamDateWithCramRequiredRecords,
    strictExistingRecordValidation:
      overrides.strictExistingRecordValidation ?? DEFAULT_SCHEDULER_CONFIG.strictExistingRecordValidation,
    minEffectiveStudyDays:
      overrides.minEffectiveStudyDays ?? overrides.minimumEffectiveStudyDays ?? DEFAULT_SCHEDULER_CONFIG.minEffectiveStudyDays,
    minimumEffectiveStudyDays:
      overrides.minimumEffectiveStudyDays ?? overrides.minEffectiveStudyDays ?? DEFAULT_SCHEDULER_CONFIG.minimumEffectiveStudyDays,
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
  scheduleStatus?: ReviewStatus;
  wasCompressed: boolean;
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
  record: Pick<StudyRecord, 'difficulty' | 'initialMastery' | 'optionalMinReviewCount'>,
  effectiveStudyDays: number,
  config?: Partial<SchedulerConfig>,
): TargetReviewCountResult & {
  systemMinimum: number;
  adjustedTarget: number;
  adjustmentReasons: string[];
} {
  const systemMinimum = getMinimumReviewCount(effectiveStudyDays);
  const optionalMinimumContribution =
    config?.optionalMinReviewCount ?? record.optionalMinReviewCount ?? 0;

  const adjustmentReasons: string[] = [];
  let extraAdjustment = 0;

  if (record.difficulty === 'hard') {
    extraAdjustment += 1;
    adjustmentReasons.push('난이도 hard → +1 회 복습 보정');
  }

  if (record.initialMastery === 1 || record.initialMastery === 2) {
    if (extraAdjustment === 0) {
      extraAdjustment += 1;
      adjustmentReasons.push('초기 숙련도 1~2 → +1 회 복습 보정');
    } else {
      adjustmentReasons.push('난이도/숙련도 보정은 한 번만 적용');
    }
  }

  const baseFloor = Math.max(systemMinimum, optionalMinimumContribution);
  const adjustedTarget = baseFloor + extraAdjustment;

  return {
    systemMinimum,
    adjustedTarget,
    adjustmentReasons,
    systemMinimumReviewCount: systemMinimum,
    optionalMinimumContribution,
    difficultyAdjustment: record.difficulty === 'hard' ? 1 : 0,
    masteryAdjustment: record.initialMastery === 1 || record.initialMastery === 2 ? 1 : 0,
    extraAdjustmentCap: Math.min(1, extraAdjustment),
    requestedTargetCount: adjustedTarget,
    notes: [
      `system minimum=${systemMinimum}`,
      `optional minimum=${optionalMinimumContribution}`,
      `adjusted target=${adjustedTarget}`,
      ...adjustmentReasons,
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

export function getRecommendedMethod(
  reviewIndex: number,
  totalReviewCount: number,
  isFinalReview: boolean,
): string {
  if (isFinalReview) {
    return '새 내용을 넓히기보다 핵심 구조·자주 틀리는 포인트·대표 문제를 인출 점검하세요.';
  }

  const normalizedIndex = reviewIndex >= 1 ? reviewIndex : 1;
  if (normalizedIndex === 1) {
    return '노트를 보지 말고 핵심 개념 3~5개를 먼저 떠올린 뒤, 기억나지 않는 부분만 확인하세요.';
  }
  if (normalizedIndex === 2) {
    return '플래시카드, 빈칸 채우기 또는 짧은 퀴즈로 능동회상을 해보세요.';
  }

  return '문제풀이, 서술형 회상, 개념 간 연결 설명 중 하나를 수행하고 오답 이유를 기록하세요.';
}

export function createRecommendedMethod(
  reviewIndex: number,
  totalReviewCount: number,
  isFinalReview: boolean,
): string {
  return getRecommendedMethod(reviewIndex, totalReviewCount, isFinalReview);
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
  const studyDate = record.studiedAt ?? record.studiedDate ?? record.createdAt;
  const finalReviewBufferDays =
    typeof input.finalReviewBufferDays === 'number'
      ? input.finalReviewBufferDays
      : config.finalReviewBufferDays;

  if (!isValidLocalDate(studyDate) || !isValidLocalDate(input.examDate)) {
    throw new RangeError('studyDate와 examDate는 모두 유효한 YYYY-MM-DD LocalDate여야 합니다.');
  }

  const reviewStartDate = getReviewStartDate(studyDate);
  const lastReviewDate = getLastReviewDate(input.examDate, finalReviewBufferDays);
  const effectiveStudyDays = getEffectiveStudyDays(studyDate, input.examDate, finalReviewBufferDays);
  const usableWindowDays = Math.max(0, daysBetween(reviewStartDate, lastReviewDate) + 1);

  const warnings: ScheduleGenerationWarning[] = [];
  let scheduleStatus: ReviewStatus = 'SCHEDULED';
  let wasCompressed = false;

  if (effectiveStudyDays < 3) {
    scheduleStatus = 'CRAM_MODE_REQUIRED';
    warnings.push({
      code: 'CRAM_MODE_REQUIRED',
      message: '정규 분산 복습은 불가능합니다. 짧은 회상·퀴즈 중심으로 압축 복습을 검토하세요.',
    });
    return {
      generatedDates: [],
      requestedTargetCount: 0,
      actualGeneratedCount: 0,
      reviewStartDate,
      lastReviewDate,
      effectiveStudyDays,
      usableWindowDays,
      status: ['CRAM_MODE_REQUIRED', 'INSUFFICIENT_WINDOW'],
      scheduleStatus,
      wasCompressed: true,
      warnings,
    };
  }

  if (usableWindowDays < 1) {
    scheduleStatus = 'INSUFFICIENT_WINDOW';
    warnings.push({
      code: 'INSUFFICIENT_WINDOW',
      message: '복습 가능한 날짜 창이 없어 자동 복습 일정을 만들 수 없습니다.',
    });
    return {
      generatedDates: [],
      requestedTargetCount: 0,
      actualGeneratedCount: 0,
      reviewStartDate,
      lastReviewDate,
      effectiveStudyDays,
      usableWindowDays,
      status: ['INSUFFICIENT_WINDOW'],
      scheduleStatus,
      wasCompressed: true,
      warnings,
    };
  }

  const targetResult = getTargetReviewCount(record, effectiveStudyDays, config);
  const requestedTargetCount = Math.max(0, targetResult.adjustedTarget ?? 0);
  const maxFeasibleCount = Math.min(requestedTargetCount, usableWindowDays);

  if (requestedTargetCount > usableWindowDays) {
    wasCompressed = true;
    scheduleStatus = 'INSUFFICIENT_WINDOW';
    warnings.push({
      code: 'INSUFFICIENT_WINDOW',
      message: `요청된 복습 수 ${requestedTargetCount}개가 사용 가능한 날짜 ${usableWindowDays}개를 초과해 가능한 최대 개수만 생성했습니다.`,
    });
  }

  const dateSpan: LocalDate[] = [];
  let cursor = reviewStartDate;
  while (!isAfter(cursor, lastReviewDate)) {
    dateSpan.push(cursor);
    cursor = addDays(cursor, 1);
  }

  const actualTargetCount = Math.max(0, Math.min(requestedTargetCount, dateSpan.length));
  const generatedDates: LocalDate[] = [];

  if (actualTargetCount === 0) {
    return {
      generatedDates: [],
      requestedTargetCount,
      actualGeneratedCount: 0,
      reviewStartDate,
      lastReviewDate,
      effectiveStudyDays,
      usableWindowDays,
      status: ['INSUFFICIENT_WINDOW'],
      scheduleStatus: 'INSUFFICIENT_WINDOW',
      wasCompressed: true,
      warnings,
    };
  }

  if (actualTargetCount === 1) {
    generatedDates.push(reviewStartDate);
  } else {
    const desiredIndices = new Set<number>();
    const totalSpan = Math.max(1, dateSpan.length - 1);

    for (let i = 0; i < actualTargetCount; i += 1) {
      if (i === 0) {
        desiredIndices.add(0);
        continue;
      }
      if (i === actualTargetCount - 1) {
        desiredIndices.add(dateSpan.length - 1);
        continue;
      }

      const ratio = i / Math.max(1, actualTargetCount - 1);
      const rawIndex = Math.round(ratio * totalSpan);
      const candidateIndex = Math.max(1, Math.min(dateSpan.length - 2, rawIndex));
      desiredIndices.add(candidateIndex);
    }

    const anchoredDates = Array.from(desiredIndices)
      .sort((a, b) => a - b)
      .map((index) => dateSpan[index])
      .filter((date): date is LocalDate => typeof date === 'string');

    const repaired = ensureStrictlyIncreasingUniqueDates(
      anchoredDates,
      reviewStartDate,
      lastReviewDate,
    );

    if (repaired.length < actualTargetCount) {
      for (const date of dateSpan) {
        if (repaired.length >= actualTargetCount) break;
        if (!repaired.includes(date)) {
          repaired.push(date);
        }
      }
    }

    const trimmed = ensureStrictlyIncreasingUniqueDates(
      repaired,
      reviewStartDate,
      lastReviewDate,
    );

    if (trimmed.length > actualTargetCount) {
      const kept: LocalDate[] = [trimmed[0]];
      for (let i = 1; i < trimmed.length - 1; i += 1) {
        if (kept.length >= actualTargetCount - 1) break;
        kept.push(trimmed[i]);
      }
      kept.push(trimmed[trimmed.length - 1]);
      trimmed.splice(0, trimmed.length, ...kept);
    }

    trimmed.forEach((date) => generatedDates.push(date));
  }

  const uniqueDates = ensureStrictlyIncreasingUniqueDates(generatedDates, reviewStartDate, lastReviewDate);
  const finalDates = uniqueDates.filter((date) => date >= reviewStartDate && date <= lastReviewDate);

  if (finalDates.length === 0) {
    scheduleStatus = 'INSUFFICIENT_WINDOW';
    warnings.push({
      code: 'INSUFFICIENT_WINDOW',
      message: '사용 가능한 리뷰 창이 없어 고유한 정규 리뷰 날짜를 만들지 못했습니다.',
    });
    return {
      generatedDates: [],
      requestedTargetCount,
      actualGeneratedCount: 0,
      reviewStartDate,
      lastReviewDate,
      effectiveStudyDays,
      usableWindowDays,
      status: ['INSUFFICIENT_WINDOW'],
      scheduleStatus,
      wasCompressed: true,
      warnings,
    };
  }

  if (requestedTargetCount > usableWindowDays || finalDates.length < requestedTargetCount) {
    wasCompressed = true;
    scheduleStatus = 'INSUFFICIENT_WINDOW';
  }

  if (!wasCompressed && usableWindowDays < 14 && requestedTargetCount > 1) {
    wasCompressed = true;
  }

  if (finalDates.length === 1) {
    scheduleStatus = 'CRAM_MODE_REQUIRED';
  }

  if (finalDates.length > 0 && finalDates.length >= 2 && scheduleStatus !== 'INSUFFICIENT_WINDOW') {
    scheduleStatus = 'SCHEDULED';
  }

  return {
    generatedDates: finalDates,
    requestedTargetCount,
    actualGeneratedCount: finalDates.length,
    reviewStartDate,
    lastReviewDate,
    effectiveStudyDays,
    usableWindowDays,
    status: [
      ...(scheduleStatus === 'SCHEDULED' ? ['SCHEDULED'] : []),
      ...(scheduleStatus === 'INSUFFICIENT_WINDOW' ? ['INSUFFICIENT_WINDOW'] : []),
      ...(scheduleStatus === 'CRAM_MODE_REQUIRED' ? ['CRAM_MODE_REQUIRED', 'INSUFFICIENT_WINDOW'] : []),
    ],
    scheduleStatus,
    wasCompressed,
    warnings,
  };
}

export function createReviewSchedulesForStudyRecord(input: {
  record: StudyRecord;
  examDate: LocalDate;
  finalReviewBufferDays?: number;
  config?: Partial<SchedulerConfig>;
}): {
  schedules: ReviewSchedule[];
  generation: GenerateReviewDatesResult;
  warnings: string[];
  scheduleStatus: ReviewStatus;
} {
  const config = createDefaultSchedulerConfig(input.config ?? {});
  const finalReviewBufferDays =
    typeof input.finalReviewBufferDays === 'number'
      ? input.finalReviewBufferDays
      : config.finalReviewBufferDays;
  const generation = generateScaledReviewDates({
    record: input.record,
    examDate: input.examDate,
    finalReviewBufferDays,
    config,
  });

  const schedules: ReviewSchedule[] = generation.generatedDates.map((scheduledDate, index) => {
    const isFinalReview = scheduledDate === generation.lastReviewDate;
    const dueDaysBeforeExam = daysBetween(scheduledDate, input.examDate);
    const reminderHour = isFinalReview ? 9 : 9;
    const followUpHour = isFinalReview ? 18 : 19;

    return {
      id: `${input.record.id}-review-${index + 1}`,
      examId: input.record.examId,
      studyRecordId: input.record.id,
      reviewIndex: index,
      scheduledDate,
      status: generation.scheduleStatus ?? 'SCHEDULED',
      priorityScore: Math.max(1, 100 - dueDaysBeforeExam),
      estimatedReviewMinutes: input.record.estimatedReviewMinutes,
      recommendedMethod: getRecommendedMethod(index + 1, generation.generatedDates.length || 1, isFinalReview),
      notificationPlan: {
        kind: 'PUSH',
        hour: reminderHour,
        minute: 0,
        reminder: true,
        timezone: config.timezone,
        reminderTimes: [
          { hour: 9, minute: 0 },
          ...(isFinalReview ? [{ hour: 18, minute: 0 }] : [{ hour: 19, minute: 0 }]),
        ],
        secondaryReminderHour: followUpHour,
        secondaryReminderMinute: 0,
        requiresFollowUpReminder: !isFinalReview,
      },
      dueDaysBeforeExam,
      isFinalReview,
      createdAt: input.record.createdAt ?? input.record.studiedAt,
      updatedAt: input.record.updatedAt ?? input.record.studiedAt,
      reviewStartDate: generation.reviewStartDate,
      lastReviewDate: generation.lastReviewDate,
    };
  });

  return {
    schedules,
    generation,
    warnings: generation.warnings.map((warning) => warning.message),
    scheduleStatus: generation.scheduleStatus ?? 'SCHEDULED',
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

export function buildExamDateBlockingMessage(validationResult: Partial<ValidationResult>): string {
  if (!validationResult || !validationResult.blockingReason) {
    return '';
  }
  return validationResult.blockingReason;
}

export function buildExamDateWarningMessages(validationResult: Partial<ValidationResult>): string[] {
  if (!validationResult || !Array.isArray(validationResult.warnings)) {
    return [];
  }

  return validationResult.warnings.filter((message): message is string => typeof message === 'string' && message.trim().length > 0);
}

export function buildExamDateValidationState(validationResult: Partial<ValidationResult>): ExamDateValidationViewModel {
  const warningCount = Array.isArray(validationResult.recordsRequiringCramMode)
    ? validationResult.recordsRequiringCramMode.length
    : 0;

  const helperMessages = validationResult.isValid === true
    ? buildExamDateWarningMessages(validationResult)
    : [];

  const primaryMessage = validationResult.isValid === false
    ? buildExamDateBlockingMessage(validationResult)
    : helperMessages[0];

  return {
    canSaveExamDate: validationResult.isValid === true,
    primaryMessage: primaryMessage || undefined,
    helperMessages,
    recommendedEarliestExamDate: validationResult.recommendedEarliestExamDate ?? validationResult.earliestAllowedExamDate,
    blockingReason: validationResult.blockingReason ?? validationResult.blockingReasons?.[0],
    warningCount,
  };
}

export function validateExamDateForNewLearning(
  input: ExamDateValidationInput,
): ValidationResult {
  const effectiveConfig = createDefaultSchedulerConfig(input.config ?? {});
  const finalReviewBufferDays = typeof input.finalReviewBufferDays === 'number'
    ? input.finalReviewBufferDays
    : effectiveConfig.finalReviewBufferDays;
  const minEffectiveStudyDays = typeof input.minEffectiveStudyDays === 'number'
    ? input.minEffectiveStudyDays
    : effectiveConfig.minEffectiveStudyDays;

  const blockingReasons: string[] = [];
  const warnings: string[] = [];
  const recordsRequiringCramMode: string[] = [];

  let normalizedToday: { year: number; month: number; day: number } | null = null;
  let normalizedExam: { year: number; month: number; day: number } | null = null;

  try {
    normalizedToday = parseLocalDate(input.todayDate);
  } catch {
    blockingReasons.push('todayDate는 유효한 YYYY-MM-DD 형식이어야 합니다.');
  }

  try {
    normalizedExam = parseLocalDate(input.examDate);
  } catch {
    blockingReasons.push('examDate는 유효한 YYYY-MM-DD 형식이어야 합니다.');
  }

  if (!Number.isInteger(finalReviewBufferDays) || finalReviewBufferDays < 0) {
    blockingReasons.push('finalReviewBufferDays는 0 이상 정수여야 합니다.');
  }
  if (!Number.isInteger(minEffectiveStudyDays) || minEffectiveStudyDays < 1) {
    blockingReasons.push('minEffectiveStudyDays는 1 이상 정수여야 합니다.');
  }

  let availableDaysForNewLearning = 0;
  let recommendedEarliestExamDate: LocalDate = input.todayDate;
  let lastReviewDate: LocalDate = input.examDate;

  if (normalizedToday && normalizedExam) {
    if (compareLocalDates(input.examDate, input.todayDate) <= 0) {
      blockingReasons.push('시험일은 오늘 이후로 설정해 주세요. 현재 선택한 날짜는 학습 및 복습 일정을 만들 수 없습니다.');
    }

    if (Number.isInteger(finalReviewBufferDays) && finalReviewBufferDays >= 0) {
      try {
        availableDaysForNewLearning = getAvailableDaysForNewLearning(input.todayDate, input.examDate, finalReviewBufferDays);
        lastReviewDate = getLastReviewDate(input.examDate, finalReviewBufferDays);
      } catch {
        availableDaysForNewLearning = 0;
      }
    }

    if (Number.isInteger(minEffectiveStudyDays) && minEffectiveStudyDays >= 1) {
      try {
        recommendedEarliestExamDate = getEarliestAllowedExamDate(
          input.todayDate,
          finalReviewBufferDays,
          minEffectiveStudyDays,
        );
      } catch {
        recommendedEarliestExamDate = input.todayDate;
      }
    }
  }

  const newLearningBlockingMessage = `현재 설정에서는 시험 전 정규 복습에 사용할 수 있는 기간이 ${availableDaysForNewLearning}일뿐입니다. 최소 ${minEffectiveStudyDays}일의 유효 학습기간과 시험 전 ${finalReviewBufferDays}일의 복습 버퍼가 필요합니다. 시험일을 ${recommendedEarliestExamDate} 이후로 설정해 주세요.`;
  if (availableDaysForNewLearning < minEffectiveStudyDays) {
    blockingReasons.push(newLearningBlockingMessage);
  }

  const blockingReason = blockingReasons.find((message) => message.includes('시험 전 정규 복습'))
    ?? blockingReasons.find((message) => message.includes('시험일은 오늘 이후로'))
    ?? blockingReasons[0];

  const isValid = blockingReasons.length === 0;

  return {
    isValid,
    blockingReason,
    blockingReasons,
    warnings,
    availableDaysForNewLearning,
    recommendedEarliestExamDate,
    recordsRequiringCramMode,
    earliestAllowedExamDate: recommendedEarliestExamDate,
    lastReviewDate,
    userMessage: isValid
      ? `시험일 ${input.examDate}은 허용됩니다. 사용 가능한 학습일 수는 ${availableDaysForNewLearning}일입니다.`
      : (blockingReason ?? '시험일 검증에 실패했습니다.'),
  };
}

export function validateExamDateAgainstExistingRecords(
  input: ExamDateValidationInput,
): ValidationResult {
  const effectiveConfig = createDefaultSchedulerConfig(input.config ?? {});
  const finalReviewBufferDays = typeof input.finalReviewBufferDays === 'number'
    ? input.finalReviewBufferDays
    : effectiveConfig.finalReviewBufferDays;
  const minEffectiveStudyDays = typeof input.minEffectiveStudyDays === 'number'
    ? input.minEffectiveStudyDays
    : effectiveConfig.minEffectiveStudyDays;

  const blockingReasons: string[] = [];
  const warnings: string[] = [];
  const records = Array.isArray(input.records) ? input.records : [];

  let availableDaysForNewLearning = 0;
  let recommendedEarliestExamDate: LocalDate = input.todayDate;
  let lastReviewDate: LocalDate = input.examDate;

  try {
    availableDaysForNewLearning = getAvailableDaysForNewLearning(input.todayDate, input.examDate, finalReviewBufferDays);
    recommendedEarliestExamDate = getEarliestAllowedExamDate(
      input.todayDate,
      finalReviewBufferDays,
      minEffectiveStudyDays,
    );
    lastReviewDate = getLastReviewDate(input.examDate, finalReviewBufferDays);
  } catch {
    availableDaysForNewLearning = 0;
    recommendedEarliestExamDate = input.todayDate;
    lastReviewDate = input.examDate;
  }

  const recordsRequiringCramMode = records
    .filter((record) => record && typeof record === 'object' && typeof record.id === 'string')
    .filter((record) => {
      if (!isValidLocalDate(record.studiedAt)) {
        return false;
      }
      return getEffectiveStudyDays(record.studiedAt, input.examDate, finalReviewBufferDays) < minEffectiveStudyDays;
    })
    .map((record) => record.id);

  const warningMessage = `등록된 학습기록 중 ${recordsRequiringCramMode.length}개 항목은 시험일까지 정규 분산복습 기준을 충족하지 못합니다. 해당 항목은 압축 복습 또는 벼락치기 모드로 표시됩니다. 시험일을 더 늦추면 더 안정적인 복습 일정을 만들 수 있습니다.`;
  const blockingMessage = `등록된 학습기록 중 ${recordsRequiringCramMode.length}개 항목의 복습 기간이 부족합니다. 현재 정책에서는 모든 항목이 최소 복습 기간을 확보해야 시험일을 설정할 수 있습니다.`;

  if (recordsRequiringCramMode.length > 0) {
    const shouldBlock = effectiveConfig.strictExistingRecordValidation || !effectiveConfig.allowExamDateWithCramRequiredRecords;
    if (shouldBlock) {
      blockingReasons.push(blockingMessage);
    } else {
      warnings.push(warningMessage);
    }
  }

  const blockingReason = blockingReasons[0];
  const isValid = blockingReasons.length === 0;

  return {
    isValid,
    blockingReason,
    blockingReasons,
    warnings,
    availableDaysForNewLearning,
    recommendedEarliestExamDate,
    recordsRequiringCramMode,
    earliestAllowedExamDate: recommendedEarliestExamDate,
    lastReviewDate,
    userMessage: isValid
      ? `시험일 ${input.examDate}은 허용됩니다. 사용 가능한 학습일 수는 ${availableDaysForNewLearning}일입니다.`
      : (blockingReason ?? warnings[0] ?? '시험일 검증에 실패했습니다.'),
  };
}

export function validateExamDate(
  todayDate: LocalDate,
  examDate: LocalDate,
  config: Partial<SchedulerConfig> = {},
): ValidationResult;
export function validateExamDate(input: ExamDateValidationInput): ValidationResult;
export function validateExamDate(
  todayDateOrInput: LocalDate | ExamDateValidationInput,
  examDate?: LocalDate,
  config: Partial<SchedulerConfig> = {},
): ValidationResult {
  const input: ExamDateValidationInput = typeof todayDateOrInput === 'string'
    ? { todayDate: todayDateOrInput, examDate: examDate ?? todayDateOrInput, config }
    : todayDateOrInput;

  const newLearningValidation = validateExamDateForNewLearning(input);
  const existingRecordsValidation = validateExamDateAgainstExistingRecords(input);

  const mergedBlockingReasons = Array.from(
    new Set([
      ...newLearningValidation.blockingReasons,
      ...existingRecordsValidation.blockingReasons,
    ]),
  );

  const mergedWarnings = Array.from(
    new Set([
      ...newLearningValidation.warnings,
      ...existingRecordsValidation.warnings,
    ]),
  );

  const mergedRecordsRequiringCramMode = Array.from(
    new Set([
      ...newLearningValidation.recordsRequiringCramMode,
      ...existingRecordsValidation.recordsRequiringCramMode,
    ]),
  );

  const blockingReason = newLearningValidation.blockingReason
    ?? existingRecordsValidation.blockingReason
    ?? undefined;

  const isValid = mergedBlockingReasons.length === 0;

  return {
    isValid,
    blockingReason,
    blockingReasons: mergedBlockingReasons,
    warnings: mergedWarnings,
    availableDaysForNewLearning: newLearningValidation.availableDaysForNewLearning,
    recommendedEarliestExamDate: newLearningValidation.recommendedEarliestExamDate,
    recordsRequiringCramMode: mergedRecordsRequiringCramMode,
    earliestAllowedExamDate: newLearningValidation.earliestAllowedExamDate,
    lastReviewDate: newLearningValidation.lastReviewDate,
    userMessage: isValid
      ? `시험일 ${input.examDate}은 허용됩니다. 사용 가능한 학습일 수는 ${newLearningValidation.availableDaysForNewLearning}일입니다.`
      : (blockingReason ?? mergedWarnings[0] ?? '시험일 검증에 실패했습니다.'),
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

export function calculatePriorityScore(input: {
  importance: Importance;
  difficulty: Difficulty;
  initialMastery: 1 | 2 | 3 | 4 | 5;
  dueDaysBeforeExam: number;
  isFinalReview: boolean;
  reviewIndex: number;
  totalReviewCount: number;
}): PriorityScoreResult;
export function calculatePriorityScore(
  schedule: ReviewSchedule,
  record: StudyRecord,
  exam: Exam,
  config?: Partial<SchedulerConfig>,
): number;
export function calculatePriorityScore(
  inputOrSchedule: {
    importance: Importance;
    difficulty: Difficulty;
    initialMastery: 1 | 2 | 3 | 4 | 5;
    dueDaysBeforeExam: number;
    isFinalReview: boolean;
    reviewIndex: number;
    totalReviewCount: number;
  } | ReviewSchedule,
  record?: StudyRecord,
  exam?: Exam,
  config: Partial<SchedulerConfig> = {},
): PriorityScoreResult | number {
  if (record && exam && 'id' in inputOrSchedule) {
    const schedule = inputOrSchedule as ReviewSchedule;
    const effective = createDefaultSchedulerConfig(config);
    const scoring = getDefaultPriorityScoringConfig(effective);
    const dueDaysBeforeExam = Math.max(0, schedule.dueDaysBeforeExam);
    const importance = scoring.importance[record.importance] ?? scoring.importance.normal;
    const difficulty = scoring.difficulty[record.difficulty] ?? scoring.difficulty.medium;
    const masteryScore = scoring.mastery[record.initialMastery] ?? scoring.mastery[3];
    return Math.max(0, scoring.urgencyBase - dueDaysBeforeExam)
      + importance
      + difficulty
      + masteryScore
      + (schedule.isFinalReview ? scoring.finalReviewBonus : 0)
      + (schedule.reviewIndex === 0 ? scoring.firstReviewBonus : 0)
      + scoring.overdueBonus;
  }

  const input = inputOrSchedule as {
    importance: Importance;
    difficulty: Difficulty;
    initialMastery: 1 | 2 | 3 | 4 | 5;
    dueDaysBeforeExam: number;
    isFinalReview: boolean;
    reviewIndex: number;
    totalReviewCount: number;
  };
  const proximityCap = 30;
  const dueDaysBeforeExam = Math.max(0, Math.floor(input.dueDaysBeforeExam));
  const breakdown: PriorityScoreBreakdown = {
    importance: { high: 40, normal: 20, low: 0 }[input.importance],
    difficulty: { hard: 25, medium: 10, easy: 0 }[input.difficulty],
    initialMastery: { 1: 25, 2: 18, 3: 10, 4: 4, 5: 0 }[input.initialMastery],
    examProximity: Math.max(0, Math.min(proximityCap, 30 - dueDaysBeforeExam)),
    finalReview: input.isFinalReview ? 30 : 0,
    reviewPosition: input.totalReviewCount > 0 && input.reviewIndex === input.totalReviewCount - 1 ? 2 : 0,
    dueDaysBeforeExam,
    proximityCap,
  };
  return {
    score: breakdown.importance
      + breakdown.difficulty
      + breakdown.initialMastery
      + breakdown.examProximity
      + breakdown.finalReview
      + breakdown.reviewPosition,
    breakdown,
  };
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
  const ordered = [...schedules].sort((a, b) => {
    const dateOrder = compareLocalDates(a.scheduledDate, b.scheduledDate);
    return dateOrder !== 0 ? dateOrder : a.id.localeCompare(b.id);
  });

  for (const schedule of ordered) {
    const bucket = grouped.get(schedule.scheduledDate) ?? [];
    bucket.push(schedule);
    grouped.set(schedule.scheduledDate, bucket);
  }

  return grouped;
}

export function calculateDailyReviewLoad(
  schedules: ReviewSchedule[],
  maxDailyReviewMinutes: number,
): DailyReviewLoad[] {
  if (!Number.isFinite(maxDailyReviewMinutes) || maxDailyReviewMinutes < 0) {
    throw new RangeError('maxDailyReviewMinutes는 0 이상 유한수여야 합니다.');
  }

  return Array.from(groupSchedulesByDate(schedules).entries()).map(([scheduledDate, grouped]) => {
    const totalEstimatedReviewMinutes = grouped.reduce(
      (total, schedule) => {
        if (!Number.isFinite(schedule.estimatedReviewMinutes) || schedule.estimatedReviewMinutes < 0) {
          throw new RangeError(`estimatedReviewMinutes는 유한하고 0 이상이어야 합니다: ${schedule.id}`);
        }
        return total + schedule.estimatedReviewMinutes;
      },
      0,
    );
    return {
      scheduledDate,
      scheduleCount: grouped.length,
      totalEstimatedReviewMinutes,
      maxDailyReviewMinutes,
      isOverloaded: totalEstimatedReviewMinutes > maxDailyReviewMinutes,
      overloadMinutes: Math.max(0, totalEstimatedReviewMinutes - maxDailyReviewMinutes),
      scheduleIds: grouped.map((schedule) => schedule.id),
    };
  });
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

export function validateRecordSpacingAfterMove(
  schedule: ReviewSchedule,
  candidateDate: LocalDate,
  schedules: ReviewSchedule[],
): boolean {
  const sameRecordDates = schedules
    .filter((item) => item.id !== schedule.id && item.studyRecordId === schedule.studyRecordId)
    .map((item) => item.scheduledDate)
    .sort(compareLocalDates);

  return sameRecordDates.every((date) => Math.abs(daysBetween(date, candidateDate)) >= 2);
}

export function findRescheduleCandidateDate(
  schedule: ReviewSchedule,
  schedules: ReviewSchedule[],
  exam: Exam,
  config: Partial<SchedulerConfig> = {},
): LocalDate | null {
  const effectiveConfig = createDefaultSchedulerConfig(config);
  const offsets = [-1, 1, -2, 2] as const;
  for (const offset of offsets) {
    const candidateDate = addDays(schedule.scheduledDate, offset);
    const currentLoad = calculateDailyLoad(schedules);
    const sourceLoad = currentLoad.get(schedule.scheduledDate) ?? 0;
    const candidateLoad = currentLoad.get(candidateDate) ?? 0;
    const finalBufferDays = Number.isInteger(exam.finalReviewBufferDays)
      ? exam.finalReviewBufferDays
      : effectiveConfig.finalReviewBufferDays;
    const lastReviewDate = schedule.lastReviewDate ?? getLastReviewDate(exam.examDate, finalBufferDays);
    const reviewStartDate = schedule.reviewStartDate ?? getReviewStartDate(schedule.scheduledDate);
    const candidateInWindow = candidateDate >= reviewStartDate && candidateDate <= lastReviewDate;
    const inBuffer = candidateDate >= addDays(exam.examDate, -finalBufferDays)
      && candidateDate < exam.examDate;
    const preservesFinal = !schedule.isFinalReview
      || candidateDate < lastReviewDate;

    if (
      candidateInWindow
      && candidateDate !== exam.examDate
      && !inBuffer
      && preservesFinal
      && sourceLoad > effectiveConfig.maxDailyReviewMinutes
      && candidateLoad + schedule.estimatedReviewMinutes <= effectiveConfig.maxDailyReviewMinutes
      && validateRecordSpacingAfterMove(schedule, candidateDate, schedules)
    ) {
      return candidateDate;
    }
  }

  return null;
}

export function buildOverloadWarningMessage(
  date: LocalDate,
  totalMinutes: number,
  maxDailyReviewMinutes: number,
): string {
  return `${date}의 예상 복습량이 ${totalMinutes}분으로 설정 한도 ${maxDailyReviewMinutes}분을 초과합니다. 하루 복습 한도를 늘리거나 시험일을 조정해 주세요.`;
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

function isReviewOutcomeRescheduleInput(
  input: ReviewOutcomeRescheduleInput | RescheduleAfterOutcomeInput,
): input is ReviewOutcomeRescheduleInput {
  return 'completedReview' in input && 'allSchedules' in input && 'studyRecord' in input;
}

function isEligibleOutcomeSchedule(
  schedule: ReviewSchedule,
  recordId: string,
  today: LocalDate,
): boolean {
  return schedule.studyRecordId === recordId
    && schedule.status !== 'COMPLETED'
    && schedule.scheduledDate >= today;
}

function isLegalOutcomeDate(
  candidateDate: LocalDate,
  schedule: ReviewSchedule,
  schedules: ReviewSchedule[],
  exam: Exam,
  today: LocalDate,
  finalReviewBufferDays: number,
  excludedScheduleId: string,
): boolean {
  const lastReviewDate = schedule.lastReviewDate
    ?? getLastReviewDate(exam.examDate, finalReviewBufferDays);
  const reviewStartDate = schedule.reviewStartDate
    ?? getReviewStartDate(today);
  if (candidateDate < today || candidateDate < reviewStartDate || candidateDate > lastReviewDate) {
    return false;
  }
  if (candidateDate >= addDays(exam.examDate, -finalReviewBufferDays)) {
    return false;
  }
  if (candidateDate === exam.examDate) {
    return false;
  }
  return validateRecordSpacingAfterMove(schedule, candidateDate, schedules)
    && schedules.every((other) => other.id === excludedScheduleId || other.scheduledDate !== candidateDate || other.studyRecordId !== schedule.studyRecordId);
}

function getOutcomeFutureSchedules(
  input: ReviewOutcomeRescheduleInput,
  schedules: ReviewSchedule[],
): ReviewSchedule[] {
  return schedules
    .filter((schedule) => isEligibleOutcomeSchedule(schedule, input.studyRecord.id, input.today))
    .filter((schedule) => schedule.id !== input.completedReview.id)
    .sort((a, b) => {
      const dateOrder = compareLocalDates(a.scheduledDate, b.scheduledDate);
      return dateOrder !== 0 ? dateOrder : a.id.localeCompare(b.id);
    });
}

function createOutcomeReinforcementSchedule(
  input: ReviewOutcomeRescheduleInput,
  schedules: ReviewSchedule[],
  reviewStartDate: LocalDate,
  lastReviewDate: LocalDate,
): ReviewSchedule | null {
  const candidates = getReviewWindowDates(reviewStartDate, lastReviewDate)
    .filter((date) => date > input.today)
    .filter((date) => isSameRecordScheduleDateAllowed(
      date,
      input.studyRecord.id,
      schedules,
      input.exam,
      input.config.finalReviewBufferDays,
    ));
  const scheduledDate = candidates[0];
  if (!scheduledDate) {
    return null;
  }
  const sameRecord = schedules.filter((schedule) => schedule.studyRecordId === input.studyRecord.id);
  const reviewIndex = Math.max(-1, ...sameRecord.map((schedule) => schedule.reviewIndex)) + 1;
  const id = `${input.studyRecord.id}-outcome-failed-${input.completedReview.id}`;
  return {
    id,
    examId: input.exam.id,
    studyRecordId: input.studyRecord.id,
    reviewIndex,
    scheduledDate,
    originalScheduledDate: scheduledDate,
    status: 'RESCHEDULED',
    priorityScore: 0,
    estimatedReviewMinutes: input.studyRecord.estimatedReviewMinutes,
    recommendedMethod: getRecommendedMethod(reviewIndex + 1, reviewIndex + 1, false),
    notificationPlan: createNotificationPlan(scheduledDate, false, input.config),
    dueDaysBeforeExam: daysBetween(scheduledDate, input.exam.examDate),
    isFinalReview: false,
    rescheduleReason: 'OUTCOME_FAILED_REINFORCEMENT',
    reviewStartDate,
    lastReviewDate,
    createdAt: input.today,
    updatedAt: input.today,
  };
}

function rescheduleAfterReviewOutcomeV2(
  input: ReviewOutcomeRescheduleInput,
): ReviewOutcomeRescheduleResult {
  const sourceSchedules = input.allSchedules.map((schedule) => cloneSchedule(schedule));
  const completedIndex = sourceSchedules.findIndex((schedule) => schedule.id === input.completedReview.id);
  const warnings: string[] = [];
  if (completedIndex < 0) {
    return {
      updatedSchedules: sourceSchedules,
      changedScheduleIds: [],
      warnings: ['완료할 복습 일정을 찾을 수 없습니다.'],
      reason: 'INVALID_REVIEW',
      notificationRescheduleRequests: [],
    };
  }
  const completed = sourceSchedules[completedIndex];
  if (completed.examId !== input.exam.id || completed.studyRecordId !== input.studyRecord.id) {
    return {
      updatedSchedules: sourceSchedules,
      changedScheduleIds: [],
      warnings: ['복습 일정이 시험 또는 학습기록과 일치하지 않습니다.'],
      reason: 'INVALID_CONTEXT',
      notificationRescheduleRequests: [],
    };
  }
  if (completed.status === 'COMPLETED') {
    return {
      updatedSchedules: sourceSchedules,
      changedScheduleIds: [],
      warnings: ['이미 완료된 복습 일정은 다시 완료 처리할 수 없습니다.'],
      reason: 'ALREADY_COMPLETED',
      notificationRescheduleRequests: [],
    };
  }

  completed.status = 'COMPLETED';
  completed.outcome = input.outcome;
  completed.completedAt = input.today;
  const beforeById = new Map(
    sourceSchedules.map((schedule) => [schedule.id, cloneSchedule(schedule)]),
  );
  const future = getOutcomeFutureSchedules(input, sourceSchedules);
  const reviewStartDate = completed.reviewStartDate
    ?? getReviewStartDate(input.studyRecord.studiedAt);
  const lastReviewDate = completed.lastReviewDate
    ?? getLastReviewDate(input.exam.examDate, input.config.finalReviewBufferDays);
  const target = future[0];
  let outcomeChanged = false;
  let reason = `OUTCOME_${input.outcome}`;

  if (input.outcome !== 'SUCCESS' && target) {
    const offsets: number[] = input.outcome === 'EASY'
      ? [Math.max(1, Math.round(daysBetween(completed.scheduledDate, target.scheduledDate) * 0.2))]
      : input.outcome === 'HARD' ? [-1] : [-2, -1];
    for (const offset of offsets) {
      const candidate = addDays(target.scheduledDate, offset);
      if (!isLegalOutcomeDate(candidate, target, sourceSchedules, input.exam, input.today, input.config.finalReviewBufferDays, target.id)) {
        continue;
      }
      target.scheduledDate = candidate;
      target.status = 'RESCHEDULED';
      target.rescheduleReason = input.outcome === 'EASY'
        ? 'OUTCOME_EASY_DELAYED'
        : input.outcome === 'HARD' ? 'OUTCOME_HARD_ADVANCED' : 'OUTCOME_FAILED_ADVANCED';
      target.dueDaysBeforeExam = daysBetween(candidate, input.exam.examDate);
      target.notificationPlan = createNotificationPlan(candidate, target.isFinalReview, input.config);
      target.updatedAt = input.today;
      outcomeChanged = true;
      reason = target.rescheduleReason;
      break;
    }
    if (!outcomeChanged) {
      warnings.push(`${input.outcome} 결과로 조정할 수 있는 합법적인 미래 복습 날짜가 없습니다.`);
    }
  } else if (input.outcome === 'FAILED' && !target) {
    const reinforcement = createOutcomeReinforcementSchedule(input, sourceSchedules, reviewStartDate, lastReviewDate);
    if (reinforcement) {
      sourceSchedules.push(reinforcement);
      outcomeChanged = true;
      reason = 'OUTCOME_FAILED_REINFORCEMENT';
    } else {
      warnings.push('FAILED 결과를 보강할 수 있는 합법적인 복습 날짜가 없습니다.');
    }
  } else if (input.outcome === 'SUCCESS') {
    reason = 'OUTCOME_SUCCESS_UNCHANGED';
  }

  const eligibleForRebalance = sourceSchedules.filter((schedule) =>
    schedule.status !== 'COMPLETED' && schedule.scheduledDate >= input.today);
  const rebalanceResult = rebalanceDailyLoad({
    schedules: eligibleForRebalance,
    exam: input.exam,
    finalReviewBufferDays: input.config.finalReviewBufferDays,
    maxDailyReviewMinutes: input.config.maxDailyReviewMinutes,
  });
  const rebalanceById = new Map(rebalanceResult.schedules.map((schedule) => [schedule.id, schedule]));
  for (let index = 0; index < sourceSchedules.length; index += 1) {
    const replacement = rebalanceById.get(sourceSchedules[index].id);
    if (replacement) {
      sourceSchedules[index] = replacement;
    }
  }
  warnings.push(...rebalanceResult.warnings);

  const changedScheduleIds: string[] = [];
  const notificationRescheduleRequests: NotificationRescheduleRequest[] = [];
  for (const schedule of sourceSchedules) {
    const before = beforeById.get(schedule.id);
    const dateChanged = before !== undefined && before.scheduledDate !== schedule.scheduledDate;
    const created = before === undefined;
    if (dateChanged || created) {
      if (dateChanged) {
        schedule.notificationPlan = createNotificationPlan(schedule.scheduledDate, schedule.isFinalReview, input.config);
        schedule.dueDaysBeforeExam = daysBetween(schedule.scheduledDate, input.exam.examDate);
      }
      if (schedule.status !== 'COMPLETED') {
        changedScheduleIds.push(schedule.id);
        notificationRescheduleRequests.push({
          reviewScheduleId: schedule.id,
          studyRecordId: schedule.studyRecordId,
          action: created ? 'CREATE' : 'CANCEL_AND_RESCHEDULE',
          previousScheduledDate: before?.scheduledDate,
          scheduledDate: schedule.scheduledDate,
          notificationPlan: schedule.notificationPlan,
          reason: schedule.rescheduleReason ?? reason,
        });
      }
    }
  }
  return {
    updatedSchedules: sourceSchedules,
    changedScheduleIds: Array.from(new Set(changedScheduleIds)),
    warnings: Array.from(new Set(warnings)),
    reason,
    notificationRescheduleRequests,
  };
}

export function rescheduleAfterReviewOutcome(
  input: ReviewOutcomeRescheduleInput,
): ReviewOutcomeRescheduleResult;
export function rescheduleAfterReviewOutcome(
  input: RescheduleAfterOutcomeInput,
): RescheduleAfterOutcomeResult;
export function rescheduleAfterReviewOutcome(
  input: ReviewOutcomeRescheduleInput | RescheduleAfterOutcomeInput,
): ReviewOutcomeRescheduleResult | RescheduleAfterOutcomeResult {
  if (isReviewOutcomeRescheduleInput(input)) {
    return rescheduleAfterReviewOutcomeV2(input);
  }
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

export function completeReviewSchedule(input: {
  reviewId: string;
  outcome: ReviewOutcome;
  completedAt: LocalDate;
  allSchedules: ReviewSchedule[];
  studyRecord: StudyRecord;
  exam: Exam;
  config: SchedulerConfig;
}): CompleteReviewScheduleResult {
  const review = input.allSchedules.find((schedule) => schedule.id === input.reviewId);
  if (!review) {
    throw new Error(`복습 일정을 찾을 수 없습니다: ${input.reviewId}`);
  }
  if (review.studyRecordId !== input.studyRecord.id || review.examId !== input.exam.id) {
    throw new Error('복습 일정이 지정된 학습기록 또는 시험에 속하지 않습니다.');
  }
  if (review.status === 'COMPLETED') {
    throw new Error('이미 완료된 복습 일정은 다시 완료 처리할 수 없습니다.');
  }
  if (review.scheduledDate > input.completedAt) {
    throw new Error('아직 도래하지 않은 복습 일정은 완료 처리할 수 없습니다.');
  }

  const rescheduling = rescheduleAfterReviewOutcome({
    completedReview: review,
    outcome: input.outcome,
    today: input.completedAt,
    allSchedules: input.allSchedules,
    studyRecord: input.studyRecord,
    exam: input.exam,
    config: input.config,
  });
  const completedSchedule = rescheduling.updatedSchedules.find((schedule) => schedule.id === input.reviewId);
  if (!completedSchedule) {
    throw new Error('완료 처리된 복습 일정이 결과에 없습니다.');
  }
  return { completedSchedule, rescheduling };
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
  input: RebalanceDailyLoadInput,
): RebalanceDailyLoadResult;
export function rebalanceDailyLoad(
  schedules: ReviewSchedule[],
  exam: Exam,
  config?: Partial<SchedulerConfig>,
): {
  schedules: ReviewSchedule[];
  warnings: SchedulingWarning[];
  unresolvedOverloads: UnresolvedOverload[];
};
export function rebalanceDailyLoad(
  inputOrSchedules: RebalanceDailyLoadInput | ReviewSchedule[],
  examArgument?: Exam,
  config: Partial<SchedulerConfig> = {},
): RebalanceDailyLoadResult | {
  schedules: ReviewSchedule[];
  warnings: SchedulingWarning[];
  unresolvedOverloads: UnresolvedOverload[];
} {
  const legacyCall = Array.isArray(inputOrSchedules);
  const input: RebalanceDailyLoadInput = legacyCall
    ? { schedules: inputOrSchedules, exam: examArgument as Exam, ...config }
    : inputOrSchedules;
  const effectiveConfig = createDefaultSchedulerConfig({
    ...config,
    finalReviewBufferDays: input.finalReviewBufferDays ?? config.finalReviewBufferDays,
    maxDailyReviewMinutes: input.maxDailyReviewMinutes ?? config.maxDailyReviewMinutes,
  });
  const sourceSchedules = input.schedules
    ?? input.bundles?.flatMap((bundle) => bundle.schedules)
    ?? [];
  const working = sourceSchedules
    .map((schedule) => cloneSchedule(schedule))
    .sort((a, b) => a.id.localeCompare(b.id));
  const movedScheduleIds = new Set<string>();
  const moves: RebalanceMove[] = [];

  const scoreForSchedule = (schedule: ReviewSchedule): PriorityScoreResult => {
    const record = input.bundles?.find((bundle) => bundle.schedules.some((item) => item.id === schedule.id))?.record;
    return calculatePriorityScore({
      importance: record?.importance ?? 'normal',
      difficulty: record?.difficulty ?? 'medium',
      initialMastery: record?.initialMastery ?? 3,
      dueDaysBeforeExam: schedule.dueDaysBeforeExam,
      isFinalReview: schedule.isFinalReview,
      reviewIndex: schedule.reviewIndex,
      totalReviewCount: input.bundles?.find((bundle) => bundle.record.id === schedule.studyRecordId)?.schedules.length ?? 1,
    });
  };

  // 이동 우선순위: 낮은 점수, 비최종 복습, 시험에서 먼 날짜, 이른 reviewIndex, ID 순이다.
  const compareForMove = (a: ReviewSchedule, b: ReviewSchedule): number => {
    const aScore = scoreForSchedule(a);
    const bScore = scoreForSchedule(b);
    if (aScore.score !== bScore.score) return aScore.score - bScore.score;
    if (a.isFinalReview !== b.isFinalReview) return Number(a.isFinalReview) - Number(b.isFinalReview);
    if (a.dueDaysBeforeExam !== b.dueDaysBeforeExam) return b.dueDaysBeforeExam - a.dueDaysBeforeExam;
    if (a.reviewIndex !== b.reviewIndex) return a.reviewIndex - b.reviewIndex;
    return a.id.localeCompare(b.id);
  };

  const maxPasses = Math.max(1, working.length * 2);
  for (let pass = 0; pass < maxPasses; pass += 1) {
    const overloadedLoads = calculateDailyReviewLoad(working, effectiveConfig.maxDailyReviewMinutes)
      .filter((load) => load.isOverloaded)
      .sort((a, b) => compareLocalDates(a.scheduledDate, b.scheduledDate));
    if (overloadedLoads.length === 0) break;

    let moved = false;
    for (const load of overloadedLoads) {
      const candidates = working
        .filter((schedule) => schedule.scheduledDate === load.scheduledDate)
        .filter((schedule) => !movedScheduleIds.has(schedule.id))
        .sort(compareForMove);

      for (const schedule of candidates) {
        const candidateDate = findRescheduleCandidateDate(
          schedule,
          working,
          input.exam,
          effectiveConfig,
        );
        if (!candidateDate) continue;

        const originalDate = schedule.scheduledDate;
        const originalLoad = load.totalEstimatedReviewMinutes;
        const newDueDaysBeforeExam = daysBetween(candidateDate, input.exam.examDate);
        const nextPriority = calculatePriorityScore({
          importance: input.bundles?.find((bundle) => bundle.record.id === schedule.studyRecordId)?.record.importance ?? 'normal',
          difficulty: input.bundles?.find((bundle) => bundle.record.id === schedule.studyRecordId)?.record.difficulty ?? 'medium',
          initialMastery: input.bundles?.find((bundle) => bundle.record.id === schedule.studyRecordId)?.record.initialMastery ?? 3,
          dueDaysBeforeExam: newDueDaysBeforeExam,
          isFinalReview: schedule.isFinalReview,
          reviewIndex: schedule.reviewIndex,
          totalReviewCount: input.bundles?.find((bundle) => bundle.record.id === schedule.studyRecordId)?.schedules.length ?? 1,
        });
        const reason = `일일 복습량 ${originalLoad}분이 설정 한도 ${effectiveConfig.maxDailyReviewMinutes}분을 초과하여 ${originalDate}에서 ${candidateDate}로 자동 조정됨`;
        const index = working.findIndex((item) => item.id === schedule.id);
        working[index] = {
          ...working[index],
          scheduledDate: candidateDate,
          status: 'RESCHEDULED',
          rescheduleReason: reason,
          dueDaysBeforeExam: newDueDaysBeforeExam,
          priorityScore: nextPriority.score,
        };
        movedScheduleIds.add(schedule.id);
        moves.push({
          scheduleId: schedule.id,
          originalDate,
          newDate: candidateDate,
          originalLoad,
          priority: nextPriority,
          reason,
        });
        moved = true;
        break;
      }
      if (moved) break;
    }
    if (!moved) break;
  }

  const finalLoads = calculateDailyReviewLoad(working, effectiveConfig.maxDailyReviewMinutes);
  const unresolvedOverloadDates = finalLoads
    .filter((load) => load.isOverloaded)
    .map((load) => load.scheduledDate);
  const warnings = unresolvedOverloadDates.map((date) => {
    const load = finalLoads.find((item) => item.scheduledDate === date);
    return buildOverloadWarningMessage(
      date,
      load?.totalEstimatedReviewMinutes ?? 0,
      effectiveConfig.maxDailyReviewMinutes,
    );
  });
  const unresolvedOverloads: UnresolvedOverload[] = unresolvedOverloadDates.map((date) => {
    const affected = working.filter((schedule) => schedule.scheduledDate === date);
    const total = affected.reduce((sum, schedule) => sum + schedule.estimatedReviewMinutes, 0);
    const warning = buildOverloadWarning(date, affected, 'DAILY_REVIEW_LIMIT_EXCEEDED', effectiveConfig);
    for (const schedule of affected) {
      schedule.status = 'OVERLOADED_UNRESOLVED';
    }
    return {
      ...warning,
      excessMinutes: Math.max(0, total - effectiveConfig.maxDailyReviewMinutes),
      message: buildOverloadWarningMessage(date, total, effectiveConfig.maxDailyReviewMinutes),
      status: 'OVERLOADED_UNRESOLVED',
    };
  });
  const result: RebalanceDailyLoadResult = {
    schedules: working,
    dailyLoads: finalLoads,
    warnings,
    unresolvedOverloadDates,
    moves,
    unresolvedOverloads,
  };

  if (legacyCall) {
    return {
      schedules: result.schedules,
      warnings: result.unresolvedOverloads,
      unresolvedOverloads: result.unresolvedOverloads,
    };
  }
  return result;
}

export function buildDistributedRebalanceExample(): {
  exam: Exam;
  bundles: StudyRecordScheduleBundle[];
  originalSchedules: ReviewSchedule[];
  originalDailyLoads: DailyReviewLoad[];
  result: RebalanceDailyLoadResult;
} {
  const exam: Exam = {
    id: 'exam-example',
    examDate: '2026-11-15' as LocalDate,
    timezone: DEFAULT_TIMEZONE,
    finalReviewBufferDays: 1,
    maxDailyReviewMinutes: 30,
    allowRegularReviewOnDayBeforeExam: false,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };
  const records: StudyRecord[] = [
    {
      id: 'A',
      examId: exam.id,
      title: '기록 A',
      content: '기록 A',
      studiedAt: '2026-10-01' as LocalDate,
      difficulty: 'medium',
      importance: 'normal',
      initialMastery: 3,
      estimatedReviewMinutes: 15,
      isCompleted: false,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    {
      id: 'B',
      examId: exam.id,
      title: '기록 B',
      content: '기록 B',
      studiedAt: '2026-10-08' as LocalDate,
      difficulty: 'hard',
      importance: 'high',
      initialMastery: 2,
      estimatedReviewMinutes: 20,
      isCompleted: false,
      createdAt: '2026-10-08' as LocalDate,
      updatedAt: '2026-10-08' as LocalDate,
    },
    {
      id: 'C',
      examId: exam.id,
      title: '기록 C',
      content: '기록 C',
      studiedAt: '2026-10-25' as LocalDate,
      difficulty: 'easy',
      importance: 'normal',
      initialMastery: 4,
      estimatedReviewMinutes: 15,
      isCompleted: false,
      createdAt: '2026-10-25' as LocalDate,
      updatedAt: '2026-10-25' as LocalDate,
    },
  ];
  const bundles = records.map((record) => ({
    record,
    ...createReviewSchedulesForStudyRecord({
      record,
      examDate: exam.examDate,
      finalReviewBufferDays: exam.finalReviewBufferDays,
      config: {
        ...DEFAULT_SCHEDULER_CONFIG,
        maxDailyReviewMinutes: exam.maxDailyReviewMinutes,
      },
    }),
  }));
  const originalSchedules = bundles.flatMap((bundle) => bundle.schedules);
  const result = rebalanceDailyLoad({
    bundles,
    exam,
    finalReviewBufferDays: exam.finalReviewBufferDays,
    maxDailyReviewMinutes: exam.maxDailyReviewMinutes,
  });
  return {
    exam,
    bundles,
    originalSchedules,
    originalDailyLoads: calculateDailyReviewLoad(originalSchedules, exam.maxDailyReviewMinutes),
    result,
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

export function runDailyLoadRebalancingSelfChecks(): void {
  const exam = {
    id: 'rebalance-check',
    examDate: '2026-11-15' as LocalDate,
    timezone: DEFAULT_TIMEZONE,
    finalReviewBufferDays: 1,
    maxDailyReviewMinutes: 30,
    allowRegularReviewOnDayBeforeExam: false,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };
  const createRecord = (
    id: string,
    importance: Importance,
    difficulty: Difficulty,
    initialMastery: 1 | 2 | 3 | 4 | 5,
  ): StudyRecord => ({
    id,
    examId: exam.id,
    title: id,
    content: id,
    studiedAt: '2026-10-01' as LocalDate,
    importance,
    difficulty,
    initialMastery,
    estimatedReviewMinutes: 15,
    isCompleted: false,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  });
  const createSchedule = (
    id: string,
    record: StudyRecord,
    date: LocalDate,
    minutes: number,
    isFinalReview = false,
  ): ReviewSchedule => ({
    id,
    examId: exam.id,
    studyRecordId: record.id,
    reviewIndex: 0,
    scheduledDate: date,
    originalScheduledDate: date,
    reviewStartDate: '2026-10-01' as LocalDate,
    lastReviewDate: '2026-11-13' as LocalDate,
    status: 'SCHEDULED',
    priorityScore: 0,
    estimatedReviewMinutes: minutes,
    recommendedMethod: '회상',
    notificationPlan: { kind: 'IN_APP', hour: 9, reminder: false },
    dueDaysBeforeExam: daysBetween(date, exam.examDate),
    isFinalReview,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  });

  const highRecord = createRecord('high', 'high', 'hard', 1);
  const lowRecord = createRecord('low', 'low', 'easy', 5);
  const highSchedule = createSchedule('high-schedule', highRecord, '2026-10-10' as LocalDate, 20);
  const lowSchedule = createSchedule('low-schedule', lowRecord, '2026-10-10' as LocalDate, 15);
  const bundles: StudyRecordScheduleBundle[] = [
    { record: highRecord, schedules: [highSchedule] },
    { record: lowRecord, schedules: [lowSchedule] },
  ];
  const inputSnapshot = JSON.stringify(bundles);
  const rebalanced = rebalanceDailyLoad({
    bundles,
    exam,
    maxDailyReviewMinutes: 30,
  });
  assert(JSON.stringify(bundles) === inputSnapshot, '재배치는 입력 스케줄을 변경하면 안 됩니다.');
  assert(rebalanced.moves[0]?.scheduleId === 'low-schedule', '낮은 우선순위 일정이 먼저 이동해야 합니다.');
  assert(rebalanced.unresolvedOverloadDates.length === 0, '이동 가능한 과부하는 해결되어야 합니다.');

  const noOverload = rebalanceDailyLoad({
    schedules: [createSchedule('no-overload', highRecord, '2026-10-10' as LocalDate, 10)],
    exam,
    maxDailyReviewMinutes: 30,
  });
  assert(noOverload.moves.length === 0 && noOverload.warnings.length === 0, '과부하가 없으면 이동하지 않아야 합니다.');

  const unresolved = rebalanceDailyLoad({
    schedules: [
      { ...createSchedule('unresolved-a', highRecord, '2026-10-10' as LocalDate, 20), reviewStartDate: '2026-10-10' as LocalDate, lastReviewDate: '2026-10-10' as LocalDate },
      { ...createSchedule('unresolved-b', lowRecord, '2026-10-10' as LocalDate, 20), reviewStartDate: '2026-10-10' as LocalDate, lastReviewDate: '2026-10-10' as LocalDate },
    ],
    exam,
    maxDailyReviewMinutes: 30,
  });
  assert(unresolved.unresolvedOverloadDates[0] === '2026-10-10' as LocalDate, '해결할 수 없는 날짜를 기록해야 합니다.');
  assert(unresolved.schedules.every((schedule) => schedule.status === 'OVERLOADED_UNRESOLVED'), '미해결 일정은 명시적 상태를 가져야 합니다.');
  assert(
    unresolved.warnings[0] === '2026-10-10의 예상 복습량이 40분으로 설정 한도 30분을 초과합니다. 하루 복습 한도를 늘리거나 시험일을 조정해 주세요.',
    '미해결 과부하 경고 문구가 정확해야 합니다.',
  );

  const example = buildDistributedRebalanceExample();
  assert(example.originalSchedules.length === example.result.schedules.length, '통합 예시는 일정을 삭제하면 안 됩니다.');
  assert(example.result.warnings.length === 0, '통합 예시는 현재 한도에서 과부하를 해결해야 합니다.');
}

export function runReviewOutcomeSelfChecks(): void {
  const exam: Exam = {
    id: 'outcome-check',
    examDate: '2026-11-15' as LocalDate,
    timezone: DEFAULT_TIMEZONE,
    finalReviewBufferDays: 1,
    maxDailyReviewMinutes: 30,
    allowRegularReviewOnDayBeforeExam: false,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };
  const config = {
    ...DEFAULT_SCHEDULER_CONFIG,
    finalReviewBufferDays: 1,
    maxDailyReviewMinutes: 30,
  };
  const record: StudyRecord = {
    id: 'outcome-record',
    examId: exam.id,
    title: '결과 점검',
    content: '결과 점검',
    studiedAt: '2026-10-01' as LocalDate,
    difficulty: 'medium',
    importance: 'normal',
    initialMastery: 3,
    estimatedReviewMinutes: 10,
    isCompleted: false,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  };
  const makeSchedule = (
    id: string,
    date: LocalDate,
    index: number,
  ): ReviewSchedule => ({
    id,
    examId: exam.id,
    studyRecordId: record.id,
    reviewIndex: index,
    scheduledDate: date,
    originalScheduledDate: date,
    status: 'SCHEDULED',
    priorityScore: 0,
    estimatedReviewMinutes: 10,
    recommendedMethod: '회상',
    notificationPlan: { kind: 'IN_APP', hour: 9, reminder: false },
    dueDaysBeforeExam: daysBetween(date, exam.examDate),
    isFinalReview: false,
    reviewStartDate: '2026-10-02' as LocalDate,
    lastReviewDate: '2026-11-13' as LocalDate,
    createdAt: '2026-10-01' as LocalDate,
    updatedAt: '2026-10-01' as LocalDate,
  });

  const hardSchedules = [
    makeSchedule('outcome-completed', '2026-10-10' as LocalDate, 0),
    makeSchedule('outcome-next', '2026-10-15' as LocalDate, 1),
  ];
  const hard = rescheduleAfterReviewOutcome({
    completedReview: hardSchedules[0],
    outcome: 'HARD',
    today: '2026-10-10' as LocalDate,
    allSchedules: hardSchedules,
    studyRecord: record,
    exam,
    config,
  });
  assert(hard.updatedSchedules[0].status === 'COMPLETED', '완료 일정은 COMPLETED가 되어야 합니다.');
  assert(hard.updatedSchedules[0].outcome === 'HARD', '완료 결과를 보존해야 합니다.');
  assert(hard.updatedSchedules[1].scheduledDate === '2026-10-14' as LocalDate, 'HARD는 다음 일정을 하루 앞당겨야 합니다.');
  assert(hard.notificationRescheduleRequests.length === 1, '변경된 일정에 알림 재생성 요청이 있어야 합니다.');

  const failedSchedule = makeSchedule('failed-completed', '2026-10-10' as LocalDate, 0);
  const failed = rescheduleAfterReviewOutcome({
    completedReview: failedSchedule,
    outcome: 'FAILED',
    today: '2026-10-10' as LocalDate,
    allSchedules: [failedSchedule],
    studyRecord: record,
    exam,
    config,
  });
  assert(failed.updatedSchedules.length === 2, 'FAILED는 보강 일정을 최대 1개 생성해야 합니다.');
  assert(failed.updatedSchedules.filter((schedule) => schedule.status !== 'COMPLETED').length === 1, '보강 일정은 하나여야 합니다.');
  assert(failed.reason === 'OUTCOME_FAILED_REINFORCEMENT', '보강 일정 사유를 보존해야 합니다.');

  const successSchedules = [
    makeSchedule('success-completed', '2026-10-10' as LocalDate, 0),
    makeSchedule('success-next', '2026-10-20' as LocalDate, 1),
  ];
  const success = rescheduleAfterReviewOutcome({
    completedReview: successSchedules[0],
    outcome: 'SUCCESS',
    today: '2026-10-10' as LocalDate,
    allSchedules: successSchedules,
    studyRecord: record,
    exam,
    config,
  });
  assert(success.changedScheduleIds.length === 0, 'SUCCESS는 용량 재배치가 없으면 일정을 이동하면 안 됩니다.');
}

/**
 * 실행 시점에 검증 예시를 바로 확인할 수 있도록 하는 경량 self-check.
 * 이 함수는 LocalDate 유틸리티와 정규 리뷰 일정 생성 규칙을 함께 검증한다.
 */
export function runSelfChecks(): void {
  runDailyLoadRebalancingSelfChecks();
  runReviewOutcomeSelfChecks();
  // LocalDate는 UTC/DST 경계 문제를 피하고 달력 일 단위로만 계산한다.
  assert(getEarliestAllowedExamDate('2026-10-01' as LocalDate, 1, 3) === '2026-10-06' as LocalDate, '가장 빠른 허용 시험일은 2026-10-06이어야 합니다.');
  assert(getLastReviewDate('2026-10-06' as LocalDate, 1) === '2026-10-04' as LocalDate, '마지막 정규 리뷰일은 시험일 1일 전 버퍼 직전이어야 합니다.');
  assert(!isValidLocalDate('2026-02-30'), '존재하지 않는 날짜는 무효해야 합니다.');
  assert(isValidLocalDate('2024-02-29'), '윤년 2월 29일은 유효해야 합니다.');
  assert(addDays('2026-02-28' as LocalDate, 1) === '2026-03-01' as LocalDate, '월 경계를 넘는 더하기가 정확해야 합니다.');
  assert(addDays('2026-10-03' as LocalDate, -1) === '2026-10-02' as LocalDate, '음수 오프셋이 올바르게 처리되어야 합니다.');
  assert(getAvailableDaysForNewLearning('2026-10-01' as LocalDate, '2026-10-06' as LocalDate, 1) === 3, 'availableDaysForNewLearning은 3이어야 합니다.');
  assert(getEffectiveStudyDays('2026-10-01' as LocalDate, '2026-10-06' as LocalDate, 1) === 3, 'effectiveStudyDays는 3이어야 합니다.');

  const todayBlocked = validateExamDateForNewLearning({
    todayDate: '2026-10-01' as LocalDate,
    examDate: '2026-10-01' as LocalDate,
    finalReviewBufferDays: 1,
    minEffectiveStudyDays: 3,
  });
  assert(todayBlocked.isValid === false, '오늘 시험일은 차단되어야 합니다.');
  assert(
    buildExamDateBlockingMessage(todayBlocked) === '시험일은 오늘 이후로 설정해 주세요. 현재 선택한 날짜는 학습 및 복습 일정을 만들 수 없습니다.',
    '오늘/과거 시험일 메시지는 정확해야 합니다.',
  );

  const earliestBufferZero = getEarliestAllowedExamDate('2026-10-01' as LocalDate, 0, 3);
  const earliestBufferOne = getEarliestAllowedExamDate('2026-10-01' as LocalDate, 1, 3);
  const earliestBufferTwo = getEarliestAllowedExamDate('2026-10-01' as LocalDate, 2, 3);
  assert(earliestBufferZero === '2026-10-05' as LocalDate, 'buffer 0일이면 가장 빠른 허용 시험일은 2026-10-05여야 합니다.');
  assert(earliestBufferOne === '2026-10-06' as LocalDate, 'buffer 1일이면 가장 빠른 허용 시험일은 2026-10-06여야 합니다.');
  assert(earliestBufferTwo === '2026-10-07' as LocalDate, 'buffer 2일이면 가장 빠른 허용 시험일은 2026-10-07여야 합니다.');

  const cramRecordList = [{
    id: 'rec-cram',
    examId: 'exam-1',
    content: '최근 기록',
    studiedAt: '2026-10-04' as LocalDate,
    difficulty: 'medium',
    importance: 'normal',
    initialMastery: 3,
    estimatedReviewMinutes: 15,
    isCompleted: false,
    createdAt: '2026-10-04' as LocalDate,
    updatedAt: '2026-10-04' as LocalDate,
  }] as StudyRecord[];

  const cramValidation = validateExamDateAgainstExistingRecords({
    todayDate: '2026-10-01' as LocalDate,
    examDate: '2026-10-06' as LocalDate,
    finalReviewBufferDays: 1,
    minEffectiveStudyDays: 3,
    records: cramRecordList,
    config: { ...DEFAULT_SCHEDULER_CONFIG, strictExistingRecordValidation: false, allowExamDateWithCramRequiredRecords: true },
  });
  assert(cramValidation.recordsRequiringCramMode.includes('rec-cram'), '최근 기록은 크램 모드 대상이어야 합니다.');
  assert(cramValidation.isValid === true, '기본 정책은 크램 모드 기록을 경고만 남기고 저장을 허용해야 합니다.');
  assert(cramValidation.warnings[0]?.includes('등록된 학습기록 중 1개 항목은 시험일까지 정규 분산복습 기준을 충족하지 못합니다.'), '기본 경고 메시지는 정확해야 합니다.');

  const strictCramValidation = validateExamDateAgainstExistingRecords({
    todayDate: '2026-10-01' as LocalDate,
    examDate: '2026-10-06' as LocalDate,
    finalReviewBufferDays: 1,
    minEffectiveStudyDays: 3,
    records: cramRecordList,
    config: { ...DEFAULT_SCHEDULER_CONFIG, strictExistingRecordValidation: true, allowExamDateWithCramRequiredRecords: true },
  });
  assert(strictCramValidation.isValid === false, 'strictExistingRecordValidation=true면 저장이 막혀야 합니다.');
  assert(
    strictCramValidation.blockingReason === '등록된 학습기록 중 1개 항목의 복습 기간이 부족합니다. 현재 정책에서는 모든 항목이 최소 복습 기간을 확보해야 시험일을 설정할 수 있습니다.',
    'strict 모드 차단 메시지는 정확해야 합니다.',
  );

  const cramDisallowedValidation = validateExamDateAgainstExistingRecords({
    todayDate: '2026-10-01' as LocalDate,
    examDate: '2026-10-06' as LocalDate,
    finalReviewBufferDays: 1,
    minEffectiveStudyDays: 3,
    records: cramRecordList,
    config: { ...DEFAULT_SCHEDULER_CONFIG, strictExistingRecordValidation: false, allowExamDateWithCramRequiredRecords: false },
  });
  assert(cramDisallowedValidation.isValid === false, 'allowExamDateWithCramRequiredRecords=false면 저장이 막혀야 합니다.');

  const combinedValidation = validateExamDate({
    todayDate: '2026-10-01' as LocalDate,
    examDate: '2026-10-06' as LocalDate,
    finalReviewBufferDays: 1,
    minEffectiveStudyDays: 3,
    records: cramRecordList,
    config: { ...DEFAULT_SCHEDULER_CONFIG, strictExistingRecordValidation: false, allowExamDateWithCramRequiredRecords: true },
  });
  assert(combinedValidation.isValid === true, '새 학습기간 조건이 통과하면 기본 설정은 크램 모드 경고만 남겨야 합니다.');
  assert(combinedValidation.recordsRequiringCramMode.length === 1, 'warningCount는 크램 모드 기록 수와 일치해야 합니다.');
  assert(
    buildExamDateValidationState(combinedValidation).warningCount === 1,
    'UI 상태의 warningCount는 크램 모드 기록 수를 반영해야 합니다.',
  );

  const combinedBlockedDueToNewWindow = validateExamDate({
    todayDate: '2026-10-01' as LocalDate,
    examDate: '2026-10-04' as LocalDate,
    finalReviewBufferDays: 1,
    minEffectiveStudyDays: 3,
    records: cramRecordList,
    config: { ...DEFAULT_SCHEDULER_CONFIG, strictExistingRecordValidation: true, allowExamDateWithCramRequiredRecords: true },
  });
  assert(combinedBlockedDueToNewWindow.isValid === false, '새 학습기간 부족은 기본적으로 차단되어야 합니다.');
  assert(
    combinedBlockedDueToNewWindow.blockingReason?.includes('현재 설정에서는 시험 전 정규 복습에 사용할 수 있는 기간이'),
    '새 학습기간 부족은 기본 차단 사유여야 합니다.',
  );

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
      studiedAt: '2026-10-01' as LocalDate,
      title: '약점 정리',
      difficulty: 'medium',
      importance: 'normal',
      estimatedReviewMinutes: 20,
      initialMastery: 3,
      isCompleted: true,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    examDate: '2026-11-15' as LocalDate,
    finalReviewBufferDays: 1,
  });
  assert(JSON.stringify(sufficient.generatedDates) === JSON.stringify(['2026-10-02','2026-10-13','2026-10-23','2026-11-03','2026-11-13']), '충분한 창의 생성 일자는 정책에 맞게 결정되어야 합니다.');
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
      studiedAt: '2026-10-01' as LocalDate,
      title: '압축 일정 사례',
      difficulty: 'hard',
      importance: 'high',
      estimatedReviewMinutes: 15,
      initialMastery: 2,
      isCompleted: true,
      createdAt: '2026-10-01' as LocalDate,
      updatedAt: '2026-10-01' as LocalDate,
    },
    examDate: '2026-10-12' as LocalDate,
    finalReviewBufferDays: 1,
  });
  assert(JSON.stringify(compressed.generatedDates) === JSON.stringify(['2026-10-02','2026-10-05','2026-10-07','2026-10-10']), '압축 창의 생성 일자는 정책에 맞게 결정되어야 합니다.');
  assert(compressed.generatedDates.length === 4, '압축 창은 4개의 유효한 날짜만 생성해야 합니다.');
  assert(compressed.generatedDates[0] === '2026-10-02' as LocalDate, '압축 일정의 시작은 reviewStartDate여야 합니다.');
  assert(compressed.generatedDates[compressed.generatedDates.length - 1] === '2026-10-10' as LocalDate, '압축 일정의 마지막은 lastReviewDate여야 합니다.');
  assert(compressed.wasCompressed === true, '짧은 압축 창은 wasCompressed=true여야 합니다.');
  assert(compressed.status.includes('SCHEDULED') || compressed.status.includes('INSUFFICIENT_WINDOW'), '압축 일정은 상태를 포함해야 합니다.');

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
