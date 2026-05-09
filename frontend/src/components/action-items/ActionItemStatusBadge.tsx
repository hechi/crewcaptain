'use client';

import { ActionItemStatus } from '@/types/action-item';

interface ActionItemStatusBadgeProps {
  status: ActionItemStatus;
}

const statusConfig: Record<ActionItemStatus, { label: string; color: string; bgColor: string; glowColor: string }> = {
  OPEN: {
    label: 'Open',
    color: 'var(--color-primary)',
    bgColor: 'var(--color-primary-muted)',
    glowColor: 'rgba(0, 245, 212, 0.2)',
  },
  DONE: {
    label: 'Done',
    color: 'var(--color-morale-green)',
    bgColor: 'rgba(16, 185, 129, 0.1)',
    glowColor: 'rgba(16, 185, 129, 0.2)',
  },
  CANCELED: {
    label: 'Canceled',
    color: 'var(--color-text-muted)',
    bgColor: 'rgba(107, 114, 128, 0.1)',
    glowColor: 'none',
  },
};

/**
 * Displays a status badge for an action item.
 * Uses cyberpunk-lite color scheme with glow effects for active states.
 */
export default function ActionItemStatusBadge({ status }: ActionItemStatusBadgeProps) {
  const config = statusConfig[status];

  return (
    <span
      data-testid="action-item-status-badge"
      style={{
        display: 'inline-block',
        padding: '2px 10px',
        borderRadius: 'var(--radius-full)',
        fontSize: 'var(--text-caption)',
        fontWeight: 'var(--weight-medium)',
        fontFamily: 'var(--font-mono)',
        color: config.color,
        backgroundColor: config.bgColor,
        boxShadow: config.glowColor !== 'none' ? `0 0 6px ${config.glowColor}` : 'none',
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
      }}
      aria-label={`Status: ${config.label}`}
    >
      {config.label}
    </span>
  );
}
