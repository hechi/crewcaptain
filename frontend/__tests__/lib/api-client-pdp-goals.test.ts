import {
  createPdpGoal,
  getPdpGoal,
  updatePdpGoal,
  achievePdpGoal,
  pausePdpGoal,
  dropPdpGoal,
  resumePdpGoal,
  deletePdpGoal,
  listPdpGoalsByPerson,
  addPdpUpdate,
  listPdpUpdates,
  deletePdpUpdate,
} from '@/lib/api-client';
import { ApiException } from '@/types/api';

const mockToken = 'test-jwt-token';
const personId = '550e8400-e29b-41d4-a716-446655440000';
const goalId = '660e8400-e29b-41d4-a716-446655440001';
const updateId = '770e8400-e29b-41d4-a716-446655440002';

const mockPdpGoal = {
  id: goalId,
  personId,
  title: 'Improve public speaking',
  description: 'Practice presentations monthly',
  targetDate: '2026-12-31',
  status: 'ACTIVE',
  createdAt: '2026-05-10T10:00:00Z',
  updatedAt: '2026-05-10T10:00:00Z',
};

const mockPdpUpdate = {
  id: updateId,
  goalId,
  textMarkdown: 'Completed first milestone',
  sensitive: false,
  createdAt: '2026-05-10T12:00:00Z',
};

const mockPaginatedGoals = {
  content: [mockPdpGoal],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

const mockPaginatedUpdates = {
  content: [mockPdpUpdate],
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

describe('createPdpGoal', () => {
  it('should send POST request with correct URL and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPdpGoal),
    });

    const data = { title: 'Improve public speaking', description: 'Practice presentations monthly', targetDate: '2026-12-31' };
    await createPdpGoal(mockToken, personId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the created PDP goal', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPdpGoal),
    });

    const result = await createPdpGoal(mockToken, personId, { title: 'Goal' });
    expect(result).toEqual(mockPdpGoal);
  });

  it('should throw ApiException on error', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      json: () => Promise.resolve({ status: 404, error: 'Not Found', message: 'Person not found', timestamp: '2026-05-10T10:00:00Z' }),
    });

    await expect(createPdpGoal(mockToken, personId, { title: 'Goal' })).rejects.toThrow(ApiException);
  });
});

describe('getPdpGoal', () => {
  it('should send GET request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPdpGoal),
    });

    await getPdpGoal(mockToken, personId, goalId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('updatePdpGoal', () => {
  it('should send PUT request with correct URL and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockPdpGoal, title: 'Updated title' }),
    });

    const data = { title: 'Updated title' };
    await updatePdpGoal(mockToken, personId, goalId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('achievePdpGoal', () => {
  it('should send POST request to achieve endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockPdpGoal, status: 'ACHIEVED' }),
    });

    const result = await achievePdpGoal(mockToken, personId, goalId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}/achieve`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.status).toBe('ACHIEVED');
  });
});

describe('pausePdpGoal', () => {
  it('should send POST request to pause endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockPdpGoal, status: 'PAUSED' }),
    });

    const result = await pausePdpGoal(mockToken, personId, goalId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}/pause`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.status).toBe('PAUSED');
  });
});

describe('dropPdpGoal', () => {
  it('should send POST request to drop endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockPdpGoal, status: 'DROPPED' }),
    });

    const result = await dropPdpGoal(mockToken, personId, goalId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}/drop`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.status).toBe('DROPPED');
  });
});

describe('resumePdpGoal', () => {
  it('should send POST request to resume endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockPdpGoal, status: 'ACTIVE' }),
    });

    const result = await resumePdpGoal(mockToken, personId, goalId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}/resume`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.status).toBe('ACTIVE');
  });
});

describe('deletePdpGoal', () => {
  it('should send DELETE request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({}),
    });

    await deletePdpGoal(mockToken, personId, goalId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('listPdpGoalsByPerson', () => {
  it('should send GET request without params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedGoals),
    });

    const result = await listPdpGoalsByPerson(mockToken, personId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.totalElements).toBe(1);
  });

  it('should include status filter in query params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedGoals),
    });

    await listPdpGoalsByPerson(mockToken, personId, { status: 'ACTIVE' });

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals?status=ACTIVE`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should include pagination params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedGoals),
    });

    await listPdpGoalsByPerson(mockToken, personId, { page: 1, size: 10 });

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals?page=1&size=10`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('addPdpUpdate', () => {
  it('should send POST request with correct URL and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPdpUpdate),
    });

    const data = { textMarkdown: 'Completed first milestone', sensitive: false };
    await addPdpUpdate(mockToken, personId, goalId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}/updates`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return the created update', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPdpUpdate),
    });

    const result = await addPdpUpdate(mockToken, personId, goalId, { textMarkdown: 'Note' });
    expect(result).toEqual(mockPdpUpdate);
  });
});

describe('listPdpUpdates', () => {
  it('should send GET request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedUpdates),
    });

    const result = await listPdpUpdates(mockToken, personId, goalId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}/updates`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
    expect(result.totalElements).toBe(1);
  });
});

describe('deletePdpUpdate', () => {
  it('should send DELETE request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({}),
    });

    await deletePdpUpdate(mockToken, personId, goalId, updateId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/persons/${personId}/pdp-goals/${goalId}/updates/${updateId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});
