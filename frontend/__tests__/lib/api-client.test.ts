import {
  createPerson,
  getPerson,
  updatePerson,
  deletePerson,
  listPersons,
  setMorale,
  addRememberItem,
  removeRememberItem,
  reorderRememberItems,
  restorePerson,
  permanentlyDeletePerson,
  listDeletedPersons,
} from '@/lib/api-client';
import { ApiException } from '@/types/api';

const mockToken = 'test-jwt-token';

const mockPerson = {
  id: '550e8400-e29b-41d4-a716-446655440000',
  name: 'Jane Smith',
  preferredName: 'Jane',
  roleTitle: 'Senior Engineer',
  timezone: 'Europe/Berlin',
  startDate: '2024-03-15',
  email: 'jane@example.com',
  tags: ['engineering', 'senior'],
  moraleStatus: 'UNKNOWN' as const,
  moraleNote: null,
  pinnedRememberItems: [],
  atAGlance: {
    last1on1Date: null,
    openActionItemsCount: null,
    activePdpGoalsSummary: null,
  },
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-08T12:00:00Z',
};

let mockFetch: jest.Mock;

beforeEach(() => {
  mockFetch = jest.fn();
  global.fetch = mockFetch;
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('createPerson', () => {
  it('should send POST request with correct URL, headers, and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPerson),
    });

    const data = {
      name: 'Jane Smith',
      preferredName: 'Jane',
      roleTitle: 'Senior Engineer',
      timezone: 'Europe/Berlin',
      startDate: '2024-03-15',
      email: 'jane@example.com',
      tags: ['engineering', 'senior'],
    };

    await createPerson(mockToken, data);

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/persons', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the created person', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPerson),
    });

    const result = await createPerson(mockToken, { name: 'Jane Smith' });
    expect(result).toEqual(mockPerson);
  });
});

describe('getPerson', () => {
  it('should send GET request with correct URL and headers', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPerson),
    });

    const personId = '550e8400-e29b-41d4-a716-446655440000';
    await getPerson(mockToken, personId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the person', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPerson),
    });

    const result = await getPerson(mockToken, mockPerson.id);
    expect(result).toEqual(mockPerson);
  });
});

describe('updatePerson', () => {
  it('should send PUT request with correct URL, headers, and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockPerson, name: 'Jane Doe' }),
    });

    const personId = '550e8400-e29b-41d4-a716-446655440000';
    const data = { name: 'Jane Doe', tags: ['engineering'] };

    await updatePerson(mockToken, personId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the updated person', async () => {
    const updatedPerson = { ...mockPerson, name: 'Jane Doe' };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(updatedPerson),
    });

    const result = await updatePerson(mockToken, mockPerson.id, { name: 'Jane Doe' });
    expect(result).toEqual(updatedPerson);
  });
});

describe('deletePerson', () => {
  it('should send DELETE request with correct URL and headers', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({}),
    });

    const personId = '550e8400-e29b-41d4-a716-446655440000';
    await deletePerson(mockToken, personId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should not return a value', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({}),
    });

    const result = await deletePerson(mockToken, mockPerson.id);
    expect(result).toBeUndefined();
  });
});

describe('listPersons', () => {
  const paginatedResponse = {
    content: [mockPerson],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  };

  it('should send GET request without query params when none provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(paginatedResponse),
    });

    await listPersons(mockToken);

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/persons', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should include page and size query params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(paginatedResponse),
    });

    await listPersons(mockToken, { page: 1, size: 10 });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/persons?page=1&size=10',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
        }),
      })
    );
  });

  it('should include tag filter query param', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(paginatedResponse),
    });

    await listPersons(mockToken, { tag: 'engineering' });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/persons?tag=engineering',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
        }),
      })
    );
  });

  it('should include morale filter query param', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(paginatedResponse),
    });

    await listPersons(mockToken, { morale: 'GREEN' });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/persons?morale=GREEN',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
        }),
      })
    );
  });

  it('should include all query params when all provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(paginatedResponse),
    });

    await listPersons(mockToken, { page: 2, size: 5, tag: 'senior', morale: 'RED' });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/persons?page=2&size=5&tag=senior&morale=RED',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
        }),
      })
    );
  });

  it('should return paginated response', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(paginatedResponse),
    });

    const result = await listPersons(mockToken);
    expect(result).toEqual(paginatedResponse);
  });
});

describe('setMorale', () => {
  it('should send PUT request with correct URL, headers, and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockPerson, moraleStatus: 'GREEN', moraleNote: 'Great sprint' }),
    });

    const personId = '550e8400-e29b-41d4-a716-446655440000';
    const data = { status: 'GREEN' as const, note: 'Great sprint' };

    await setMorale(mockToken, personId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/morale`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the updated person', async () => {
    const updatedPerson = { ...mockPerson, moraleStatus: 'GREEN' as const, moraleNote: 'Great sprint' };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(updatedPerson),
    });

    const result = await setMorale(mockToken, mockPerson.id, { status: 'GREEN', note: 'Great sprint' });
    expect(result).toEqual(updatedPerson);
  });
});

describe('addRememberItem', () => {
  const mockItems = [
    { id: 'item-1', text: 'Prefers async communication', color: 'cyan', tag: null, sensitive: false, displayOrder: 0, createdAt: '2025-05-08T12:00:00Z' },
  ];

  it('should send POST request with correct URL, headers, and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockItems),
    });

    const personId = '550e8400-e29b-41d4-a716-446655440000';
    const data = { text: 'Prefers async communication' };

    await addRememberItem(mockToken, personId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/remember-items`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the updated remember items list', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockItems),
    });

    const result = await addRememberItem(mockToken, mockPerson.id, { text: 'Prefers async communication' });
    expect(result).toEqual(mockItems);
  });
});

describe('removeRememberItem', () => {
  const mockItems: never[] = [];

  it('should send DELETE request with correct URL and headers', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockItems),
    });

    const personId = '550e8400-e29b-41d4-a716-446655440000';
    const itemId = 'item-1';

    await removeRememberItem(mockToken, personId, itemId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/remember-items/${itemId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the updated remember items list', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockItems),
    });

    const result = await removeRememberItem(mockToken, mockPerson.id, 'item-1');
    expect(result).toEqual(mockItems);
  });
});

describe('reorderRememberItems', () => {
  const mockItems = [
    { id: 'item-2', text: 'Second item', displayOrder: 0, createdAt: '2025-05-08T12:00:00Z' },
    { id: 'item-1', text: 'First item', displayOrder: 1, createdAt: '2025-05-08T11:00:00Z' },
  ];

  it('should send PUT request with correct URL, headers, and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockItems),
    });

    const personId = '550e8400-e29b-41d4-a716-446655440000';
    const orderedIds = ['item-2', 'item-1'];

    await reorderRememberItems(mockToken, personId, orderedIds);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/remember-items/reorder`, {
      method: 'PUT',
      body: JSON.stringify({ orderedIds }),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the reordered remember items list', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockItems),
    });

    const result = await reorderRememberItems(mockToken, mockPerson.id, ['item-2', 'item-1']);
    expect(result).toEqual(mockItems);
  });
});

describe('error handling', () => {
  const errorResponse = {
    status: 400,
    error: 'Bad Request',
    message: 'Name must not be blank',
    timestamp: '2025-05-08T12:00:00Z',
  };

  it('should throw ApiException when response is not ok', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      json: () => Promise.resolve(errorResponse),
    });

    await expect(createPerson(mockToken, { name: '' })).rejects.toThrow(ApiException);
  });

  it('should map error fields correctly to ApiException', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      json: () => Promise.resolve(errorResponse),
    });

    try {
      await createPerson(mockToken, { name: '' });
      fail('Expected ApiException to be thrown');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiException);
      const apiError = error as ApiException;
      expect(apiError.status).toBe(400);
      expect(apiError.error).toBe('Bad Request');
      expect(apiError.message).toBe('Name must not be blank');
      expect(apiError.timestamp).toBe('2025-05-08T12:00:00Z');
      expect(apiError.name).toBe('ApiException');
    }
  });

  it('should throw ApiException for 404 Not Found', async () => {
    const notFoundError = {
      status: 404,
      error: 'Not Found',
      message: 'Person not found',
      timestamp: '2025-05-08T12:00:00Z',
    };

    mockFetch.mockResolvedValue({
      ok: false,
      json: () => Promise.resolve(notFoundError),
    });

    try {
      await getPerson(mockToken, 'non-existent-id');
      fail('Expected ApiException to be thrown');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiException);
      const apiError = error as ApiException;
      expect(apiError.status).toBe(404);
      expect(apiError.error).toBe('Not Found');
      expect(apiError.message).toBe('Person not found');
    }
  });

  it('should throw ApiException for 401 Unauthorized', async () => {
    const unauthorizedError = {
      status: 401,
      error: 'Unauthorized',
      message: 'Invalid or expired token',
      timestamp: '2025-05-08T12:00:00Z',
    };

    mockFetch.mockResolvedValue({
      ok: false,
      json: () => Promise.resolve(unauthorizedError),
    });

    try {
      await listPersons(mockToken);
      fail('Expected ApiException to be thrown');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiException);
      const apiError = error as ApiException;
      expect(apiError.status).toBe(401);
      expect(apiError.error).toBe('Unauthorized');
    }
  });
});

describe('restorePerson', () => {
  it('should send POST request to restore endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockPerson, deletedAt: null }),
    });

    const result = await restorePerson(mockToken, '550e8400-e29b-41d4-a716-446655440000');

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/persons/550e8400-e29b-41d4-a716-446655440000/restore', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.id).toBe('550e8400-e29b-41d4-a716-446655440000');
  });

  it('should throw ApiException on 404', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      json: () => Promise.resolve({
        status: 404,
        error: 'Not Found',
        message: 'Person not found',
        timestamp: '2025-05-10T12:00:00Z',
      }),
    });

    await expect(restorePerson(mockToken, 'nonexistent-id')).rejects.toBeInstanceOf(ApiException);
  });
});

describe('permanentlyDeletePerson', () => {
  it('should send DELETE request to permanent endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(undefined),
    });

    await permanentlyDeletePerson(mockToken, '550e8400-e29b-41d4-a716-446655440000');

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/persons/550e8400-e29b-41d4-a716-446655440000/permanent', {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should throw ApiException on 404', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      json: () => Promise.resolve({
        status: 404,
        error: 'Not Found',
        message: 'Person not found',
        timestamp: '2025-05-10T12:00:00Z',
      }),
    });

    await expect(permanentlyDeletePerson(mockToken, 'nonexistent-id')).rejects.toBeInstanceOf(ApiException);
  });
});

describe('listDeletedPersons', () => {
  it('should send GET request to trash endpoint', async () => {
    const paginatedResponse = {
      content: [{ ...mockPerson, deletedAt: '2025-05-10T12:00:00Z' }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(paginatedResponse),
    });

    const result = await listDeletedPersons(mockToken);

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/persons/trash', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].deletedAt).toBe('2025-05-10T12:00:00Z');
  });

  it('should send pagination params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 }),
    });

    await listDeletedPersons(mockToken, { page: 1, size: 10 });

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/persons/trash?page=1&size=10', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should throw ApiException on error', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: () => Promise.resolve({
        status: 401,
        error: 'Unauthorized',
        message: 'Authentication required',
        timestamp: '2025-05-10T12:00:00Z',
      }),
    });

    await expect(listDeletedPersons(mockToken)).rejects.toBeInstanceOf(ApiException);
  });
});
