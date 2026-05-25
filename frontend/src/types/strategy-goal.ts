// Strategy Goal Types
// Matches API contract from StrategyGoalResponses.kt and StrategyGoalRequests.kt

export type StrategyGoalStatus = 'ACTIVE' | 'ACHIEVED' | 'DROPPED';

export interface StrategyGoal {
  id: string;
  title: string;
  description: string | null;
  targetDate: string | null;
  status: StrategyGoalStatus;
  sensitive: boolean;
  createdAt: string;
  updatedAt: string;
  linkedPdpGoalCount?: number;
}

export interface CreateStrategyGoalRequest {
  title: string;
  description?: string | null;
  targetDate?: string | null;
  sensitive?: boolean;
}

export interface UpdateStrategyGoalRequest {
  title?: string;
  description?: string | null;
  targetDate?: string | null;
}

export interface LinkPdpGoalRequest {
  pdpGoalId: string;
  personId: string;
}

export interface PaginatedStrategyGoalResponse {
  content: StrategyGoal[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AlignmentScore {
  strategyGoalId: string;
  strategyGoalTitle: string;
  totalActivePdpGoals: number;
  linkedPdpGoals: number;
  alignmentPercentage: number;
}

export interface AllAlignmentScoresResponse {
  scores: AlignmentScore[];
}

export interface UnlinkedPdpGoalInfo {
  pdpGoalId: string;
  personId: string;
  title: string;
}

export interface EmptyStrategyGoalInfo {
  strategyGoalId: string;
  title: string;
}

export interface GapAnalysis {
  unlinkedPdpGoals: UnlinkedPdpGoalInfo[];
  emptyStrategyGoals: EmptyStrategyGoalInfo[];
}

export interface LinkedPdpGoalInfo {
  pdpGoalId: string;
  personId: string;
  personName: string;
  title: string;
}

export interface StrategyGoalBasicInfo {
  strategyGoalId: string;
  title: string;
  status: StrategyGoalStatus;
}

export interface LinkSuggestion {
  strategyGoalId: string;
  strategyGoalTitle: string;
  pdpGoalId: string;
  personId: string;
  pdpGoalTitle: string;
  personName: string;
  matchScore: number;
  reasoning: string;
}
