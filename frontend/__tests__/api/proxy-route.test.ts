/**
 * @jest-environment node
 */
import { NextRequest } from 'next/server';

// Mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('API Proxy Route', () => {
  beforeEach(() => {
    jest.resetModules();
    mockFetch.mockReset();
    process.env.API_BASE_URL = 'http://api:8080';
  });

  afterEach(() => {
    delete process.env.API_BASE_URL;
  });

  async function importRoute() {
    return await import('@/app/api/v1/[...path]/route');
  }

  function createRequest(method: string, url: string, options?: { body?: string; headers?: Record<string, string> }) {
    const req = new NextRequest(new URL(url, 'http://localhost:3000'), {
      method,
      body: options?.body,
      headers: options?.headers,
    });
    return req;
  }

  it('should proxy GET requests to the backend API', async () => {
    const { GET } = await importRoute();
    mockFetch.mockResolvedValueOnce(new Response(JSON.stringify({ count: 5 }), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    }));

    const request = createRequest('GET', '/api/v1/notifications/unread-count');
    const response = await GET(request, { params: Promise.resolve({ path: ['notifications', 'unread-count'] }) });

    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body).toEqual({ count: 5 });
    expect(mockFetch).toHaveBeenCalledWith(
      'http://api:8080/api/v1/notifications/unread-count',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('should forward query parameters', async () => {
    const { GET } = await importRoute();
    mockFetch.mockResolvedValueOnce(new Response(JSON.stringify([]), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    }));

    const request = createRequest('GET', '/api/v1/persons?page=0&size=20&morale=GREEN');
    const response = await GET(request, { params: Promise.resolve({ path: ['persons'] }) });

    expect(response.status).toBe(200);
    const calledUrl = mockFetch.mock.calls[0][0];
    expect(calledUrl).toContain('page=0');
    expect(calledUrl).toContain('size=20');
    expect(calledUrl).toContain('morale=GREEN');
  });

  it('should proxy POST requests with body', async () => {
    const { POST } = await importRoute();
    const requestBody = JSON.stringify({ name: 'Test Person', roleTitle: 'Engineer' });
    mockFetch.mockResolvedValueOnce(new Response(JSON.stringify({ id: '123', name: 'Test Person' }), {
      status: 201,
      headers: { 'content-type': 'application/json' },
    }));

    const request = createRequest('POST', '/api/v1/persons', {
      body: requestBody,
      headers: { 'content-type': 'application/json' },
    });
    const response = await POST(request, { params: Promise.resolve({ path: ['persons'] }) });

    expect(response.status).toBe(201);
    expect(mockFetch).toHaveBeenCalledWith(
      'http://api:8080/api/v1/persons',
      expect.objectContaining({ method: 'POST', body: requestBody })
    );
  });

  it('should proxy DELETE requests', async () => {
    const { DELETE } = await importRoute();
    mockFetch.mockResolvedValueOnce(new Response(null, { status: 204 }));

    const request = createRequest('DELETE', '/api/v1/persons/abc-123');
    const response = await DELETE(request, { params: Promise.resolve({ path: ['persons', 'abc-123'] }) });

    expect(response.status).toBe(204);
    expect(mockFetch).toHaveBeenCalledWith(
      'http://api:8080/api/v1/persons/abc-123',
      expect.objectContaining({ method: 'DELETE' })
    );
  });

  it('should forward authorization header', async () => {
    const { GET } = await importRoute();
    mockFetch.mockResolvedValueOnce(new Response(JSON.stringify({}), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    }));

    const request = createRequest('GET', '/api/v1/dashboard', {
      headers: { authorization: 'Bearer test-token-123' },
    });
    await GET(request, { params: Promise.resolve({ path: ['dashboard'] }) });

    const calledHeaders = mockFetch.mock.calls[0][1].headers;
    expect(calledHeaders.get('authorization')).toBe('Bearer test-token-123');
  });

  it('should return 502 when backend is unreachable', async () => {
    const { GET } = await importRoute();
    mockFetch.mockRejectedValueOnce(new Error('ECONNREFUSED'));

    const request = createRequest('GET', '/api/v1/notifications/unread-count');
    const response = await GET(request, { params: Promise.resolve({ path: ['notifications', 'unread-count'] }) });

    expect(response.status).toBe(502);
    const body = await response.json();
    expect(body.error).toBe('Bad Gateway');
  });

  it('should use API_BASE_URL from environment', async () => {
    process.env.API_BASE_URL = 'https://api.crewcaptain.de';
    const { GET } = await importRoute();
    mockFetch.mockResolvedValueOnce(new Response(JSON.stringify({}), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    }));

    const request = createRequest('GET', '/api/v1/settings');
    await GET(request, { params: Promise.resolve({ path: ['settings'] }) });

    expect(mockFetch.mock.calls[0][0]).toBe('https://api.crewcaptain.de/api/v1/settings');
  });

  it('should fall back to http://api:8080 when API_BASE_URL is not set', async () => {
    delete process.env.API_BASE_URL;
    const { GET } = await importRoute();
    mockFetch.mockResolvedValueOnce(new Response(JSON.stringify({}), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    }));

    const request = createRequest('GET', '/api/v1/settings');
    await GET(request, { params: Promise.resolve({ path: ['settings'] }) });

    expect(mockFetch.mock.calls[0][0]).toBe('http://api:8080/api/v1/settings');
  });
});
