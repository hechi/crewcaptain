import {
  listNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  markAllNotificationsAsRead,
} from '@/lib/api-client';

const mockToken = 'test-jwt-token';

const mockNotification = {
  id: '550e8400-e29b-41d4-a716-446655440001',
  type: 'ACTION_ITEM_OVERDUE' as const,
  title: 'Action item overdue',
  message: '"Review PR" for Alice Smith was due on 2026-05-08',
  referenceId: '550e8400-e29b-41d4-a716-446655440010',
  personId: '550e8400-e29b-41d4-a716-446655440002',
  isRead: false,
  readAt: null,
  createdAt: '2026-05-10T10:00:00Z',
};

const mockPaginatedResponse = {
  content: [mockNotification],
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

describe('listNotifications', () => {
  it('should send GET request with correct URL and auth header', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    const result = await listNotifications(mockToken);

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/notifications',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: `Bearer ${mockToken}`,
          'Content-Type': 'application/json',
        }),
      })
    );
    expect(result).toEqual(mockPaginatedResponse);
  });

  it('should include unreadOnly parameter when true', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await listNotifications(mockToken, { unreadOnly: true });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/notifications?unreadOnly=true',
      expect.any(Object)
    );
  });

  it('should include pagination parameters', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await listNotifications(mockToken, { page: 2, size: 10 });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/notifications?page=2&size=10',
      expect.any(Object)
    );
  });

  it('should include all parameters when provided', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedResponse),
    });

    await listNotifications(mockToken, { unreadOnly: true, page: 1, size: 5 });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/notifications?unreadOnly=true&page=1&size=5',
      expect.any(Object)
    );
  });

  it('should throw on error response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: () => Promise.resolve({
        status: 401,
        error: 'Unauthorized',
        message: 'Authentication failed',
        timestamp: '2026-05-10T12:00:00Z',
      }),
    });

    await expect(listNotifications(mockToken)).rejects.toThrow();
  });

  it('should return empty content when no notifications', async () => {
    const emptyResponse = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(emptyResponse),
    });

    const result = await listNotifications(mockToken);

    expect(result.content).toEqual([]);
    expect(result.totalElements).toBe(0);
  });
});

describe('getUnreadNotificationCount', () => {
  it('should send GET request to unread-count endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ count: 5 }),
    });

    const result = await getUnreadNotificationCount(mockToken);

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/notifications/unread-count',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: `Bearer ${mockToken}`,
        }),
      })
    );
    expect(result.count).toBe(5);
  });

  it('should return zero count', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ count: 0 }),
    });

    const result = await getUnreadNotificationCount(mockToken);

    expect(result.count).toBe(0);
  });

  it('should throw on error response', async () => {
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

    await expect(getUnreadNotificationCount(mockToken)).rejects.toThrow();
  });
});

describe('markNotificationAsRead', () => {
  it('should send POST request to mark notification as read', async () => {
    const readNotification = { ...mockNotification, isRead: true, readAt: '2026-05-10T12:00:00Z' };
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(readNotification),
    });

    const result = await markNotificationAsRead(mockToken, mockNotification.id);

    expect(mockFetch).toHaveBeenCalledWith(
      `/api/v1/notifications/${mockNotification.id}/read`,
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: `Bearer ${mockToken}`,
        }),
      })
    );
    expect(result.isRead).toBe(true);
    expect(result.readAt).toBe('2026-05-10T12:00:00Z');
  });

  it('should throw on 404 response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      json: () => Promise.resolve({
        status: 404,
        error: 'Not Found',
        message: 'Notification not found',
        timestamp: '2026-05-10T12:00:00Z',
      }),
    });

    await expect(markNotificationAsRead(mockToken, 'non-existent-id')).rejects.toThrow();
  });
});

describe('markAllNotificationsAsRead', () => {
  it('should send POST request to mark all as read', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ markedCount: 3 }),
    });

    const result = await markAllNotificationsAsRead(mockToken);

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/notifications/read-all',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: `Bearer ${mockToken}`,
        }),
      })
    );
    expect(result.markedCount).toBe(3);
  });

  it('should return zero when no unread notifications', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ markedCount: 0 }),
    });

    const result = await markAllNotificationsAsRead(mockToken);

    expect(result.markedCount).toBe(0);
  });

  it('should throw on error response', async () => {
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

    await expect(markAllNotificationsAsRead(mockToken)).rejects.toThrow();
  });
});
