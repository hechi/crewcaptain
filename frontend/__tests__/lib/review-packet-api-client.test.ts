import { generateReviewPacket } from '@/lib/api-client';
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

describe('generateReviewPacket', () => {
  const mockMarkdown = '# Review Packet: Jane Smith\n\n## Executive Summary\n';

  it('should send GET request to correct URL with date params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(mockMarkdown),
    });

    await generateReviewPacket(mockToken, mockPersonId, {
      dateFrom: '2024-01-01',
      dateTo: '2024-06-30',
    });

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${mockPersonId}/review-packet?dateFrom=2024-01-01&dateTo=2024-06-30`,
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
          'Content-Type': 'application/json',
        }),
      })
    );
  });

  it('should return markdown text', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(mockMarkdown),
    });

    const result = await generateReviewPacket(mockToken, mockPersonId, {
      dateFrom: '2024-01-01',
      dateTo: '2024-06-30',
    });

    expect(result).toBe(mockMarkdown);
  });

  it('should always include both dateFrom and dateTo parameters', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(mockMarkdown),
    });

    await generateReviewPacket(mockToken, mockPersonId, {
      dateFrom: '2024-03-15',
      dateTo: '2024-09-15',
    });

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${mockPersonId}/review-packet?dateFrom=2024-03-15&dateTo=2024-09-15`,
      expect.any(Object)
    );
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
        timestamp: '2024-01-01T00:00:00Z',
      }),
    });

    await expect(
      generateReviewPacket(mockToken, mockPersonId, {
        dateFrom: '2024-01-01',
        dateTo: '2024-06-30',
      })
    ).rejects.toThrow(ApiException);
  });

  it('should throw ApiException on 400 (invalid date range)', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      json: () => Promise.resolve({
        status: 400,
        error: 'Bad Request',
        message: 'dateTo must not be before dateFrom',
        timestamp: '2024-01-01T00:00:00Z',
      }),
    });

    await expect(
      generateReviewPacket(mockToken, mockPersonId, {
        dateFrom: '2024-06-30',
        dateTo: '2024-01-01',
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
      generateReviewPacket(mockToken, mockPersonId, {
        dateFrom: '2024-01-01',
        dateTo: '2024-06-30',
      })
    ).rejects.toThrow(ApiException);
  });
});
