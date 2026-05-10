import { Person, PaginatedResponse, PinnedRememberItem, MoraleStatus } from '@/types/person';
import { ApiError, ApiException } from '@/types/api';
import {
  OneOnOneSeries,
  OneOnOneEntry,
  UpsertSeriesRequest,
  CreateOneOnOneEntryRequest,
  UpdateOneOnOneEntryRequest,
} from '@/types/one-on-one';
import {
  ActionItem,
  ActionItemStatus,
  CreateActionItemRequest,
  UpdateActionItemRequest,
  PaginatedActionItemResponse,
} from '@/types/action-item';
import {
  PdpGoal,
  PdpGoalStatus,
  PdpUpdate,
  CreatePdpGoalRequest,
  UpdatePdpGoalRequest,
  CreatePdpUpdateRequest,
  PaginatedPdpGoalResponse,
  PaginatedPdpUpdateResponse,
} from '@/types/pdp-goal';
import {
  Kudos,
  CreateKudosRequest,
  PaginatedKudosResponse,
} from '@/types/kudos';
import {
  QuickNote,
  QuickNoteStatus,
  CreateQuickNoteRequest,
  UpdateQuickNoteRequest,
  AssignQuickNoteToPersonRequest,
  AttachQuickNoteToEntryRequest,
  PaginatedQuickNoteResponse,
} from '@/types/quick-note';
import { DashboardResponse } from '@/types/dashboard';
import {
  PaginatedNotificationResponse,
  UnreadCountResponse,
  Notification as NotificationItem,
  MarkAllReadResponse,
} from '@/types/notification';
import { SearchResponse, SearchResultType } from '@/types/search';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || '/api/v1';

async function fetchWithAuth(url: string, options: RequestInit = {}, token: string): Promise<Response> {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...options.headers,
    },
  });

  if (!response.ok) {
    let errorBody: ApiError;
    try {
      errorBody = await response.json();
    } catch {
      errorBody = {
        status: response.status,
        error: response.statusText || 'Error',
        message: response.status === 401 ? 'Authentication failed. Please sign in again.' : `Request failed with status ${response.status}`,
        timestamp: new Date().toISOString(),
      };
    }
    throw new ApiException(errorBody.status, errorBody.error, errorBody.message, errorBody.timestamp);
  }

  return response;
}

export async function createPerson(token: string, data: {
  name: string;
  preferredName?: string;
  roleTitle?: string;
  timezone?: string;
  startDate?: string;
  email?: string;
  tags?: string[];
}): Promise<Person> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function getPerson(token: string, id: string): Promise<Person> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${id}`, {}, token);
  return response.json();
}

export async function updatePerson(token: string, id: string, data: {
  name: string;
  preferredName?: string;
  roleTitle?: string;
  timezone?: string;
  startDate?: string;
  email?: string;
  tags?: string[];
}): Promise<Person> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function deletePerson(token: string, id: string): Promise<void> {
  await fetchWithAuth(`${API_BASE_URL}/persons/${id}`, {
    method: 'DELETE',
  }, token);
}

export async function listPersons(token: string, params?: {
  page?: number;
  size?: number;
  tag?: string;
  morale?: MoraleStatus;
}): Promise<PaginatedResponse<Person>> {
  const searchParams = new URLSearchParams();
  if (params?.page !== undefined) searchParams.set('page', params.page.toString());
  if (params?.size !== undefined) searchParams.set('size', params.size.toString());
  if (params?.tag) searchParams.set('tag', params.tag);
  if (params?.morale) searchParams.set('morale', params.morale);

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/persons${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

export async function setMorale(token: string, personId: string, data: {
  status: MoraleStatus;
  note?: string;
}): Promise<Person> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/morale`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function addRememberItem(token: string, personId: string, text: string): Promise<PinnedRememberItem[]> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/remember-items`, {
    method: 'POST',
    body: JSON.stringify({ text }),
  }, token);
  return response.json();
}

export async function removeRememberItem(token: string, personId: string, itemId: string): Promise<PinnedRememberItem[]> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/remember-items/${itemId}`, {
    method: 'DELETE',
  }, token);
  return response.json();
}

export async function reorderRememberItems(token: string, personId: string, orderedIds: string[]): Promise<PinnedRememberItem[]> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/remember-items/reorder`, {
    method: 'PUT',
    body: JSON.stringify({ orderedIds }),
  }, token);
  return response.json();
}

// --- 1:1 Series ---

export async function upsertOneOnOneSeries(token: string, personId: string, data: UpsertSeriesRequest): Promise<OneOnOneSeries> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/one-on-one-series`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function getOneOnOneSeries(token: string, personId: string): Promise<OneOnOneSeries> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/one-on-one-series`, {}, token);
  return response.json();
}

// --- 1:1 Entries ---

export async function createOneOnOneEntry(token: string, personId: string, data: CreateOneOnOneEntryRequest): Promise<OneOnOneEntry> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/one-on-one-entries`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function getOneOnOneEntry(token: string, personId: string, entryId: string): Promise<OneOnOneEntry> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/one-on-one-entries/${entryId}`, {}, token);
  return response.json();
}

export async function updateOneOnOneEntry(token: string, personId: string, entryId: string, data: UpdateOneOnOneEntryRequest): Promise<OneOnOneEntry> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/one-on-one-entries/${entryId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function deleteOneOnOneEntry(token: string, personId: string, entryId: string): Promise<void> {
  await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/one-on-one-entries/${entryId}`, {
    method: 'DELETE',
  }, token);
}

export async function listOneOnOneEntries(token: string, personId: string, page?: number, size?: number): Promise<PaginatedResponse<OneOnOneEntry>> {
  const searchParams = new URLSearchParams();
  if (page !== undefined) searchParams.set('page', page.toString());
  if (size !== undefined) searchParams.set('size', size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/persons/${personId}/one-on-one-entries${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

// --- Action Items ---

export async function createActionItem(token: string, personId: string, data: CreateActionItemRequest): Promise<ActionItem> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/action-items`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function getActionItem(token: string, personId: string, actionItemId: string): Promise<ActionItem> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/action-items/${actionItemId}`, {}, token);
  return response.json();
}

export async function updateActionItem(token: string, personId: string, actionItemId: string, data: UpdateActionItemRequest): Promise<ActionItem> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/action-items/${actionItemId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function completeActionItem(token: string, personId: string, actionItemId: string): Promise<ActionItem> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/action-items/${actionItemId}/complete`, {
    method: 'POST',
  }, token);
  return response.json();
}

export async function cancelActionItem(token: string, personId: string, actionItemId: string): Promise<ActionItem> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/action-items/${actionItemId}/cancel`, {
    method: 'POST',
  }, token);
  return response.json();
}

export async function deleteActionItem(token: string, personId: string, actionItemId: string): Promise<void> {
  await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/action-items/${actionItemId}`, {
    method: 'DELETE',
  }, token);
}

export async function listActionItemsByPerson(token: string, personId: string, params?: {
  status?: ActionItemStatus;
  page?: number;
  size?: number;
}): Promise<PaginatedActionItemResponse> {
  const searchParams = new URLSearchParams();
  if (params?.status) searchParams.set('status', params.status);
  if (params?.page !== undefined) searchParams.set('page', params.page.toString());
  if (params?.size !== undefined) searchParams.set('size', params.size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/persons/${personId}/action-items${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

export async function listAllActionItems(token: string, params?: {
  status?: ActionItemStatus;
  overdueOnly?: boolean;
  page?: number;
  size?: number;
}): Promise<PaginatedActionItemResponse> {
  const searchParams = new URLSearchParams();
  if (params?.status) searchParams.set('status', params.status);
  if (params?.overdueOnly) searchParams.set('overdueOnly', 'true');
  if (params?.page !== undefined) searchParams.set('page', params.page.toString());
  if (params?.size !== undefined) searchParams.set('size', params.size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/action-items${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

// --- PDP Goals ---

export async function createPdpGoal(token: string, personId: string, data: CreatePdpGoalRequest): Promise<PdpGoal> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function getPdpGoal(token: string, personId: string, goalId: string): Promise<PdpGoal> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}`, {}, token);
  return response.json();
}

export async function updatePdpGoal(token: string, personId: string, goalId: string, data: UpdatePdpGoalRequest): Promise<PdpGoal> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function achievePdpGoal(token: string, personId: string, goalId: string): Promise<PdpGoal> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}/achieve`, {
    method: 'POST',
  }, token);
  return response.json();
}

export async function pausePdpGoal(token: string, personId: string, goalId: string): Promise<PdpGoal> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}/pause`, {
    method: 'POST',
  }, token);
  return response.json();
}

export async function dropPdpGoal(token: string, personId: string, goalId: string): Promise<PdpGoal> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}/drop`, {
    method: 'POST',
  }, token);
  return response.json();
}

export async function resumePdpGoal(token: string, personId: string, goalId: string): Promise<PdpGoal> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}/resume`, {
    method: 'POST',
  }, token);
  return response.json();
}

export async function deletePdpGoal(token: string, personId: string, goalId: string): Promise<void> {
  await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}`, {
    method: 'DELETE',
  }, token);
}

export async function listPdpGoalsByPerson(token: string, personId: string, params?: {
  status?: PdpGoalStatus;
  page?: number;
  size?: number;
}): Promise<PaginatedPdpGoalResponse> {
  const searchParams = new URLSearchParams();
  if (params?.status) searchParams.set('status', params.status);
  if (params?.page !== undefined) searchParams.set('page', params.page.toString());
  if (params?.size !== undefined) searchParams.set('size', params.size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/persons/${personId}/pdp-goals${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

// --- PDP Updates (Progress Notes) ---

export async function addPdpUpdate(token: string, personId: string, goalId: string, data: CreatePdpUpdateRequest): Promise<PdpUpdate> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}/updates`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function listPdpUpdates(token: string, personId: string, goalId: string, params?: {
  page?: number;
  size?: number;
}): Promise<PaginatedPdpUpdateResponse> {
  const searchParams = new URLSearchParams();
  if (params?.page !== undefined) searchParams.set('page', params.page.toString());
  if (params?.size !== undefined) searchParams.set('size', params.size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}/updates${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

export async function deletePdpUpdate(token: string, personId: string, goalId: string, updateId: string): Promise<void> {
  await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/pdp-goals/${goalId}/updates/${updateId}`, {
    method: 'DELETE',
  }, token);
}

// --- Kudos ---

export async function createKudos(token: string, personId: string, data: CreateKudosRequest): Promise<Kudos> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/kudos`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function getKudos(token: string, personId: string, kudosId: string): Promise<Kudos> {
  const response = await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/kudos/${kudosId}`, {}, token);
  return response.json();
}

export async function deleteKudos(token: string, personId: string, kudosId: string): Promise<void> {
  await fetchWithAuth(`${API_BASE_URL}/persons/${personId}/kudos/${kudosId}`, {
    method: 'DELETE',
  }, token);
}

export async function listKudosByPerson(token: string, personId: string, params?: {
  page?: number;
  size?: number;
}): Promise<PaginatedKudosResponse> {
  const searchParams = new URLSearchParams();
  if (params?.page !== undefined) searchParams.set('page', params.page.toString());
  if (params?.size !== undefined) searchParams.set('size', params.size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/persons/${personId}/kudos${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

export async function listAllKudos(token: string, params?: {
  page?: number;
  size?: number;
}): Promise<PaginatedKudosResponse> {
  const searchParams = new URLSearchParams();
  if (params?.page !== undefined) searchParams.set('page', params.page.toString());
  if (params?.size !== undefined) searchParams.set('size', params.size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/kudos${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

// --- Quick Notes ---

export async function createQuickNote(token: string, data: CreateQuickNoteRequest): Promise<QuickNote> {
  const response = await fetchWithAuth(`${API_BASE_URL}/quick-notes`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function getQuickNote(token: string, quickNoteId: string): Promise<QuickNote> {
  const response = await fetchWithAuth(`${API_BASE_URL}/quick-notes/${quickNoteId}`, {}, token);
  return response.json();
}

export async function updateQuickNote(token: string, quickNoteId: string, data: UpdateQuickNoteRequest): Promise<QuickNote> {
  const response = await fetchWithAuth(`${API_BASE_URL}/quick-notes/${quickNoteId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function deleteQuickNote(token: string, quickNoteId: string): Promise<void> {
  await fetchWithAuth(`${API_BASE_URL}/quick-notes/${quickNoteId}`, {
    method: 'DELETE',
  }, token);
}

export async function listQuickNotes(token: string, params?: {
  status?: QuickNoteStatus;
  personId?: string;
  page?: number;
  size?: number;
}): Promise<PaginatedQuickNoteResponse> {
  const searchParams = new URLSearchParams();
  if (params?.status) searchParams.set('status', params.status);
  if (params?.personId) searchParams.set('personId', params.personId);
  if (params?.page !== undefined) searchParams.set('page', params.page.toString());
  if (params?.size !== undefined) searchParams.set('size', params.size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/quick-notes${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

export async function assignQuickNoteToPerson(token: string, quickNoteId: string, data: AssignQuickNoteToPersonRequest): Promise<QuickNote> {
  const response = await fetchWithAuth(`${API_BASE_URL}/quick-notes/${quickNoteId}/assign`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function attachQuickNote(token: string, quickNoteId: string, data: AttachQuickNoteToEntryRequest): Promise<QuickNote> {
  const response = await fetchWithAuth(`${API_BASE_URL}/quick-notes/${quickNoteId}/attach`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function convertQuickNote(token: string, quickNoteId: string, data: { personId: string }): Promise<QuickNote> {
  const response = await fetchWithAuth(`${API_BASE_URL}/quick-notes/${quickNoteId}/convert`, {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
  return response.json();
}

export async function archiveQuickNote(token: string, quickNoteId: string): Promise<QuickNote> {
  const response = await fetchWithAuth(`${API_BASE_URL}/quick-notes/${quickNoteId}/archive`, {
    method: 'POST',
  }, token);
  return response.json();
}

// ===== Dashboard =====

export async function getDashboard(
  token: string,
  options?: { dueSoonDays?: number; anniversaryLookaheadDays?: number }
): Promise<DashboardResponse> {
  const params = new URLSearchParams();
  if (options?.dueSoonDays !== undefined) {
    params.set('dueSoonDays', options.dueSoonDays.toString());
  }
  if (options?.anniversaryLookaheadDays !== undefined) {
    params.set('anniversaryLookaheadDays', options.anniversaryLookaheadDays.toString());
  }
  const queryString = params.toString();
  const url = `${API_BASE_URL}/dashboard${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}


// ===== Notifications =====

export async function listNotifications(
  token: string,
  params?: { unreadOnly?: boolean; page?: number; size?: number }
): Promise<PaginatedNotificationResponse> {
  const searchParams = new URLSearchParams();
  if (params?.unreadOnly) searchParams.set('unreadOnly', 'true');
  if (params?.page !== undefined) searchParams.set('page', params.page.toString());
  if (params?.size !== undefined) searchParams.set('size', params.size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/notifications${queryString ? `?${queryString}` : ''}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

export async function getUnreadNotificationCount(token: string): Promise<UnreadCountResponse> {
  const response = await fetchWithAuth(`${API_BASE_URL}/notifications/unread-count`, {}, token);
  return response.json();
}

export async function markNotificationAsRead(token: string, notificationId: string): Promise<NotificationItem> {
  const response = await fetchWithAuth(`${API_BASE_URL}/notifications/${notificationId}/read`, {
    method: 'POST',
  }, token);
  return response.json();
}

export async function markAllNotificationsAsRead(token: string): Promise<MarkAllReadResponse> {
  const response = await fetchWithAuth(`${API_BASE_URL}/notifications/read-all`, {
    method: 'POST',
  }, token);
  return response.json();
}


// ===== Search =====

export async function search(
  token: string,
  params: { q: string; type?: SearchResultType[]; page?: number; size?: number }
): Promise<SearchResponse> {
  const searchParams = new URLSearchParams();
  searchParams.set('q', params.q);
  if (params.type && params.type.length > 0) {
    params.type.forEach((t) => searchParams.append('type', t));
  }
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());

  const queryString = searchParams.toString();
  const url = `${API_BASE_URL}/search?${queryString}`;
  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}

// --- Person Export ---

export async function exportPersonMarkdown(
  token: string,
  personId: string,
  params?: { dateFrom?: string; dateTo?: string }
): Promise<string> {
  const searchParams = new URLSearchParams();
  if (params?.dateFrom) searchParams.set('dateFrom', params.dateFrom);
  if (params?.dateTo) searchParams.set('dateTo', params.dateTo);

  const queryString = searchParams.toString();
  const url = queryString
    ? `${API_BASE_URL}/persons/${personId}/export?${queryString}`
    : `${API_BASE_URL}/persons/${personId}/export`;

  const response = await fetchWithAuth(url, {}, token);
  return response.text();
}

// --- Gamification ---

import { GamificationStats } from '@/types/gamification';

export async function getGamificationStats(
  token: string,
  params?: { heatmapDays?: number }
): Promise<GamificationStats> {
  const searchParams = new URLSearchParams();
  if (params?.heatmapDays !== undefined) searchParams.set('heatmapDays', params.heatmapDays.toString());

  const queryString = searchParams.toString();
  const url = queryString
    ? `${API_BASE_URL}/gamification/stats?${queryString}`
    : `${API_BASE_URL}/gamification/stats`;

  const response = await fetchWithAuth(url, {}, token);
  return response.json();
}
