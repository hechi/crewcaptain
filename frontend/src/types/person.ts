export type MoraleStatus = 'GREEN' | 'YELLOW' | 'RED' | 'UNKNOWN';

export interface PinnedRememberItem {
  id: string;
  text: string;
  displayOrder: number;
  createdAt: string;
}

export interface AtAGlance {
  last1on1Date: string | null;
  openActionItemsCount: number | null;
  activePdpGoalsSummary: string | null;
}

export interface Person {
  id: string;
  name: string;
  preferredName: string | null;
  roleTitle: string | null;
  timezone: string | null;
  startDate: string | null;
  email: string | null;
  tags: string[];
  moraleStatus: MoraleStatus;
  moraleNote: string | null;
  pinnedRememberItems: PinnedRememberItem[];
  workspaceId: string | null;
  atAGlance: AtAGlance;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
