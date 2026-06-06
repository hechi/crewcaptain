// Triage Queue Types
// Matches API contract from TriageResponses.kt

export type TriageItemType = 'ACTION_ITEM_OVERDUE' | 'ACTION_ITEM_DUE_SOON' | 'STALE_ONE_ON_ONE' | 'UPCOMING_ANNIVERSARY';
export type TriageCriticality = 'OVERDUE' | 'DUE_SOON' | 'STALE' | 'INFORMATIONAL';
export type ActionItemOwnerType = 'MANAGER' | 'PERSON';
export type OwnerScope = 'ALL' | 'MINE';

export interface TriageItem {
  id: string;
  type: TriageItemType;
  criticality: TriageCriticality;
  title: string;
  personId: string;
  personName: string;
  workspaceId: string | null;
  workspaceName: string | null;
  sensitive: boolean;
  dueDate: string | null;
  daysOverdue: number | null;
  daysUntilDue: number | null;
  ownerType: ActionItemOwnerType | null;
  sourceActionItemId: string | null;
  snoozedUntil: string | null;
  createdAt: string;
}

export interface TriageQueueResponse {
  items: TriageItem[];
  totalCount: number;
}

export interface TriageHintResponse {
  hint: string | null;
  error: string | null;
}

export interface SnoozeActionItemRequest {
  days?: number;
  snoozedUntil?: string;
}

export interface TriageFilters {
  type?: TriageItemType;
  scope: OwnerScope;
  workspaceId?: string[];
  personId?: string;
}
