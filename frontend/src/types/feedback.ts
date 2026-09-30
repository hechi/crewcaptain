// Feedback Links feature types.
// Match the API contracts in the api/ backend (FeedbackTemplateDtos.kt, FeedbackLinkDtos.kt,
// FeedbackResponseDtos.kt, FeedbackAnalyticsDtos.kt, PublicFeedbackDtos.kt).

export type FeedbackQuestionType = 'RATING' | 'LIKERT' | 'TEXT';
export type ShowIfOperator = 'LTE' | 'GTE' | 'EQ';

export interface ShowIfRule {
  questionId: string;
  operator: ShowIfOperator;
  value: number;
}

export interface FeedbackQuestion {
  id: string;
  type: FeedbackQuestionType;
  text: string;
  required: boolean;
  lowLabel?: string | null;
  highLabel?: string | null;
  showIf?: ShowIfRule | null;
}

export interface FeedbackTemplate {
  id: string;
  title: string;
  description?: string | null;
  questions: FeedbackQuestion[];
  createdAt?: string;
  updatedAt?: string;
}

export interface FeedbackTemplateDraft {
  title: string;
  description?: string | null;
  questions: FeedbackQuestion[];
}

export interface SaveFeedbackTemplateRequest {
  title: string;
  description?: string | null;
  questions: FeedbackQuestion[];
}

// ===== Links =====

export type FeedbackLinkStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED';

export interface FeedbackLink {
  id: string;
  personId: string;
  token: string;
  title: string;
  description?: string | null;
  label?: string | null;
  requestSubmitterInfo: boolean;
  status: FeedbackLinkStatus;
  expiresAt: string;
  revokedAt?: string | null;
  submissionCount: number;
  lastSubmissionAt?: string | null;
  createdAt: string;
}

export interface CreateFeedbackLinkRequest {
  templateId?: string | null;
  // Inline questions used when no saved template is chosen (e.g. an AI draft or starter).
  title?: string | null;
  description?: string | null;
  questions?: FeedbackQuestion[] | null;
  expiresInDays?: number | null;
  label?: string | null;
  requestSubmitterInfo?: boolean | null;
}

export interface BulkCreateFeedbackLinkRequest {
  personIds: string[];
  templateId: string;
  expiresInDays?: number | null;
  label?: string | null;
  requestSubmitterInfo?: boolean | null;
}

export interface BulkFeedbackLinkResult {
  links: Array<{ personId: string; personName: string; token: string; linkId: string }>;
}

// ===== Responses =====

export type FeedbackResponseStatus = 'PENDING' | 'APPROVED';
export type FeedbackConversionType = 'KUDO' | 'QUICK_NOTE' | 'ACTION_ITEM';

export interface FeedbackAnswer {
  questionId: string;
  ratingValue?: number | null;
  textValue?: string | null;
}

export interface FeedbackResponseItem {
  id: string;
  personId: string;
  linkId: string;
  submitterName?: string | null;
  submitterEmail?: string | null;
  anonymous: boolean;
  answers: FeedbackAnswer[];
  additionalComments?: string | null;
  status: FeedbackResponseStatus;
  flagged: boolean;
  pinned: boolean;
  convertedToType?: FeedbackConversionType | null;
  convertedToId?: string | null;
  createdAt: string;
}

export interface UpdateFeedbackResponseRequest {
  approve?: boolean;
  flagged?: boolean;
  pinned?: boolean;
}

export interface ConvertFeedbackResponseRequest {
  type: FeedbackConversionType;
  text?: string | null;
}

// ===== Analytics & summaries =====

export interface RatingPeriod {
  label: string;
  averageRating: number;
  responseCount: number;
}

export interface ThemeCount {
  theme: string;
  count: number;
}

export interface FeedbackAnalytics {
  totalResponses: number;
  overallAverageRating?: number | null;
  periods: RatingPeriod[];
  topThemes: ThemeCount[];
}

export interface FeedbackSummary {
  id: string;
  personId: string;
  periodFrom: string;
  periodTo: string;
  content: string;
  responseCount: number;
  createdAt: string;
  updatedAt: string;
}

// ===== Public form =====

export interface PublicFeedbackForm {
  personName: string;
  title: string;
  description?: string | null;
  requestSubmitterInfo: boolean;
  questions: FeedbackQuestion[];
}

export interface PublicFeedbackSubmission {
  anonymous: boolean;
  submitterName?: string | null;
  submitterEmail?: string | null;
  answers: FeedbackAnswer[];
  additionalComments?: string | null;
  // Honeypot: must be empty. Bots that fill every field get silently rejected.
  website?: string;
}
