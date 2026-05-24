'use client';

import { StrategyGoalStatus } from '@/types/strategy-goal';

interface StrategyGoalStatusBadgeProps {
  status: StrategyGoalStatus;
}

export default function StrategyGoalStatusBadge({ status }: StrategyGoalStatusBadgeProps) {
  const getStatusStyles = () => {
    switch (status) {
      case 'ACTIVE':
        return {
          backgroundColor: 'var(--color-primary-muted)',
          color: 'var(--color-primary)',
          border: '1px solid var(--color-primary)',
        };
      case 'ACHIEVED':
        return {
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          color: 'var(--color-morale-green)',
          border: '1px solid var(--color-morale-green)',
        };
      case 'DROPPED':
        return {
          backgroundColor: 'var(--color-bg-elevated)',
          color: 'var(--color-text-muted)',
          border: '1px solid var(--color-border)',
        };
      default:
        return {
          backgroundColor: 'var(--color-bg-elevated)',
          color: 'var(--color-text-secondary)',
          border: '1px solid var(--color-border)',
        };
    }
  };

  return (
    <span
      data-testid="strategy-goal-status-badge"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '2px 8px',
        borderRadius: 'var(--radius-full)',
        fontSize: 'var(--text-caption)',
        fontFamily: 'var(--font-mono)',
        fontWeight: 'var(--weight-medium)',
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
        ...getStatusStyles(),
      }}
    >
      {status}
    </span>
  );
}
