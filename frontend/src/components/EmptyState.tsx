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
        border: '2px dashed var(--color-neutral-border)',
        borderRadius: 'var(--radius-large)',
        backgroundColor: 'var(--color-neutral-surface)',
      }}
    >
      <p style={{ fontSize: '16px', color: 'var(--color-neutral-text-muted)', marginBottom: 'var(--space-4)' }}>
        {message}
      </p>
      {onAction && (
        <button
          type="button"
          onClick={onAction}
          data-testid="empty-state-cta"
          style={{
            padding: '10px 20px',
            backgroundColor: 'var(--color-accent)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-medium)',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}
