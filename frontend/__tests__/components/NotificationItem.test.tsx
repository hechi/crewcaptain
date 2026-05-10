import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import NotificationItem from '@/components/notifications/NotificationItem';
import { Notification } from '@/types/notification';

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

const baseNotification: Notification = {
  id: 'notif-1',
  type: 'ACTION_ITEM_OVERDUE',
  title: 'Action item overdue',
  message: '"Review PR" for Alice Smith was due on 2026-05-08',
  referenceId: 'action-item-1',
  personId: 'person-1',
  isRead: false,
  readAt: null,
  createdAt: new Date(Date.now() - 3600000).toISOString(), // 1 hour ago
};

describe('NotificationItem', () => {
  const mockOnMarkAsRead = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render notification title', () => {
    render(<NotificationItem notification={baseNotification} onMarkAsRead={mockOnMarkAsRead} />);

    expect(screen.getByTestId(`notification-title-${baseNotification.id}`)).toHaveTextContent('Action item overdue');
  });

  it('should render notification message', () => {
    render(<NotificationItem notification={baseNotification} onMarkAsRead={mockOnMarkAsRead} />);

    expect(screen.getByTestId(`notification-message-${baseNotification.id}`)).toHaveTextContent(
      '"Review PR" for Alice Smith was due on 2026-05-08'
    );
  });

  it('should render time ago', () => {
    render(<NotificationItem notification={baseNotification} onMarkAsRead={mockOnMarkAsRead} />);

    expect(screen.getByTestId(`notification-time-${baseNotification.id}`)).toBeInTheDocument();
  });

  it('should show unread dot for unread notifications', () => {
    render(<NotificationItem notification={baseNotification} onMarkAsRead={mockOnMarkAsRead} />);

    expect(screen.getByTestId(`notification-unread-dot-${baseNotification.id}`)).toBeInTheDocument();
  });

  it('should not show unread dot for read notifications', () => {
    const readNotification = { ...baseNotification, isRead: true, readAt: '2026-05-10T12:00:00Z' };
    render(<NotificationItem notification={readNotification} onMarkAsRead={mockOnMarkAsRead} />);

    expect(screen.queryByTestId(`notification-unread-dot-${baseNotification.id}`)).not.toBeInTheDocument();
  });

  it('should call onMarkAsRead when unread notification is clicked', () => {
    render(<NotificationItem notification={baseNotification} onMarkAsRead={mockOnMarkAsRead} />);

    fireEvent.click(screen.getByTestId(`notification-item-${baseNotification.id}`));

    expect(mockOnMarkAsRead).toHaveBeenCalledWith(baseNotification.id);
  });

  it('should not call onMarkAsRead when read notification is clicked', () => {
    const readNotification = { ...baseNotification, isRead: true, readAt: '2026-05-10T12:00:00Z' };
    render(<NotificationItem notification={readNotification} onMarkAsRead={mockOnMarkAsRead} />);

    fireEvent.click(screen.getByTestId(`notification-item-${baseNotification.id}`));

    expect(mockOnMarkAsRead).not.toHaveBeenCalled();
  });

  it('should display overdue icon for ACTION_ITEM_OVERDUE', () => {
    render(<NotificationItem notification={baseNotification} onMarkAsRead={mockOnMarkAsRead} />);

    expect(screen.getByText('⚠️')).toBeInTheDocument();
  });

  it('should display clock icon for ACTION_ITEM_DUE_SOON', () => {
    const dueSoonNotification = { ...baseNotification, type: 'ACTION_ITEM_DUE_SOON' as const };
    render(<NotificationItem notification={dueSoonNotification} onMarkAsRead={mockOnMarkAsRead} />);

    expect(screen.getByText('⏰')).toBeInTheDocument();
  });

  it('should display calendar icon for STALE_ONE_ON_ONE', () => {
    const staleNotification = { ...baseNotification, type: 'STALE_ONE_ON_ONE' as const };
    render(<NotificationItem notification={staleNotification} onMarkAsRead={mockOnMarkAsRead} />);

    expect(screen.getByText('📅')).toBeInTheDocument();
  });

  it('should display celebration icon for UPCOMING_ANNIVERSARY', () => {
    const anniversaryNotification = { ...baseNotification, type: 'UPCOMING_ANNIVERSARY' as const };
    render(<NotificationItem notification={anniversaryNotification} onMarkAsRead={mockOnMarkAsRead} />);

    expect(screen.getByText('🎉')).toBeInTheDocument();
  });

  it('should link to action items tab for overdue action items', () => {
    render(<NotificationItem notification={baseNotification} onMarkAsRead={mockOnMarkAsRead} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/people/person-1?tab=action-items');
  });

  it('should link to 1:1 tab for stale one-on-one', () => {
    const staleNotification = { ...baseNotification, type: 'STALE_ONE_ON_ONE' as const };
    render(<NotificationItem notification={staleNotification} onMarkAsRead={mockOnMarkAsRead} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/people/person-1?tab=one-on-ones');
  });

  it('should link to person page for upcoming anniversary', () => {
    const anniversaryNotification = { ...baseNotification, type: 'UPCOMING_ANNIVERSARY' as const };
    render(<NotificationItem notification={anniversaryNotification} onMarkAsRead={mockOnMarkAsRead} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/people/person-1');
  });

  it('should have accessible aria-label', () => {
    render(<NotificationItem notification={baseNotification} onMarkAsRead={mockOnMarkAsRead} />);

    const item = screen.getByTestId(`notification-item-${baseNotification.id}`);
    expect(item).toHaveAttribute('aria-label', expect.stringContaining('Action item overdue'));
    expect(item).toHaveAttribute('aria-label', expect.stringContaining('(unread)'));
  });

  it('should handle keyboard interaction', () => {
    render(<NotificationItem notification={baseNotification} onMarkAsRead={mockOnMarkAsRead} />);

    const item = screen.getByTestId(`notification-item-${baseNotification.id}`);
    fireEvent.keyDown(item, { key: 'Enter' });

    expect(mockOnMarkAsRead).toHaveBeenCalledWith(baseNotification.id);
  });
});
