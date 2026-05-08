import {
  upsertOneOnOneSeries,
  getOneOnOneSeries,
  createOneOnOneEntry,
  getOneOnOneEntry,
  updateOneOnOneEntry,
  deleteOneOnOneEntry,
  listOneOnOneEntries,
} from '@/lib/api-client';
import { ApiException } from '@/types/api';

const mockToken = 'test-jwt-token';
const personId = '550e8400-e29b-41d4-a716-446655440000';
const entryId = '660e8400-e29b-41d4-a716-446655440001';

const mockSeries = {
  id: '770e8400-e29b-41d4-a716-446655440002',
  personId,
  cadenceType: 'BIWEEKLY' as const,
  customIntervalDays: null,
  templateMarkdown: '## Agenda\n- [ ] Review action items\n',
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-08T12:00:00Z',
};

const mockEntry = {
  id: entryId,
  personId,
  meetingDate: '2025-05-08T14:00:00Z',
  agendaItems: [
    { id: 'a1', text: 'Review Q2 goals', checked: false, displayOrder: 0, createdAt: '2025-05-08T14:00:00Z' },
    { id: 'a2', text: 'Discuss timeline', checked: true, displayOrder: 1, createdAt: '2025-05-08T14:00:00Z' },
  ],
  notesMarkdown: '## Discussion\nTalked about goals.',
  outcomesMarkdown: 'Agreed to extend deadline.',
  sensitive: false,
  createdAt: '2025-05-08T14:00:00Z',
  updatedAt: '2025-05-08T14:00:00Z',
};

let mockFetch: jest.Mock;

beforeEach(() => {
  mockFetch = jest.fn();
  global.fetch = mockFetch;
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('upsertOneOnOneSeries', () => {
  it('should send PUT request with correct URL, headers, and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSeries),
    });

    const data = {
      cadenceType: 'BIWEEKLY' as const,
      customIntervalDays: null,
      templateMarkdown: '## Agenda\n- [ ] Review action items\n',
    };

    await upsertOneOnOneSeries(mockToken, personId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/one-on-one-series`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the series response', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSeries),
    });

    const result = await upsertOneOnOneSeries(mockToken, personId, {
      cadenceType: 'BIWEEKLY',
    });
    expect(result).toEqual(mockSeries);
  });
});

describe('getOneOnOneSeries', () => {
  it('should send GET request with correct URL and headers', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSeries),
    });

    await getOneOnOneSeries(mockToken, personId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/one-on-one-series`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the series', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSeries),
    });

    const result = await getOneOnOneSeries(mockToken, personId);
    expect(result).toEqual(mockSeries);
  });
});

describe('createOneOnOneEntry', () => {
  it('should send POST request with correct URL, headers, and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockEntry),
    });

    const data = {
      meetingDate: '2025-05-08T14:00:00Z',
      agendaItems: [
        { text: 'Review Q2 goals', checked: false },
        { text: 'Discuss timeline', checked: true },
      ],
      notesMarkdown: '## Discussion\nTalked about goals.',
      outcomesMarkdown: 'Agreed to extend deadline.',
      sensitive: false,
    };

    await createOneOnOneEntry(mockToken, personId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/one-on-one-entries`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the created entry', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockEntry),
    });

    const result = await createOneOnOneEntry(mockToken, personId, {
      meetingDate: '2025-05-08T14:00:00Z',
    });
    expect(result).toEqual(mockEntry);
  });
});

describe('getOneOnOneEntry', () => {
  it('should send GET request with correct URL including entryId and headers', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockEntry),
    });

    await getOneOnOneEntry(mockToken, personId, entryId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/one-on-one-entries/${entryId}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the entry', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockEntry),
    });

    const result = await getOneOnOneEntry(mockToken, personId, entryId);
    expect(result).toEqual(mockEntry);
  });
});

describe('updateOneOnOneEntry', () => {
  it('should send PUT request with correct URL including entryId, headers, and body', async () => {
    const updatedEntry = { ...mockEntry, notesMarkdown: 'Updated notes' };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(updatedEntry),
    });

    const data = {
      notesMarkdown: 'Updated notes',
      sensitive: true,
    };

    await updateOneOnOneEntry(mockToken, personId, entryId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/one-on-one-entries/${entryId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the updated entry', async () => {
    const updatedEntry = { ...mockEntry, sensitive: true };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(updatedEntry),
    });

    const result = await updateOneOnOneEntry(mockToken, personId, entryId, { sensitive: true });
    expect(result).toEqual(updatedEntry);
  });
});

describe('deleteOneOnOneEntry', () => {
  it('should send DELETE request with correct URL including entryId and headers', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({}),
    });

    await deleteOneOnOneEntry(mockToken, personId, entryId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/one-on-one-entries/${entryId}`, {
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

    const result = await deleteOneOnOneEntry(mockToken, personId, entryId);
    expect(result).toBeUndefined();
  });
});

describe('listOneOnOneEntries', () => {
  const paginatedResponse = {
    content: [mockEntry],
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

    await listOneOnOneEntries(mockToken, personId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/one-on-one-entries`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should include page and size query params when provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(paginatedResponse),
    });

    await listOneOnOneEntries(mockToken, personId, 2, 10);

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${personId}/one-on-one-entries?page=2&size=10`,
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
        }),
      })
    );
  });

  it('should include only page param when size is not provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(paginatedResponse),
    });

    await listOneOnOneEntries(mockToken, personId, 1);

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${personId}/one-on-one-entries?page=1`,
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

    const result = await listOneOnOneEntries(mockToken, personId);
    expect(result).toEqual(paginatedResponse);
  });
});

describe('1:1 API error handling', () => {
  it('should throw ApiException when response is not ok', async () => {
    const errorResponse = {
      status: 404,
      error: 'Not Found',
      message: '1:1 entry not found',
      timestamp: '2025-05-08T12:00:00Z',
    };

    mockFetch.mockResolvedValue({
      ok: false,
      json: () => Promise.resolve(errorResponse),
    });

    await expect(getOneOnOneEntry(mockToken, personId, 'non-existent-id')).rejects.toThrow(ApiException);
  });

  it('should map error fields correctly to ApiException', async () => {
    const errorResponse = {
      status: 400,
      error: 'Bad Request',
      message: 'Agenda item text must not be blank',
      timestamp: '2025-05-08T12:00:00Z',
    };

    mockFetch.mockResolvedValue({
      ok: false,
      json: () => Promise.resolve(errorResponse),
    });

    try {
      await createOneOnOneEntry(mockToken, personId, {
        meetingDate: '2025-05-08T14:00:00Z',
        agendaItems: [{ text: '' }],
      });
      fail('Expected ApiException to be thrown');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiException);
      const apiError = error as ApiException;
      expect(apiError.status).toBe(400);
      expect(apiError.error).toBe('Bad Request');
      expect(apiError.message).toBe('Agenda item text must not be blank');
      expect(apiError.timestamp).toBe('2025-05-08T12:00:00Z');
      expect(apiError.name).toBe('ApiException');
    }
  });

  it('should throw ApiException for 401 Unauthorized on series endpoint', async () => {
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
      await getOneOnOneSeries(mockToken, personId);
      fail('Expected ApiException to be thrown');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiException);
      const apiError = error as ApiException;
      expect(apiError.status).toBe(401);
      expect(apiError.error).toBe('Unauthorized');
    }
  });

  it('should throw ApiException for 404 when person not found on upsert series', async () => {
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
      await upsertOneOnOneSeries(mockToken, 'non-existent-person', { cadenceType: 'WEEKLY' });
      fail('Expected ApiException to be thrown');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiException);
      const apiError = error as ApiException;
      expect(apiError.status).toBe(404);
      expect(apiError.message).toBe('Person not found');
    }
  });
});
