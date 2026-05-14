'use client';

import { useState, useEffect, useCallback } from 'react';
import { listNotifications, markNotificationAsRead, markAllNotificationsAsRead } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import { Notification } from '@/types/notification';
import NotificationItem from '@/components/notifications/NotificationItem';
import Pagination from '@/components/Pagination';
import Navigation from '@/components/Navigation';

export default function NotificationsPage() {
  const { getToken } = useStableToken();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);

  const fetchNotifications = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    try {
      setLoading(true);
      const data = await listNotifications(token, { unreadOnly, page, size: 20 });
      setNotifications(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch {
      setError('Failed to load notifications');
    } finally {
      setLoading(false);
    }
  }, [getToken, page, unreadOnly]);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  const handleMarkAsRead = async (notificationId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await markNotificationAsRead(token, notificationId);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notificationId ? { ...n, isRead: true, readAt: new Date().toISOString() } : n))
      );
    } catch {
      // Silently fail
    }
  };

  const handleMarkAllAsRead = async () => {
    const token = getToken();
    if (!token) return;
    try {
      await markAllNotificationsAsRead(token);
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, readAt: new Date().toISOString() }))
      );
    } catch {
      // Silently fail
    }
  };

  return (
    <>
      <Navigation />
      <main
        data-testid="notifications-page"
        style={{
          maxWidth: '720px',
          margin: '0 auto',
          padding: '32px 24px',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '24px',
          }}
        >
          <h1
            style={{
              margin: 0,
              fontSize: '24px',
              fontFamily: 'var(--font-heading)',
              fontWeight: 700,
              color: 'var(--color-text-primary)',
            }}
          >
            Notifications
          </h1>
          <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
            <label
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: '13px',
                color: 'var(--color-text-secondary)',
                cursor: 'pointer',
              }}
            >
              <input
                type="checkbox"
                checked={unreadOnly}
                onChange={(e) => {
                  setUnreadOnly(e.target.checked);
                  setPage(0);
                }}
                data-testid="unread-only-filter"
              />
              Unread only
            </label>
            <button
              type="button"
              onClick={handleMarkAllAsRead}
              data-testid="mark-all-read-page-button"
              style={{
                padding: '6px 12px',
                fontSize: '13px',
                color: 'var(--color-primary)',
                backgroundColor: 'var(--color-primary-muted)',
                border: '1px solid var(--color-border-glow)',
                borderRadius: 'var(--radius-small)',
                cursor: 'pointer',
                fontWeight: 500,
              }}
            >
              Mark all as read
            </button>
          </div>
        </div>

        {loading && (
          <p
            data-testid="notifications-loading"
            style={{ textAlign: 'center', color: 'var(--color-text-secondary)', padding: '48px 0' }}
          >
            Loading notifications...
          </p>
        )}

        {error && (
          <p
            data-testid="notifications-error"
            style={{ textAlign: 'center', color: 'var(--color-danger, #ef4444)', padding: '48px 0' }}
          >
            {error}
          </p>
        )}

        {!loading && !error && notifications.length === 0 && (
          <div
            data-testid="notifications-empty"
            style={{
              textAlign: 'center',
              padding: '64px 24px',
              color: 'var(--color-text-secondary)',
            }}
          >
            <p style={{ fontSize: '32px', marginBottom: '12px' }}>🔔</p>
            <p style={{ fontSize: '15px', fontWeight: 500 }}>
              {unreadOnly ? 'No unread notifications' : 'No notifications yet'}
            </p>
            <p style={{ fontSize: '13px', marginTop: '8px' }}>
              Notifications will appear here when action items are overdue, 1:1s are stale, or anniversaries are coming up.
            </p>
          </div>
        )}

        {!loading && !error && notifications.length > 0 && (
          <>
            <div
              style={{
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium, 8px)',
                overflow: 'hidden',
              }}
            >
              {notifications.map((notification) => (
                <NotificationItem
                  key={notification.id}
                  notification={notification}
                  onMarkAsRead={handleMarkAsRead}
                />
              ))}
            </div>

            {totalPages > 1 && (
              <div style={{ marginTop: '24px' }}>
                <Pagination
                  currentPage={page}
                  totalPages={totalPages}
                  onPageChange={setPage}
                />
              </div>
            )}

            <p
              style={{
                marginTop: '12px',
                fontSize: '12px',
                color: 'var(--color-text-secondary)',
                textAlign: 'center',
              }}
            >
              {totalElements} notification{totalElements !== 1 ? 's' : ''} total
            </p>
          </>
        )}
      </main>
    </>
  );
}
