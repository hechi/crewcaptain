import {
  createActionItem,
  getActionItem,
  updateActionItem,
  completeActionItem,
  cancelActionItem,
  deleteActionItem,
  listActionItemsByPerson,
  listAllActionItems,
} from '@/lib/api-client';
import { ApiException } from '@/types/api';

const mockToken = 'test-jwt-token';
const personId = '550e8400-e29b-41d4-a716-446655440000';
const actionItemId = '660e8400-e29b-41d4-a716-446655440001';

const mockActionItem = {
  id: actionItemId,
  personId,
  title: 'Follow up on project',
  description: 'Check progress',
  ownerType: 'MANAGER',
  dueDate: '2026-05-20',
  status: 'OPEN',
  originatingEntryId: null,
  createdAt: '2026-05-10T10:00:00Z',
  updatedAt: '2026-05-10T10:00:00Z',
};

const mockPaginatedResponse = {
  content: [mockActionItem],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

let mockFetch: jest.Mock;

beforeEach(() => {
  mockFetch = jest.fn();
  global.fetch = mockFetch;
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('createActionItem', () => {
  it('should send POST request with correct URL and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockActionItem),
    });

    const data = { title: 'Follow up on project', description: 'Check progress', ownerType: 'MANAGER' as const };
    await createActionItem(mockToken, personId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/action-items`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the created action item', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockActionItem),
    });

    const result = await createActionItem(mockToken, personId, { title: 'Task' });
    expect(result).toEqual(mockActionItem);
  });

  it('should throw ApiException on error', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      json: () => Promise.resolve({ status: 404, error: 'Not Found', message: 'Person not found', timestamp: '2026-05-10T10:00:00Z' }),
    });

    await expect(createActionItem(mockToken, personId, { title: 'Task' })).rejects.toThrow(ApiException);
  });
});

describe('getActionItem', () => {
  it('should send GET request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockActionItem),
    });

    await getActionItem(mockToken, personId, actionItemId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/action-items/${actionItemId}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the action item', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockActionItem),
    });

    const result = await getActionItem(mockToken, personId, actionItemId);
    expect(result).toEqual(mockActionItem);
  });
});

describe('updateActionItem', () => {
  it('should send PUT request with correct URL and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockActionItem, title: 'Updated' }),
    });

    const data = { title: 'Updated' };
    await updateActionItem(mockToken, personId, actionItemId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/action-items/${actionItemId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('completeActionItem', () => {
  it('should send POST request to complete endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockActionItem, status: 'DONE' }),
    });

    const result = await completeActionItem(mockToken, personId, actionItemId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/action-items/${actionItemId}/complete`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.status).toBe('DONE');
  });
});

describe('cancelActionItem', () => {
  it('should send POST request to cancel endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockActionItem, status: 'CANCELED' }),
    });

    const result = await cancelActionItem(mockToken, personId, actionItemId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/action-items/${actionItemId}/cancel`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.status).toBe('CANCELED');
  });
});

describe('deleteActionItem', () => {
  it('should send DELETE request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({}),
    });

    await deleteActionItem(mockToken, personId, actionItemId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/action-items/${actionItemId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('listActionItemsByPerson', () => {
  it('should send GET request with no params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await listActionItemsByPerson(mockToken, personId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/action-items`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should include status filter in query params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await listActionItemsByPerson(mockToken, personId, { status: 'OPEN' });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('status=OPEN'),
      expect.any(Object)
    );
  });

  it('should include pagination params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await listActionItemsByPerson(mockToken, personId, { page: 1, size: 10 });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('page=1'),
      expect.any(Object)
    );
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('size=10'),
      expect.any(Object)
    );
  });
});

describe('listAllActionItems', () => {
  it('should send GET request to /action-items', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await listAllActionItems(mockToken);

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/action-items', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should include overdueOnly param', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await listAllActionItems(mockToken, { overdueOnly: true });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('overdueOnly=true'),
      expect.any(Object)
    );
  });

  it('should include status filter', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await listAllActionItems(mockToken, { status: 'DONE' });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('status=DONE'),
      expect.any(Object)
    );
  });
});
