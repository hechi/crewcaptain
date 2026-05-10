'use client';

import { Kudos } from '@/types/kudos';

interface KudosCardProps {
  kudos: Kudos;
  onDelete: (id: string) => void;
}

/**
 * Displays a single kudos entry with date, text, tags, and delete action.
 */
export default function KudosCard({ kudos, onDelete }: KudosCardProps) {
  const formattedDate = new Date(kudos.date + 'T00:00:00').toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });

  return (
    <div
      data-testid={`kudos-card-${kudos.id}`}
      style={{
        padding: 'var(--space-4)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
        transition: 'border-color 0.2s',
      }}
    >
      {/* Header: date + delete */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <span
          data-testid="kudos-date"
          style={{
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
          }}
        >
          {formattedDate}
        </span>
        <button
          type="button"
          onClick={() => onDelete(kudos.id)}
          data-testid="kudos-delete-btn"
          aria-label="Delete kudos"
          style={{
            padding: '4px 8px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-alert-muted)',
            borderRadius: 'var(--radius-small)',
            backgroundColor: 'transparent',
            color: 'var(--color-alert)',
            cursor: 'pointer',
          }}
        >
          Delete
        </button>
      </div>

      {/* Text */}
      <p
        data-testid="kudos-text"
        style={{
          margin: '0 0 8px',
          fontSize: 'var(--text-body)',
          color: 'var(--color-text-primary)',
          lineHeight: '1.5',
          whiteSpace: 'pre-wrap',
        }}
      >
        {kudos.text}
      </p>

      {/* Tags */}
      {kudos.tags.length > 0 && (
        <div data-testid="kudos-tags" style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
          {kudos.tags.map((tag) => (
            <span
              key={tag}
              style={{
                padding: '2px 8px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-secondary)',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--color-secondary-muted)',
                color: 'var(--color-secondary)',
              }}
            >
              {tag}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
