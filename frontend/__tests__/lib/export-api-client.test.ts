import { exportPersonMarkdown } from '@/lib/api-client';
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

describe('exportPersonMarkdown', () => {
  const mockMarkdown = '# Jane Smith\n\n## Profile\n\n| Field | Value |\n';

  it('should send GET request to correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(mockMarkdown),
    });

    await exportPersonMarkdown(mockToken, mockPersonId);

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${mockPersonId}/export`,
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

    const result = await exportPersonMarkdown(mockToken, mockPersonId);

    expect(result).toBe(mockMarkdown);
  });

  it('should include dateFrom parameter when provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(mockMarkdown),
    });

    await exportPersonMarkdown(mockToken, mockPersonId, { dateFrom: '2024-01-01' });

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${mockPersonId}/export?dateFrom=2024-01-01`,
      expect.any(Object)
    );
  });

  it('should include dateTo parameter when provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(mockMarkdown),
    });

    await exportPersonMarkdown(mockToken, mockPersonId, { dateTo: '2024-06-30' });

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${mockPersonId}/export?dateTo=2024-06-30`,
      expect.any(Object)
    );
  });

  it('should include both dateFrom and dateTo parameters when provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(mockMarkdown),
    });

    await exportPersonMarkdown(mockToken, mockPersonId, {
      dateFrom: '2024-01-01',
      dateTo: '2024-06-30',
    });

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${mockPersonId}/export?dateFrom=2024-01-01&dateTo=2024-06-30`,
      expect.any(Object)
    );
  });

  it('should not include query params when none provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(mockMarkdown),
    });

    await exportPersonMarkdown(mockToken, mockPersonId);

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${mockPersonId}/export`,
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

    await expect(exportPersonMarkdown(mockToken, mockPersonId)).rejects.toThrow(ApiException);
  });

  it('should throw ApiException on 401', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: () => Promise.reject(new Error('no json')),
    });

    await expect(exportPersonMarkdown(mockToken, mockPersonId)).rejects.toThrow(ApiException);
  });
});
