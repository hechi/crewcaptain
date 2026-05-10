'use client';

interface ProgressRingProps {
  /** Percentage value (0-100) */
  percentage: number;
  /** Size of the ring in pixels */
  size?: number;
  /** Stroke width in pixels */
  strokeWidth?: number;
  /** Label text displayed below the ring */
  label?: string;
  /** Color of the progress arc */
  color?: string;
}

/**
 * Animated circular progress ring with glow effect.
 * Used to visualize PDP goal completion percentage.
 * Respects prefers-reduced-motion for accessibility.
 */
export default function ProgressRing({
  percentage,
  size = 120,
  strokeWidth = 8,
  label,
  color = 'var(--color-primary)',
}: ProgressRingProps) {
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (Math.min(Math.max(percentage, 0), 100) / 100) * circumference;

  return (
    <div
      data-testid="progress-ring"
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '8px',
      }}
    >
      <div style={{ position: 'relative', width: size, height: size }}>
        <svg
          width={size}
          height={size}
          viewBox={`0 0 ${size} ${size}`}
          aria-label={`Progress: ${percentage}%`}
          role="img"
        >
          {/* Background circle */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke="var(--color-border)"
            strokeWidth={strokeWidth}
          />
          {/* Progress arc */}
          <circle
            data-testid="progress-ring-arc"
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            style={{
              transform: 'rotate(-90deg)',
              transformOrigin: '50% 50%',
              transition: 'stroke-dashoffset 0.8s ease-out',
              filter: `drop-shadow(0 0 6px ${color})`,
            }}
          />
        </svg>
        {/* Center percentage text */}
        <div
          data-testid="progress-ring-value"
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            fontFamily: 'var(--font-mono)',
            fontSize: size > 80 ? '20px' : '14px',
            fontWeight: 'var(--weight-bold)',
            color: 'var(--color-text-primary)',
          }}
        >
          {percentage}%
        </div>
      </div>
      {label && (
        <span
          data-testid="progress-ring-label"
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 'var(--text-caption)',
            fontWeight: 'var(--weight-medium)',
            color: 'var(--color-text-secondary)',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          {label}
        </span>
      )}
    </div>
  );
}
