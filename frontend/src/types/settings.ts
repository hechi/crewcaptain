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
}

export interface AiPrepResponse {
  suggestions: string[];
  error: string | null;
}

export interface AiNarrativeResponse {
  narrative: string | null;
  error: string | null;
}
