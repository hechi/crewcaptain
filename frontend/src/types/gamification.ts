export interface StreakData {
  currentStreak: number;
  longestStreak: number;
  totalOneOnOnesHeld: number;
}

export type AchievementType =
  | 'FIRST_ONE_ON_ONE'
  | 'TEN_ONE_ON_ONES'
  | 'FIFTY_ONE_ON_ONES'
  | 'FIRST_ACTION_ITEM_CLOSED'
  | 'TEN_ACTION_ITEMS_CLOSED'
  | 'FIFTY_ACTION_ITEMS_CLOSED'
  | 'HUNDRED_ACTION_ITEMS_CLOSED'
  | 'FIRST_PDP_GOAL_ACHIEVED'
  | 'FIVE_PDP_GOALS_ACHIEVED'
  | 'FIRST_KUDOS_GIVEN'
  | 'TEN_KUDOS_GIVEN'
  | 'STREAK_SEVEN'
  | 'STREAK_THIRTY';

export interface Achievement {
  type: AchievementType;
  unlockedAt: string;
  label: string;
  description: string;
}

export interface ActivityDay {
  date: string;
  count: number;
}

export interface PdpProgressSummary {
  totalActive: number;
  totalAchieved: number;
  totalPaused: number;
  totalDropped: number;
  completionPercentage: number;
}

export interface GamificationStats {
  streaks: StreakData;
  achievements: Achievement[];
  activityHeatmap: ActivityDay[];
  pdpProgress: PdpProgressSummary;
}
