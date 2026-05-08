'use client';

import { MoraleStatus } from '@/types/person';

interface MoraleIndicatorProps {
  moraleStatus: MoraleStatus;
}

const moraleColors: Record<MoraleStatus, string> = {
  GREEN: 'var(--color-morale-green)',
  YELLOW: 'var(--color-morale-yellow)',
  RED: 'var(--color-morale-red)',
  UNKNOWN: 'var(--color-morale-unknown)',
};

const moraleLabels: Record<MoraleStatus, string> = {
  GREEN: 'Green',
  YELLOW: 'Yellow',
  RED: 'Red',
  UNKNOWN: 'Unknown',
};

export default function MoraleIndicator({ moraleStatus }: MoraleIndicatorProps) {
  const color = moraleColors[moraleStatus];
  const label = moraleLabels[moraleStatus];

  return (
    <span
      data-testid="morale-indicator"
      aria-label={`Morale: ${label}`}
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: 'var(--radius-full)',
        backgroundColor: color,
        color: moraleStatus === 'YELLOW' ? '#000' : '#fff',
        fontSize: 'var(--text-caption)',
        fontWeight: 'var(--weight-semibold)',
        lineHeight: '1.5',
      }}
    >
      {label}
    </span>
  );
}
