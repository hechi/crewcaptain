// Quick Note Types
// Matches API contract from QuickNoteResponses.kt and QuickNoteRequests.kt

export type QuickNoteStatus = 'INBOX' | 'ATTACHED' | 'CONVERTED' | 'ARCHIVED';

export interface QuickNote {
  id: string;
  personId: string | null;
  text: string;
  sensitive: boolean;
  selfAssigned: boolean;
  status: QuickNoteStatus;
  attachedEntryId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateQuickNoteRequest {
  text: string;
  personId?: string;
  sensitive?: boolean;
  selfAssigned?: boolean;
}

export interface UpdateQuickNoteRequest {
  text?: string;
  personId?: string;
  sensitive?: boolean;
}

export interface AssignQuickNoteToPersonRequest {
  personId: string;
}

export interface AttachQuickNoteToEntryRequest {
  entryId: string;
}

export interface PaginatedQuickNoteResponse {
  content: QuickNote[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
