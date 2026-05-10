import { importPersonsCsv } from '@/lib/api-client';
import { ApiException } from '@/types/api';

const mockToken = 'test-jwt-token';

let mockFetch: jest.Mock;

beforeEach(() => {
  mockFetch = jest.fn();
  global.fetch = mockFetch;
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('importPersonsCsv', () => {
  const mockFile = new File(['name,email\nAlice,alice@example.com'], 'people.csv', { type: 'text/csv' });

  it('should send POST request to correct URL with multipart form data', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ successCount: 1, errorCount: 0, errors: [] }),
    });

    await importPersonsCsv(mockToken, mockFile);

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/persons/import',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
        }),
      })
    );
  });

  it('should send file as FormData', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ successCount: 1, errorCount: 0, errors: [] }),
    });

    await importPersonsCsv(mockToken, mockFile);

    const callArgs = mockFetch.mock.calls[0];
    const body = callArgs[1].body;
    expect(body).toBeInstanceOf(FormData);
    expect(body.get('file')).toBe(mockFile);
  });

  it('should not include Content-Type header (let browser set multipart boundary)', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ successCount: 1, errorCount: 0, errors: [] }),
    });

    await importPersonsCsv(mockToken, mockFile);

    const callArgs = mockFetch.mock.calls[0];
    const headers = callArgs[1].headers;
    expect(headers['Content-Type']).toBeUndefined();
  });

  it('should return BulkImportResponse on success', async () => {
    const mockResponse = { successCount: 3, errorCount: 1, errors: ['Row 4: Name must not be blank'] };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    });

    const result = await importPersonsCsv(mockToken, mockFile);

    expect(result).toEqual(mockResponse);
  });

  it('should throw ApiException on non-ok response with JSON body', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      json: () => Promise.resolve({
        status: 400,
        error: 'Bad Request',
        message: 'File must be a CSV',
        timestamp: '2026-05-10T00:00:00Z',
      }),
    });

    await expect(importPersonsCsv(mockToken, mockFile)).rejects.toThrow(ApiException);
    await expect(importPersonsCsv(mockToken, mockFile)).rejects.toMatchObject({
      status: 400,
      message: 'File must be a CSV',
    });
  });

  it('should throw ApiException with fallback message on non-ok response without JSON', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
      json: () => Promise.reject(new Error('not json')),
    });

    await expect(importPersonsCsv(mockToken, mockFile)).rejects.toThrow(ApiException);
    await expect(importPersonsCsv(mockToken, mockFile)).rejects.toMatchObject({
      status: 500,
    });
  });

  it('should handle 401 unauthorized response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: () => Promise.reject(new Error('not json')),
    });

    await expect(importPersonsCsv(mockToken, mockFile)).rejects.toThrow(ApiException);
    await expect(importPersonsCsv(mockToken, mockFile)).rejects.toMatchObject({
      status: 401,
    });
  });

  it('should return zero counts when all rows fail', async () => {
    const mockResponse = {
      successCount: 0,
      errorCount: 3,
      errors: ['Row 2: Name must not be blank', 'Row 3: Name must not be blank', 'Row 4: Invalid start_date'],
    };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    });

    const result = await importPersonsCsv(mockToken, mockFile);

    expect(result.successCount).toBe(0);
    expect(result.errorCount).toBe(3);
    expect(result.errors).toHaveLength(3);
  });
});
