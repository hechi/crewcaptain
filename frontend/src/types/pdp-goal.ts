// PDP Goal Types
// Matches API contract from PdpGoalResponses.kt and PdpGoalRequests.kt

export type PdpGoalStatus = 'ACTIVE' | 'ACHIEVED' | 'PAUSED' | 'DROPPED';

export interface PdpGoal {
  id: string;
  personId: string;
  title: string;
  description: string | null;
  targetDate: string | null;
  status: PdpGoalStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PdpUpdate {
  id: string;
  goalId: string;
  textMarkdown: string;
  sensitive: boolean;
  createdAt: string;
}

export interface CreatePdpGoalRequest {
  title: string;
  description?: string | null;
  targetDate?: string | null;
}

export interface UpdatePdpGoalRequest {
  title?: string;
  description?: string | null;
  targetDate?: string | null;
}

export interface CreatePdpUpdateRequest {
  textMarkdown: string;
  sensitive?: boolean;
}

export interface PaginatedPdpGoalResponse {
  content: PdpGoal[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PaginatedPdpUpdateResponse {
  content: PdpUpdate[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
