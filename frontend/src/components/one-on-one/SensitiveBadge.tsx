'use client';

interface SensitiveBadgeProps {
  className?: string;
}

/**
 * Visual indicator (lock icon) for sensitive entries.
 * Displays a small lock badge to mark content as sensitive.
 */
export default function SensitiveBadge({ className }: SensitiveBadgeProps) {
  return (
    <span
      data-testid="sensitive-badge"
      className={className}
      role="img"
      aria-label="Sensitive content"
      title="Sensitive"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        padding: '2px 8px',
        backgroundColor: 'var(--color-warning-bg)',
        color: '#92400e',
        borderRadius: 'var(--radius-full)',
        fontSize: 'var(--text-caption)',
        fontWeight: 'var(--weight-medium)',
      }}
    >
      <svg
        width="12"
        height="12"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
        <path d="M7 11V7a5 5 0 0 1 10 0v4" />
      </svg>
      Sensitive
    </span>
  );
}
