'use client';

import Link from 'next/link';
import { Notification, NotificationType } from '@/types/notification';

interface NotificationItemProps {
  notification: Notification;
  onMarkAsRead: (id: string) => void;
}

function getNotificationIcon(type: NotificationType): string {
  switch (type) {
    case 'ACTION_ITEM_OVERDUE':
      return '⚠️';
    case 'ACTION_ITEM_DUE_SOON':
      return '⏰';
    case 'STALE_ONE_ON_ONE':
      return '📅';
    case 'UPCOMING_ANNIVERSARY':
      return '🎉';
    default:
      return '🔔';
  }
}

function getNotificationLink(notification: Notification): string | null {
  if (!notification.personId) return null;

  switch (notification.type) {
    case 'ACTION_ITEM_OVERDUE':
    case 'ACTION_ITEM_DUE_SOON':
      return `/people/${notification.personId}?tab=action-items`;
    case 'STALE_ONE_ON_ONE':
      return `/people/${notification.personId}?tab=one-on-ones`;
    case 'UPCOMING_ANNIVERSARY':
      return `/people/${notification.personId}`;
    default:
      return null;
  }
}

function formatTimeAgo(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMinutes = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMinutes / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMinutes < 1) return 'just now';
  if (diffMinutes < 60) return `${diffMinutes}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString();
}

/**
 * Single notification item in the notification panel.
 */
export default function NotificationItem({ notification, onMarkAsRead }: NotificationItemProps) {
  const icon = getNotificationIcon(notification.type);
  const link = getNotificationLink(notification);

  const content = (
    <div
      data-testid={`notification-item-${notification.id}`}
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: '10px',
        padding: '10px 16px',
        backgroundColor: notification.isRead ? 'transparent' : 'rgba(0, 240, 255, 0.03)',
        borderLeft: notification.isRead ? '3px solid transparent' : '3px solid var(--color-primary)',
        cursor: 'pointer',
        transition: 'background-color 0.2s',
      }}
      onClick={() => {
        if (!notification.isRead) {
          onMarkAsRead(notification.id);
        }
      }}
      role="button"
      tabIndex={0}
      aria-label={`${notification.title}: ${notification.message}${notification.isRead ? '' : ' (unread)'}`}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          if (!notification.isRead) {
            onMarkAsRead(notification.id);
          }
        }
      }}
    >
      <span style={{ fontSize: '16px', flexShrink: 0, marginTop: '2px' }} aria-hidden="true">
        {icon}
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <p
          data-testid={`notification-title-${notification.id}`}
          style={{
            margin: 0,
            fontSize: '13px',
            fontWeight: notification.isRead ? 400 : 600,
            color: 'var(--color-text-primary)',
            lineHeight: 1.3,
          }}
        >
          {notification.title}
        </p>
        <p
          data-testid={`notification-message-${notification.id}`}
          style={{
            margin: '2px 0 0',
            fontSize: '12px',
            color: 'var(--color-text-secondary)',
            lineHeight: 1.3,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {notification.message}
        </p>
        <span
          data-testid={`notification-time-${notification.id}`}
          style={{
            fontSize: '11px',
            color: 'var(--color-text-muted, var(--color-text-secondary))',
            marginTop: '4px',
            display: 'inline-block',
          }}
        >
          {formatTimeAgo(notification.createdAt)}
        </span>
      </div>
      {!notification.isRead && (
        <span
          data-testid={`notification-unread-dot-${notification.id}`}
          style={{
            width: '8px',
            height: '8px',
            borderRadius: '50%',
            backgroundColor: 'var(--color-primary)',
            flexShrink: 0,
            marginTop: '6px',
          }}
          aria-hidden="true"
        />
      )}
    </div>
  );

  if (link) {
    return (
      <Link href={link} style={{ textDecoration: 'none', color: 'inherit' }}>
        {content}
      </Link>
    );
  }

  return content;
}
