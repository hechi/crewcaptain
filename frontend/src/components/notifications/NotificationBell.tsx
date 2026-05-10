'use client';

import { useState, useEffect, useRef } from 'react';
import { getUnreadNotificationCount } from '@/lib/api-client';
import NotificationPanel from './NotificationPanel';

interface NotificationBellProps {
  token: string;
}

/**
 * Bell icon with unread badge that opens the notification panel.
 * Polls for unread count every 60 seconds.
 */
export default function NotificationBell({ token }: NotificationBellProps) {
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let mounted = true;

    async function fetchCount() {
      try {
        const data = await getUnreadNotificationCount(token);
        if (mounted) setUnreadCount(data.count);
      } catch {
        // Silently fail — notification count is non-critical
      }
    }

    fetchCount();
    const interval = setInterval(fetchCount, 60000);

    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, [token]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const handleNotificationsRead = () => {
    setUnreadCount(0);
  };

  const handleSingleRead = () => {
    setUnreadCount((prev) => Math.max(0, prev - 1));
  };

  return (
    <div ref={panelRef} style={{ position: 'relative' }} data-testid="notification-bell-container">
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        data-testid="notification-bell"
        aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
        style={{
          position: 'relative',
          padding: '6px 10px',
          fontSize: '18px',
          backgroundColor: 'transparent',
          border: 'none',
          color: 'var(--color-text-secondary)',
          cursor: 'pointer',
          borderRadius: 'var(--radius-small)',
          transition: 'color 0.2s',
        }}
      >
        🔔
        {unreadCount > 0 && (
          <span
            data-testid="notification-badge"
            style={{
              position: 'absolute',
              top: '2px',
              right: '4px',
              minWidth: '16px',
              height: '16px',
              padding: '0 4px',
              fontSize: '10px',
              fontWeight: 700,
              lineHeight: '16px',
              textAlign: 'center',
              color: '#fff',
              backgroundColor: 'var(--color-danger, #ef4444)',
              borderRadius: '8px',
            }}
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <NotificationPanel
          token={token}
          onAllRead={handleNotificationsRead}
          onSingleRead={handleSingleRead}
          onClose={() => setIsOpen(false)}
        />
      )}
    </div>
  );
}
