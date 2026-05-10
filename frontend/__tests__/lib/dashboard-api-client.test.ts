import { getDashboard } from '@/lib/api-client';

const mockToken = 'test-jwt-token';

const mockDashboardResponse = {
  overdueActionItems: [
    {
      id: '550e8400-e29b-41d4-a716-446655440001',
      personId: '550e8400-e29b-41d4-a716-446655440002',
      personName: 'Alice Smith',
      title: 'Review PR',
      dueDate: '2026-05-07',
      ownerType: 'PERSON' as const,
    },
  ],
  dueSoonActionItems: [
    {
      id: '550e8400-e29b-41d4-a716-446655440003',
      personId: '550e8400-e29b-41d4-a716-446655440004',
      personName: 'Bob Jones',
      title: 'Submit report',
      dueDate: '2026-05-12',
      ownerType: 'MANAGER' as const,
    },
  ],
  staleOneOnOnes: [
    {
      personId: '550e8400-e29b-41d4-a716-446655440005',
      personName: 'Charlie Brown',
      cadenceType: 'WEEKLY' as const,
      customIntervalDays: null,
      lastMeetingDate: '2026-04-25T10:00:00Z',
      daysSinceLastMeeting: 15,
      expectedIntervalDays: 7,
    },
  ],
  upcomingAnniversaries: [
    {
      personId: '550e8400-e29b-41d4-a716-446655440006',
      personName: 'Diana Prince',
      startDate: '2023-05-15',
      anniversaryDate: '2026-05-15',
      yearsCompleted: 3,
      daysUntil: 5,
    },
  ],
};

let mockFetch: jest.Mock;

beforeEach(() => {
  mockFetch = jest.fn();
  global.fetch = mockFetch;
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('getDashboard', () => {
  it('should send GET request with correct URL and auth header', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockDashboardResponse),
    });

    const result = await getDashboard(mockToken);

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/dashboard',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': `Bearer ${mockToken}`,
          'Content-Type': 'application/json',
        }),
      })
    );
    expect(result).toEqual(mockDashboardResponse);
  });

  it('should include dueSoonDays parameter when provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockDashboardResponse),
    });

    await getDashboard(mockToken, { dueSoonDays: 7 });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/dashboard?dueSoonDays=7',
      expect.any(Object)
    );
  });

  it('should include anniversaryLookaheadDays parameter when provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockDashboardResponse),
    });

    await getDashboard(mockToken, { anniversaryLookaheadDays: 60 });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/dashboard?anniversaryLookaheadDays=60',
      expect.any(Object)
    );
  });

  it('should include both parameters when provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockDashboardResponse),
    });

    await getDashboard(mockToken, { dueSoonDays: 5, anniversaryLookaheadDays: 14 });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/dashboard?dueSoonDays=5&anniversaryLookaheadDays=14',
      expect.any(Object)
    );
  });

  it('should throw ApiException on error response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.resolve({
        status: 500,
        error: 'Internal Server Error',
        message: 'Something went wrong',
        timestamp: '2026-05-10T12:00:00Z',
      }),
    });

    await expect(getDashboard(mockToken)).rejects.toThrow();
  });

  it('should return empty arrays when dashboard has no data', async () => {
    const emptyDashboard = {
      overdueActionItems: [],
      dueSoonActionItems: [],
      staleOneOnOnes: [],
      upcomingAnniversaries: [],
    };

    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(emptyDashboard),
    });

    const result = await getDashboard(mockToken);

    expect(result.overdueActionItems).toEqual([]);
    expect(result.dueSoonActionItems).toEqual([]);
    expect(result.staleOneOnOnes).toEqual([]);
    expect(result.upcomingAnniversaries).toEqual([]);
  });

  it('should return overdue action items with correct structure', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockDashboardResponse),
    });

    const result = await getDashboard(mockToken);

    expect(result.overdueActionItems[0]).toEqual({
      id: '550e8400-e29b-41d4-a716-446655440001',
      personId: '550e8400-e29b-41d4-a716-446655440002',
      personName: 'Alice Smith',
      title: 'Review PR',
      dueDate: '2026-05-07',
      ownerType: 'PERSON',
    });
  });

  it('should return stale 1:1 reminders with correct structure', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockDashboardResponse),
    });

    const result = await getDashboard(mockToken);

    expect(result.staleOneOnOnes[0]).toEqual({
      personId: '550e8400-e29b-41d4-a716-446655440005',
      personName: 'Charlie Brown',
      cadenceType: 'WEEKLY',
      customIntervalDays: null,
      lastMeetingDate: '2026-04-25T10:00:00Z',
      daysSinceLastMeeting: 15,
      expectedIntervalDays: 7,
    });
  });

  it('should return upcoming anniversaries with correct structure', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockDashboardResponse),
    });

    const result = await getDashboard(mockToken);

    expect(result.upcomingAnniversaries[0]).toEqual({
      personId: '550e8400-e29b-41d4-a716-446655440006',
      personName: 'Diana Prince',
      startDate: '2023-05-15',
      anniversaryDate: '2026-05-15',
      yearsCompleted: 3,
      daysUntil: 5,
    });
  });
});
