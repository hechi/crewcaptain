export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'RESTORE' | 'LINK' | 'UNLINK';

export type AuditEntityType =
  | 'PERSON'
  | 'ONE_ON_ONE_ENTRY'
  | 'ONE_ON_ONE_SERIES'
  | 'ACTION_ITEM'
  | 'PDP_GOAL'
  | 'PDP_UPDATE'
  | 'KUDOS'
  | 'QUICK_NOTE'
  | 'USER_SETTINGS'
  | 'WORKSPACE'
  | 'STRATEGY_GOAL';

export interface AuditLogEntry {
  id: string;
  action: AuditAction;
  entityType: AuditEntityType;
  entityId: string;
  personId: string | null;
  summary: string;
  createdAt: string;
}

export interface PaginatedAuditLogResponse {
  content: AuditLogEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
