export type Theme = 'DARK' | 'LIGHT';

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
}
