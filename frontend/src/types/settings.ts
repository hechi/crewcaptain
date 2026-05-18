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
  kudosRefinementPrompt: string | null;
  pdpOptimizationPrompt: string | null;
  agendaPrepPrompt: string | null;
  narrativePrompt: string | null;
  outcomeExtractorPrompt: string | null;
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
  kudosRefinementPrompt?: string | null;
  pdpOptimizationPrompt?: string | null;
  agendaPrepPrompt?: string | null;
  narrativePrompt?: string | null;
  outcomeExtractorPrompt?: string | null;
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
