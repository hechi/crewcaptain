'use client';

interface StreakCounterProps {
  /** Current streak count (weeks) */
  currentStreak: number;
  /** Longest streak ever achieved */
  longestStreak: number;
  /** Total 1:1s held */
  totalOneOnOnesHeld: number;
}

/**
 * Displays the current 1:1 streak in a monospace, cockpit-style readout.
 * Shows current streak prominently with longest streak and total as secondary stats.
 */
export default function StreakCounter({
  currentStreak,
  longestStreak,
  totalOneOnOnesHeld,
}: StreakCounterProps) {
  const isActive = currentStreak > 0;

  return (
    <div
      data-testid="streak-counter"
      style={{ width: '100%' }}
    >
      {/* Main streak display */}
      <div
        style={{
          display: 'flex',
          alignItems: 'baseline',
          gap: '8px',
          marginBottom: '12px',
        }}
      >
        <span
          data-testid="streak-current-value"
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: '32px',
            fontWeight: 'var(--weight-bold)',
            color: isActive ? 'var(--color-primary)' : 'var(--color-text-muted)',
            lineHeight: 1,
          }}
        >
          {currentStreak}
        </span>
        <span
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 'var(--text-caption)',
            fontWeight: 'var(--weight-medium)',
            color: 'var(--color-text-secondary)',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          {currentStreak === 1 ? 'week streak' : 'week streak'}
        </span>
      </div>

      {/* Secondary stats */}
      <div
        style={{
          display: 'flex',
          gap: '16px',
          marginTop: 'auto',
          paddingTop: '12px',
          borderTop: '1px solid var(--color-border)',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
          <span
            data-testid="streak-longest-value"
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 'var(--text-body)',
              fontWeight: 'var(--weight-semibold)',
              color: 'var(--color-text-primary)',
            }}
          >
            {longestStreak}
          </span>
          <span
            style={{
              fontFamily: 'var(--font-ui)',
              fontSize: 'var(--text-caption)',
              color: 'var(--color-text-muted)',
            }}
          >
            Best
          </span>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
          <span
            data-testid="streak-total-value"
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 'var(--text-body)',
              fontWeight: 'var(--weight-semibold)',
              color: 'var(--color-text-primary)',
            }}
          >
            {totalOneOnOnesHeld}
          </span>
          <span
            style={{
              fontFamily: 'var(--font-ui)',
              fontSize: 'var(--text-caption)',
              color: 'var(--color-text-muted)',
            }}
          >
            Total 1:1s
          </span>
        </div>
      </div>
    </div>
  );
}
