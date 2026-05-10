'use client';

import { useState, useEffect } from 'react';
import { listNotifications, markNotificationAsRead, markAllNotificationsAsRead } from '@/lib/api-client';
import { Notification } from '@/types/notification';
import NotificationItem from './NotificationItem';

interface NotificationPanelProps {
  token: string;
  onAllRead: () => void;
  onSingleRead: () => void;
  onClose: () => void;
}

/**
 * Dropdown panel showing recent notifications with mark-as-read actions.
 */
export default function NotificationPanel({ token, onAllRead, onSingleRead, onClose }: NotificationPanelProps) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchNotifications() {
      try {
        setLoading(true);
        const data = await listNotifications(token, { page: 0, size: 20 });
        setNotifications(data.content);
      } catch {
        setError('Failed to load notifications');
      } finally {
        setLoading(false);
      }
    }

    fetchNotifications();
  }, [token]);

  const handleMarkAsRead = async (notificationId: string) => {
    try {
      const updated = await markNotificationAsRead(token, notificationId);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notificationId ? { ...n, isRead: true, readAt: updated.readAt } : n))
      );
      onSingleRead();
    } catch {
      // Silently fail
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await markAllNotificationsAsRead(token);
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, readAt: new Date().toISOString() }))
      );
      onAllRead();
    } catch {
      // Silently fail
    }
  };

  return (
    <div
      data-testid="notification-panel"
      role="dialog"
      aria-label="Notifications"
      style={{
        position: 'absolute',
        top: '100%',
        right: 0,
        marginTop: '8px',
        width: '380px',
        maxHeight: '480px',
        overflowY: 'auto',
        backgroundColor: 'var(--color-bg-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium, 8px)',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3), 0 0 8px rgba(0, 240, 255, 0.05)',
        zIndex: 1000,
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '12px 16px',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        <h3
          style={{
            margin: 0,
            fontSize: '14px',
            fontWeight: 600,
            color: 'var(--color-text-primary)',
          }}
        >
          Notifications
        </h3>
        <button
          type="button"
          onClick={handleMarkAllAsRead}
          data-testid="mark-all-read-button"
          style={{
            padding: '4px 8px',
            fontSize: '12px',
            color: 'var(--color-primary)',
            backgroundColor: 'transparent',
            border: 'none',
            cursor: 'pointer',
            fontWeight: 500,
          }}
        >
          Mark all read
        </button>
      </div>

      <div data-testid="notification-list" style={{ padding: '4px 0' }}>
        {loading && (
          <p
            data-testid="notification-loading"
            style={{ padding: '24px 16px', textAlign: 'center', color: 'var(--color-text-secondary)', fontSize: '13px' }}
          >
            Loading...
          </p>
        )}

        {error && (
          <p
            data-testid="notification-error"
            style={{ padding: '24px 16px', textAlign: 'center', color: 'var(--color-danger, #ef4444)', fontSize: '13px' }}
          >
            {error}
          </p>
        )}

        {!loading && !error && notifications.length === 0 && (
          <p
            data-testid="notification-empty"
            style={{ padding: '24px 16px', textAlign: 'center', color: 'var(--color-text-secondary)', fontSize: '13px' }}
          >
            No notifications yet
          </p>
        )}

        {!loading && !error && notifications.map((notification) => (
          <NotificationItem
            key={notification.id}
            notification={notification}
            onMarkAsRead={handleMarkAsRead}
          />
        ))}
      </div>

      <div
        style={{
          padding: '8px 16px',
          borderTop: '1px solid var(--color-border)',
          textAlign: 'center',
        }}
      >
        <a
          href="/notifications"
          data-testid="view-all-notifications"
          style={{
            fontSize: '12px',
            color: 'var(--color-primary)',
            textDecoration: 'none',
            fontWeight: 500,
          }}
        >
          View all notifications
        </a>
      </div>
    </div>
  );
}
