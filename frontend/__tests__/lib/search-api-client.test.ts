import { search } from '@/lib/api-client';

const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('search API client', () => {
  const token = 'test-token';

  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('should call search endpoint with query parameter', async () => {
    const mockResponse = {
      results: [],
      query: 'test',
      totalCount: 0,
      page: 0,
      size: 20,
      totalPages: 0,
    };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    });

    const result = await search(token, { q: 'test' });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/search?q=test'),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer test-token',
        }),
      })
    );
    expect(result).toEqual(mockResponse);
  });

  it('should include type filters in request', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ results: [], query: 'test', totalCount: 0, page: 0, size: 20, totalPages: 0 }),
    });

    await search(token, { q: 'test', type: ['PERSON', 'ACTION_ITEM'] });

    const calledUrl = mockFetch.mock.calls[0][0];
    expect(calledUrl).toContain('type=PERSON');
    expect(calledUrl).toContain('type=ACTION_ITEM');
  });

  it('should include pagination parameters', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ results: [], query: 'test', totalCount: 0, page: 2, size: 10, totalPages: 0 }),
    });

    await search(token, { q: 'test', page: 2, size: 10 });

    const calledUrl = mockFetch.mock.calls[0][0];
    expect(calledUrl).toContain('page=2');
    expect(calledUrl).toContain('size=10');
  });

  it('should not include type parameter when no types specified', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ results: [], query: 'test', totalCount: 0, page: 0, size: 20, totalPages: 0 }),
    });

    await search(token, { q: 'test' });

    const calledUrl = mockFetch.mock.calls[0][0];
    expect(calledUrl).not.toContain('type=');
  });

  it('should not include type parameter when empty array', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ results: [], query: 'test', totalCount: 0, page: 0, size: 20, totalPages: 0 }),
    });

    await search(token, { q: 'test', type: [] });

    const calledUrl = mockFetch.mock.calls[0][0];
    expect(calledUrl).not.toContain('type=');
  });

  it('should throw ApiException on error response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      json: () => Promise.resolve({
        status: 400,
        error: 'Bad Request',
        message: 'Query parameter is required',
        timestamp: '2026-05-10T10:00:00Z',
      }),
    });

    await expect(search(token, { q: 'test' })).rejects.toThrow('Query parameter is required');
  });

  it('should throw ApiException on 401 response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: () => Promise.reject(new Error('no json')),
    });

    await expect(search(token, { q: 'test' })).rejects.toThrow();
  });

  it('should return results with all fields', async () => {
    const mockResponse = {
      results: [
        {
          id: '123e4567-e89b-12d3-a456-426614174000',
          type: 'PERSON',
          title: 'John Doe',
          snippet: 'Software Engineer',
          personId: '123e4567-e89b-12d3-a456-426614174000',
          personName: 'John Doe',
          sensitive: false,
          createdAt: '2026-05-10T10:00:00Z',
          relevanceScore: 0.85,
        },
      ],
      query: 'john',
      totalCount: 1,
      page: 0,
      size: 20,
      totalPages: 1,
    };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    });

    const result = await search(token, { q: 'john' });

    expect(result.results).toHaveLength(1);
    expect(result.results[0].type).toBe('PERSON');
    expect(result.results[0].title).toBe('John Doe');
    expect(result.results[0].snippet).toBe('Software Engineer');
    expect(result.totalCount).toBe(1);
    expect(result.totalPages).toBe(1);
  });

  it('should handle multiple type filters', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ results: [], query: 'test', totalCount: 0, page: 0, size: 20, totalPages: 0 }),
    });

    await search(token, { q: 'test', type: ['PERSON', 'ACTION_ITEM', 'KUDOS'] });

    const calledUrl = mockFetch.mock.calls[0][0];
    expect(calledUrl).toContain('type=PERSON');
    expect(calledUrl).toContain('type=ACTION_ITEM');
    expect(calledUrl).toContain('type=KUDOS');
  });

  it('should always include q parameter', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ results: [], query: 'hello world', totalCount: 0, page: 0, size: 20, totalPages: 0 }),
    });

    await search(token, { q: 'hello world' });

    const calledUrl = mockFetch.mock.calls[0][0];
    expect(calledUrl).toContain('q=hello+world');
  });

  it('should use correct HTTP method (GET)', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ results: [], query: 'test', totalCount: 0, page: 0, size: 20, totalPages: 0 }),
    });

    await search(token, { q: 'test' });

    const options = mockFetch.mock.calls[0][1];
    // GET is the default, so method should not be set or should be undefined
    expect(options.method).toBeUndefined();
  });
});
