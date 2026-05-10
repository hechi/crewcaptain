import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import NotificationBell from '@/components/notifications/NotificationBell';

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

jest.mock('@/lib/api-client', () => ({
  getUnreadNotificationCount: jest.fn(),
  listNotifications: jest.fn(),
  markNotificationAsRead: jest.fn(),
  markAllNotificationsAsRead: jest.fn(),
}));

import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsAsRead,
} from '@/lib/api-client';

const mockGetUnreadCount = getUnreadNotificationCount as jest.MockedFunction<typeof getUnreadNotificationCount>;
const mockListNotifications = listNotifications as jest.MockedFunction<typeof listNotifications>;
const mockMarkAllRead = markAllNotificationsAsRead as jest.MockedFunction<typeof markAllNotificationsAsRead>;

describe('NotificationBell', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    mockGetUnreadCount.mockResolvedValue({ count: 0 });
    mockListNotifications.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should render the bell button', async () => {
    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    expect(screen.getByTestId('notification-bell')).toBeInTheDocument();
  });

  it('should show unread badge when count > 0', async () => {
    mockGetUnreadCount.mockResolvedValue({ count: 3 });

    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-badge')).toHaveTextContent('3');
    });
  });

  it('should not show badge when count is 0', async () => {
    mockGetUnreadCount.mockResolvedValue({ count: 0 });

    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    await waitFor(() => {
      expect(screen.queryByTestId('notification-badge')).not.toBeInTheDocument();
    });
  });

  it('should show 99+ when count exceeds 99', async () => {
    mockGetUnreadCount.mockResolvedValue({ count: 150 });

    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-badge')).toHaveTextContent('99+');
    });
  });

  it('should open notification panel when clicked', async () => {
    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    await act(async () => {
      screen.getByTestId('notification-bell').click();
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-panel')).toBeInTheDocument();
    });
  });

  it('should close notification panel when clicked again', async () => {
    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    await act(async () => {
      screen.getByTestId('notification-bell').click();
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-panel')).toBeInTheDocument();
    });

    await act(async () => {
      screen.getByTestId('notification-bell').click();
    });

    expect(screen.queryByTestId('notification-panel')).not.toBeInTheDocument();
  });

  it('should have accessible aria-label with unread count', async () => {
    mockGetUnreadCount.mockResolvedValue({ count: 5 });

    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-bell')).toHaveAttribute(
        'aria-label',
        'Notifications (5 unread)'
      );
    });
  });

  it('should have accessible aria-label without count when zero', async () => {
    mockGetUnreadCount.mockResolvedValue({ count: 0 });

    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-bell')).toHaveAttribute(
        'aria-label',
        'Notifications'
      );
    });
  });

  it('should fetch unread count on mount', async () => {
    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    expect(mockGetUnreadCount).toHaveBeenCalledWith('test-token');
  });

  it('should handle fetch error gracefully', async () => {
    mockGetUnreadCount.mockRejectedValue(new Error('Network error'));

    await act(async () => {
      render(<NotificationBell token="test-token" />);
    });

    // Should not crash, badge should not appear
    expect(screen.queryByTestId('notification-badge')).not.toBeInTheDocument();
  });
});
