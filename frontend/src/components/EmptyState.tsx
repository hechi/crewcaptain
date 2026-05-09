'use client';

interface EmptyStateProps {
  message?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export default function EmptyState({
  message = 'No people in your directory yet.',
  actionLabel = 'Add your first person',
  onAction,
}: EmptyStateProps) {
  return (
    <div
      data-testid="empty-state"
      style={{
        textAlign: 'center',
        padding: '48px 24px',
        border: '1px dashed var(--color-border)',
        borderRadius: 'var(--radius-large)',
        backgroundColor: 'var(--color-bg-surface)',
      }}
    >
      <p style={{ fontSize: '16px', color: 'var(--color-text-secondary)', marginBottom: 'var(--space-4)' }}>
        {message}
      </p>
      {onAction && (
        <button
          type="button"
          onClick={onAction}
          data-testid="empty-state-cta"
          style={{
            padding: '10px 20px',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-semibold)',
            fontFamily: 'var(--font-mono)',
            cursor: 'pointer',
            boxShadow: 'var(--glow-primary)',
            transition: 'box-shadow 0.2s',
          }}
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}
