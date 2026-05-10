export type NotificationType =
  | 'ACTION_ITEM_OVERDUE'
  | 'ACTION_ITEM_DUE_SOON'
  | 'STALE_ONE_ON_ONE'
  | 'UPCOMING_ANNIVERSARY';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  referenceId: string | null;
  personId: string | null;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface PaginatedNotificationResponse {
  content: Notification[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UnreadCountResponse {
  count: number;
}

export interface MarkAllReadResponse {
  markedCount: number;
}
