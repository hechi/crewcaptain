import { getAuditLog } from '@/lib/api-client';

const mockToken = 'test-jwt-token';

const mockAuditLogEntry = {
  id: '550e8400-e29b-41d4-a716-446655440001',
  action: 'CREATE' as const,
  entityType: 'PERSON' as const,
  entityId: '550e8400-e29b-41d4-a716-446655440002',
  personId: '550e8400-e29b-41d4-a716-446655440002',
  summary: 'Created person "John Doe"',
  createdAt: '2026-05-11T10:00:00Z',
};

const mockPaginatedResponse = {
  content: [mockAuditLogEntry],
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

describe('getAuditLog', () => {
  it('should send GET request with correct URL and auth header', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    const result = await getAuditLog(mockToken);

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/audit-log',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: `Bearer ${mockToken}`,
          'Content-Type': 'application/json',
        }),
      })
    );
    expect(result).toEqual(mockPaginatedResponse);
  });

  it('should include entityType filter in query params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await getAuditLog(mockToken, { entityType: 'PERSON' });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('entityType=PERSON'),
      expect.any(Object)
    );
  });

  it('should include action filter in query params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await getAuditLog(mockToken, { action: 'DELETE' });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('action=DELETE'),
      expect.any(Object)
    );
  });

  it('should include both filters in query params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await getAuditLog(mockToken, { entityType: 'ACTION_ITEM', action: 'CREATE' });

    const url = mockFetch.mock.calls[0][0];
    expect(url).toContain('entityType=ACTION_ITEM');
    expect(url).toContain('action=CREATE');
  });

  it('should include pagination params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await getAuditLog(mockToken, { page: 2, size: 10 });

    const url = mockFetch.mock.calls[0][0];
    expect(url).toContain('page=2');
    expect(url).toContain('size=10');
  });

  it('should throw ApiException on error response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: () => Promise.resolve({
        status: 401,
        error: 'Unauthorized',
        message: 'Invalid token',
        timestamp: '2026-05-11T10:00:00Z',
      }),
    });

    await expect(getAuditLog(mockToken)).rejects.toThrow();
  });

  it('should return empty content when no entries exist', async () => {
    const emptyResponse = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(emptyResponse),
    });

    const result = await getAuditLog(mockToken);

    expect(result.content).toHaveLength(0);
    expect(result.totalElements).toBe(0);
  });
});
