import { generateAiNarrative } from '@/lib/api-client';
import { ApiException } from '@/types/api';

const mockToken = 'test-jwt-token';
const mockPersonId = '550e8400-e29b-41d4-a716-446655440000';

let mockFetch: jest.Mock;

beforeEach(() => {
  mockFetch = jest.fn();
  global.fetch = mockFetch;
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('generateAiNarrative', () => {
  it('should send POST request to correct URL with date params in body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ narrative: 'Generated narrative', error: null }),
    });

    await generateAiNarrative(mockToken, mockPersonId, {
      dateFrom: '2026-01-01',
      dateTo: '2026-06-30',
    });

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${mockPersonId}/ai-narrative`,
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
          'Content-Type': 'application/json',
        }),
        body: JSON.stringify({ dateFrom: '2026-01-01', dateTo: '2026-06-30' }),
      })
    );
  });

  it('should return narrative on success', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        narrative: 'Alice demonstrated exceptional leadership...',
        error: null,
      }),
    });

    const result = await generateAiNarrative(mockToken, mockPersonId, {
      dateFrom: '2026-01-01',
      dateTo: '2026-06-30',
    });

    expect(result.narrative).toBe('Alice demonstrated exceptional leadership...');
    expect(result.error).toBeNull();
  });

  it('should return error message when AI fails', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        narrative: null,
        error: 'Cannot connect to AI API.',
      }),
    });

    const result = await generateAiNarrative(mockToken, mockPersonId, {
      dateFrom: '2026-01-01',
      dateTo: '2026-06-30',
    });

    expect(result.narrative).toBeNull();
    expect(result.error).toBe('Cannot connect to AI API.');
  });

  it('should throw ApiException on 404', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      json: () => Promise.resolve({
        status: 404,
        error: 'Not Found',
        message: 'Person not found',
        timestamp: '2026-01-01T00:00:00Z',
      }),
    });

    await expect(
      generateAiNarrative(mockToken, mockPersonId, {
        dateFrom: '2026-01-01',
        dateTo: '2026-06-30',
      })
    ).rejects.toThrow(ApiException);
  });

  it('should throw ApiException on 401', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: () => Promise.reject(new Error('no json')),
    });

    await expect(
      generateAiNarrative(mockToken, mockPersonId, {
        dateFrom: '2026-01-01',
        dateTo: '2026-06-30',
      })
    ).rejects.toThrow(ApiException);
  });

  it('should throw ApiException on 400 (bad request)', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      json: () => Promise.resolve({
        status: 400,
        error: 'Bad Request',
        message: 'Validation failed',
        timestamp: '2026-01-01T00:00:00Z',
      }),
    });

    await expect(
      generateAiNarrative(mockToken, mockPersonId, {
        dateFrom: '2026-06-30',
        dateTo: '2026-01-01',
      })
    ).rejects.toThrow(ApiException);
  });
});
