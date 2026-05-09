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
