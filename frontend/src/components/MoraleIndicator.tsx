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

const moraleGlows: Record<MoraleStatus, string> = {
  GREEN: '0 0 8px rgba(57, 255, 133, 0.4)',
  YELLOW: '0 0 8px rgba(255, 214, 0, 0.4)',
  RED: '0 0 8px rgba(255, 45, 123, 0.4)',
  UNKNOWN: 'none',
};

const moraleBgColors: Record<MoraleStatus, string> = {
  GREEN: 'rgba(57, 255, 133, 0.15)',
  YELLOW: 'rgba(255, 214, 0, 0.15)',
  RED: 'rgba(255, 45, 123, 0.15)',
  UNKNOWN: 'rgba(74, 85, 104, 0.2)',
};

const moraleLabels: Record<MoraleStatus, string> = {
  GREEN: 'Green',
  YELLOW: 'Yellow',
  RED: 'Red',
  UNKNOWN: 'Unknown',
};

export default function MoraleIndicator({ moraleStatus }: MoraleIndicatorProps) {
  const color = moraleColors[moraleStatus];
  const glow = moraleGlows[moraleStatus];
  const bgColor = moraleBgColors[moraleStatus];
  const label = moraleLabels[moraleStatus];

  return (
    <span
      data-testid="morale-indicator"
      aria-label={`Morale: ${label}`}
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: 'var(--radius-full)',
        backgroundColor: bgColor,
        color: color,
        fontSize: 'var(--text-caption)',
        fontWeight: 'var(--weight-semibold)',
        fontFamily: 'var(--font-mono)',
        lineHeight: '1.5',
        border: `1px solid ${color}`,
        boxShadow: glow,
        letterSpacing: '0.3px',
      }}
    >
      {label}
    </span>
  );
}
