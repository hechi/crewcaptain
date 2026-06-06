export type Theme = 'DARK' | 'LIGHT';

export type AiWritingStyle = 'NARRATIVE' | 'BULLET_POINTS' | 'CONCISE';

export interface UserSettings {
  dueSoonDays: number;
  staleOneOnOneDays: number;
  anniversaryLookaheadDays: number;
  theme: Theme;
  showAchievements: boolean;
  notifyActionItemOverdue: boolean;
  notifyActionItemDueSoon: boolean;
  notifyStaleOneOnOne: boolean;
  notifyUpcomingAnniversary: boolean;
  aiEnabled: boolean;
  aiApiBaseUrl: string | null;
  aiModelName: string | null;
  aiPrivacyMode: boolean;
  aiWritingStyle: AiWritingStyle;
  aiAutoExecuteCommands: boolean;
  kudosRefinementPrompt: string | null;
  pdpOptimizationPrompt: string | null;
  agendaPrepPrompt: string | null;
  narrativePrompt: string | null;
  outcomeExtractorPrompt: string | null;
  trendRadarPrompt: string | null;
  linkSuggestionsPrompt: string | null;
  triageHintPrompt: string | null;
  commandTerminalPrompt: string | null;
}

export interface UpdateUserSettingsRequest {
  dueSoonDays: number;
  staleOneOnOneDays: number;
  anniversaryLookaheadDays: number;
  theme: Theme;
  showAchievements: boolean;
  notifyActionItemOverdue: boolean;
  notifyActionItemDueSoon: boolean;
  notifyStaleOneOnOne: boolean;
  notifyUpcomingAnniversary: boolean;
  aiEnabled: boolean;
  aiApiBaseUrl?: string | null;
  aiApiKey?: string | null;
  aiModelName?: string | null;
  aiPrivacyMode: boolean;
  aiWritingStyle: AiWritingStyle;
  aiAutoExecuteCommands: boolean;
  kudosRefinementPrompt?: string | null;
  pdpOptimizationPrompt?: string | null;
  agendaPrepPrompt?: string | null;
  narrativePrompt?: string | null;
  outcomeExtractorPrompt?: string | null;
  trendRadarPrompt?: string | null;
  linkSuggestionsPrompt?: string | null;
  triageHintPrompt?: string | null;
  commandTerminalPrompt?: string | null;
}

export interface AiPrepResponse {
  suggestions: string[];
  error: string | null;
}

export interface AiNarrativeResponse {
  narrative: string | null;
  error: string | null;
}

export interface AiCoachingResponse {
  result: string | null;
  error: string | null;
}

export interface AiExtractionResponse {
  actionItems: ExtractedActionItem[];
  decisions: string[];
  error: string | null;
}

export interface ExtractedActionItem {
  title: string;
  ownerType: 'MANAGER' | 'PERSON';
  suggestedDaysToDue: number | null;
}

export interface ApplyOutcomesRequest {
  actionItems: ApplyActionItemRequest[];
  decisions: string[];
}

export interface ApplyActionItemRequest {
  title: string;
  ownerType: string;
  suggestedDaysToDue: number | null;
}

export interface ApplyOutcomesResponse {
  actionItemsCreated: number;
  decisionsAppended: number;
}

export interface AiTrendRadarResponse {
  insights: TrendRadarInsight[];
  insufficientData: boolean;
  meetingsNeeded: number | null;
  error: string | null;
}

export interface TrendRadarInsight {
  title: string;
  description: string;
  dimension: 'MORALE' | 'WORK_GROWTH_BALANCE' | 'RECOGNITION' | 'MEETING_EFFICACY';
  confidenceScore: number;
}

export interface AiCommandResponse {
  intent: string | null;
  targetPersonId: string | null;
  content: string | null;
  dueDate: string | null;
  meetingDate: string | null;
  tags: string[];
  sensitive: boolean;
  error: string | null;
}

export interface PersonDirectoryEntry {
  id: string;
  preferredName: string;
}
