import { generateTrendRadar } from '@/lib/api-client';
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

describe('generateTrendRadar', () => {
  it('should send POST request to correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        insights: [],
        insufficientData: true,
        meetingsNeeded: 3,
        error: 'Need more data',
      }),
    });

    await generateTrendRadar(mockToken, mockPersonId);

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${mockPersonId}/ai-trend-radar`,
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
          'Content-Type': 'application/json',
        }),
      })
    );
  });

  it('should return insights on success', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        insights: [
          { title: 'Burnout Risk', description: 'High workload.', dimension: 'MORALE', confidenceScore: 72 },
          { title: 'Growth Gap', description: 'No PDP updates.', dimension: 'WORK_GROWTH_BALANCE', confidenceScore: 55 },
        ],
        insufficientData: false,
        meetingsNeeded: null,
        error: null,
      }),
    });

    const result = await generateTrendRadar(mockToken, mockPersonId);

    expect(result.insights).toHaveLength(2);
    expect(result.insights[0].title).toBe('Burnout Risk');
    expect(result.insights[0].confidenceScore).toBe(72);
    expect(result.insufficientData).toBe(false);
    expect(result.error).toBeNull();
  });

  it('should return insufficient data response', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        insights: [],
        insufficientData: true,
        meetingsNeeded: 2,
        error: 'Scanning horizon... Need 2 more 1:1(s) to establish a baseline.',
      }),
    });

    const result = await generateTrendRadar(mockToken, mockPersonId);

    expect(result.insights).toHaveLength(0);
    expect(result.insufficientData).toBe(true);
    expect(result.meetingsNeeded).toBe(2);
    expect(result.error).toContain('Scanning horizon');
  });

  it('should return error when AI fails', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        insights: [],
        insufficientData: false,
        meetingsNeeded: null,
        error: 'Cannot connect to AI API.',
      }),
    });

    const result = await generateTrendRadar(mockToken, mockPersonId);

    expect(result.insights).toHaveLength(0);
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

    await expect(generateTrendRadar(mockToken, mockPersonId)).rejects.toThrow(ApiException);
  });

  it('should throw ApiException on 401', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: () => Promise.reject(new Error('no json')),
    });

    await expect(generateTrendRadar(mockToken, mockPersonId)).rejects.toThrow(ApiException);
  });
});
