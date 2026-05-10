'use client';

import { PdpGoalStatus } from '@/types/pdp-goal';

interface PdpGoalStatusBadgeProps {
  status: PdpGoalStatus;
}

const statusConfig: Record<PdpGoalStatus, { label: string; color: string; bgColor: string; glowColor: string }> = {
  ACTIVE: {
    label: 'Active',
    color: 'var(--color-primary)',
    bgColor: 'var(--color-primary-muted)',
    glowColor: 'rgba(0, 245, 212, 0.2)',
  },
  ACHIEVED: {
    label: 'Achieved',
    color: 'var(--color-morale-green)',
    bgColor: 'rgba(16, 185, 129, 0.1)',
    glowColor: 'rgba(16, 185, 129, 0.2)',
  },
  PAUSED: {
    label: 'Paused',
    color: 'var(--color-morale-yellow)',
    bgColor: 'rgba(245, 158, 11, 0.1)',
    glowColor: 'rgba(245, 158, 11, 0.2)',
  },
  DROPPED: {
    label: 'Dropped',
    color: 'var(--color-text-muted)',
    bgColor: 'rgba(107, 114, 128, 0.1)',
    glowColor: 'none',
  },
};

/**
 * Displays a status badge for a PDP goal.
 * Uses cyberpunk-lite color scheme with glow effects for active states.
 */
export default function PdpGoalStatusBadge({ status }: PdpGoalStatusBadgeProps) {
  const config = statusConfig[status];

  return (
    <span
      data-testid="pdp-goal-status-badge"
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
