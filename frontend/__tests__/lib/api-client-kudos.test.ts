import {
  createKudos,
  getKudos,
  deleteKudos,
  listKudosByPerson,
  listAllKudos,
} from '@/lib/api-client';
import { ApiException } from '@/types/api';

const mockToken = 'test-jwt-token';
const personId = '550e8400-e29b-41d4-a716-446655440000';
const kudosId = '660e8400-e29b-41d4-a716-446655440001';

const mockKudos = {
  id: kudosId,
  personId,
  date: '2026-05-10',
  text: 'Great job on the presentation!',
  tags: ['impact', 'collaboration'],
  createdAt: '2026-05-10T10:00:00Z',
  updatedAt: '2026-05-10T10:00:00Z',
};

const mockPaginatedKudos = {
  content: [mockKudos],
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

describe('createKudos', () => {
  it('should send POST request with correct URL and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockKudos),
    });

    const data = { text: 'Great job!', date: '2026-05-10', tags: ['impact'] };
    await createKudos(mockToken, personId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/kudos`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return created kudos', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockKudos),
    });

    const result = await createKudos(mockToken, personId, { text: 'Great job!' });
    expect(result).toEqual(mockKudos);
  });

  it('should throw ApiException on error', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      json: () => Promise.resolve({
        status: 404,
        error: 'Not Found',
        message: 'Person not found',
        timestamp: '2026-05-10T10:00:00Z',
      }),
    });

    await expect(createKudos(mockToken, personId, { text: 'Test' }))
      .rejects.toThrow(ApiException);
  });
});

describe('getKudos', () => {
  it('should send GET request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockKudos),
    });

    await getKudos(mockToken, personId, kudosId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/kudos/${kudosId}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return kudos', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockKudos),
    });

    const result = await getKudos(mockToken, personId, kudosId);
    expect(result).toEqual(mockKudos);
  });

  it('should throw ApiException when not found', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      json: () => Promise.resolve({
        status: 404,
        error: 'Not Found',
        message: 'Kudos not found',
        timestamp: '2026-05-10T10:00:00Z',
      }),
    });

    await expect(getKudos(mockToken, personId, kudosId))
      .rejects.toThrow(ApiException);
  });
});

describe('deleteKudos', () => {
  it('should send DELETE request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(undefined),
    });

    await deleteKudos(mockToken, personId, kudosId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/kudos/${kudosId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should throw ApiException when not found', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      json: () => Promise.resolve({
        status: 404,
        error: 'Not Found',
        message: 'Kudos not found',
        timestamp: '2026-05-10T10:00:00Z',
      }),
    });

    await expect(deleteKudos(mockToken, personId, kudosId))
      .rejects.toThrow(ApiException);
  });
});

describe('listKudosByPerson', () => {
  it('should send GET request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedKudos),
    });

    await listKudosByPerson(mockToken, personId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/kudos`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should include pagination params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedKudos),
    });

    await listKudosByPerson(mockToken, personId, { page: 1, size: 10 });

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/persons/${personId}/kudos?page=1&size=10`,
      expect.any(Object)
    );
  });

  it('should return paginated kudos', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedKudos),
    });

    const result = await listKudosByPerson(mockToken, personId);
    expect(result).toEqual(mockPaginatedKudos);
  });
});

describe('listAllKudos', () => {
  it('should send GET request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedKudos),
    });

    await listAllKudos(mockToken);

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/kudos', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should include pagination params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedKudos),
    });

    await listAllKudos(mockToken, { page: 2, size: 5 });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/kudos?page=2&size=5',
      expect.any(Object)
    );
  });

  it('should return paginated kudos', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedKudos),
    });

    const result = await listAllKudos(mockToken);
    expect(result).toEqual(mockPaginatedKudos);
  });
});
