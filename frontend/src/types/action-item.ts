// Action Item Types
// Matches API contract from ActionItemResponses.kt and ActionItemRequests.kt

export type ActionItemStatus = 'OPEN' | 'DONE' | 'CANCELED';
export type ActionItemOwnerType = 'MANAGER' | 'PERSON';

export interface ActionItem {
  id: string;
  // Null for a manager's own (person-less) TODO, e.g. one converted from a personal note.
  personId: string | null;
  title: string;
  description: string | null;
  ownerType: ActionItemOwnerType;
  dueDate: string | null;
  status: ActionItemStatus;
  originatingEntryId: string | null;
  originatingQuickNoteId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateActionItemRequest {
  title: string;
  description?: string | null;
  ownerType?: ActionItemOwnerType;
  dueDate?: string | null;
  originatingEntryId?: string | null;
}

export interface UpdateActionItemRequest {
  title?: string;
  description?: string | null;
  ownerType?: ActionItemOwnerType;
  dueDate?: string | null;
}

export interface PaginatedActionItemResponse {
  content: ActionItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
