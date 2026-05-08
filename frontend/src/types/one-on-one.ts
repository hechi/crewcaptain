// 1:1 Entry Management Types
// Matches API contract from OneOnOneResponses.kt and OneOnOneRequests.kt

// --- Enums ---

export type CadenceType = 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'CUSTOM';

// --- Response Types ---

export interface AgendaItem {
  id: string;
  text: string;
  checked: boolean;
  displayOrder: number;
  createdAt: string;
}

export interface OneOnOneSeries {
  id: string;
  personId: string;
  cadenceType: CadenceType;
  customIntervalDays: number | null;
  templateMarkdown: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OneOnOneEntry {
  id: string;
  personId: string;
  meetingDate: string;
  agendaItems: AgendaItem[];
  notesMarkdown: string | null;
  outcomesMarkdown: string | null;
  sensitive: boolean;
  createdAt: string;
  updatedAt: string;
}

// --- Request Types ---

export interface UpsertSeriesRequest {
  cadenceType: CadenceType;
  customIntervalDays?: number | null;
  templateMarkdown?: string | null;
}

export interface AgendaItemRequest {
  text: string;
  checked?: boolean;
}

export interface CreateOneOnOneEntryRequest {
  meetingDate: string;
  agendaItems?: AgendaItemRequest[];
  notesMarkdown?: string | null;
  outcomesMarkdown?: string | null;
  sensitive?: boolean;
}

export interface UpdateOneOnOneEntryRequest {
  meetingDate?: string;
  agendaItems?: AgendaItemRequest[];
  notesMarkdown?: string | null;
  outcomesMarkdown?: string | null;
  sensitive?: boolean;
}
