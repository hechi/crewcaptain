import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import NotificationPanel from '@/components/notifications/NotificationPanel';

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

jest.mock('@/lib/api-client', () => ({
  listNotifications: jest.fn(),
  markNotificationAsRead: jest.fn(),
  markAllNotificationsAsRead: jest.fn(),
}));

import {
  listNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
} from '@/lib/api-client';

const mockListNotifications = listNotifications as jest.MockedFunction<typeof listNotifications>;
const mockMarkAsRead = markNotificationAsRead as jest.MockedFunction<typeof markNotificationAsRead>;
const mockMarkAllRead = markAllNotificationsAsRead as jest.MockedFunction<typeof markAllNotificationsAsRead>;

const mockNotifications = [
  {
    id: 'notif-1',
    type: 'ACTION_ITEM_OVERDUE' as const,
    title: 'Action item overdue',
    message: '"Review PR" for Alice was due on 2026-05-08',
    referenceId: 'ai-1',
    personId: 'person-1',
    isRead: false,
    readAt: null,
    createdAt: '2026-05-10T10:00:00Z',
  },
  {
    id: 'notif-2',
    type: 'STALE_ONE_ON_ONE' as const,
    title: '1:1 overdue',
    message: "You haven't had a 1:1 with Bob in 14 days",
    referenceId: 'person-2',
    personId: 'person-2',
    isRead: true,
    readAt: '2026-05-10T11:00:00Z',
    createdAt: '2026-05-10T09:00:00Z',
  },
];

describe('NotificationPanel', () => {
  const mockOnAllRead = jest.fn();
  const mockOnSingleRead = jest.fn();
  const mockOnClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockListNotifications.mockResolvedValue({
      content: mockNotifications,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });
  });

  it('should render the panel with notifications header', async () => {
    await act(async () => {
      render(
        <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
      );
    });

    expect(screen.getByTestId('notification-panel')).toBeInTheDocument();
    expect(screen.getByText('Notifications')).toBeInTheDocument();
  });

  it('should show loading state initially', () => {
    mockListNotifications.mockReturnValue(new Promise(() => {})); // Never resolves

    render(
      <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
    );

    expect(screen.getByTestId('notification-loading')).toBeInTheDocument();
  });

  it('should show notifications after loading', async () => {
    await act(async () => {
      render(
        <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-item-notif-1')).toBeInTheDocument();
      expect(screen.getByTestId('notification-item-notif-2')).toBeInTheDocument();
    });
  });

  it('should show empty state when no notifications', async () => {
    mockListNotifications.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    await act(async () => {
      render(
        <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-empty')).toHaveTextContent('No notifications yet');
    });
  });

  it('should show error state on fetch failure', async () => {
    mockListNotifications.mockRejectedValue(new Error('Network error'));

    await act(async () => {
      render(
        <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-error')).toHaveTextContent('Failed to load notifications');
    });
  });

  it('should have mark all read button', async () => {
    await act(async () => {
      render(
        <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
      );
    });

    expect(screen.getByTestId('mark-all-read-button')).toBeInTheDocument();
  });

  it('should call markAllNotificationsAsRead when mark all button clicked', async () => {
    mockMarkAllRead.mockResolvedValue({ markedCount: 1 });

    await act(async () => {
      render(
        <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
      );
    });

    await act(async () => {
      screen.getByTestId('mark-all-read-button').click();
    });

    expect(mockMarkAllRead).toHaveBeenCalledWith('test-token');
    expect(mockOnAllRead).toHaveBeenCalled();
  });

  it('should have view all notifications link', async () => {
    await act(async () => {
      render(
        <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
      );
    });

    const viewAllLink = screen.getByTestId('view-all-notifications');
    expect(viewAllLink).toHaveAttribute('href', '/notifications');
  });

  it('should have dialog role for accessibility', async () => {
    await act(async () => {
      render(
        <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
      );
    });

    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('should mark individual notification as read', async () => {
    const readNotification = { ...mockNotifications[0], isRead: true, readAt: '2026-05-10T12:00:00Z' };
    mockMarkAsRead.mockResolvedValue(readNotification);

    await act(async () => {
      render(
        <NotificationPanel token="test-token" onAllRead={mockOnAllRead} onSingleRead={mockOnSingleRead} onClose={mockOnClose} />
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('notification-item-notif-1')).toBeInTheDocument();
    });

    await act(async () => {
      screen.getByTestId('notification-item-notif-1').click();
    });

    expect(mockMarkAsRead).toHaveBeenCalledWith('test-token', 'notif-1');
    expect(mockOnSingleRead).toHaveBeenCalled();
  });
});
