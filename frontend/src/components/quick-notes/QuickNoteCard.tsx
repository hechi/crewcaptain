'use client';

import { QuickNote } from '@/types/quick-note';

interface QuickNoteCardProps {
  quickNote: QuickNote;
  onArchive: (id: string) => void;
  onConvert: (id: string) => void;
  onAttach: (id: string) => void;
  onDelete: (id: string) => void;
}

/**
 * Displays a single quick note with status, text, and action buttons.
 */
export default function QuickNoteCard({ quickNote, onArchive, onConvert, onAttach, onDelete }: QuickNoteCardProps) {
  const formattedDate = new Date(quickNote.createdAt).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  const isInbox = quickNote.status === 'INBOX';

  return (
    <div
      data-testid={`quick-note-card-${quickNote.id}`}
      style={{
        padding: 'var(--space-4)',
        border: quickNote.sensitive
          ? '1px solid var(--color-warning-muted)'
          : '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
        transition: 'border-color 0.2s',
      }}
    >
      {/* Header: date + status + sensitive badge */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span
            data-testid="quick-note-date"
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            {formattedDate}
          </span>
          {quickNote.sensitive && (
            <span
              data-testid="quick-note-sensitive-badge"
              style={{
                padding: '2px 6px',
                fontSize: '10px',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-warning)',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--color-warning-muted)',
                color: 'var(--color-warning)',
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
              }}
            >
              Sensitive
            </span>
          )}
        </div>
        <span
          data-testid="quick-note-status"
          style={{
            padding: '2px 8px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-full)',
            color: 'var(--color-text-secondary)',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          {quickNote.status}
        </span>
      </div>

      {/* Text */}
      <p
        data-testid="quick-note-text"
        style={{
          margin: '0 0 12px',
          fontSize: 'var(--text-body)',
          color: 'var(--color-text-primary)',
          lineHeight: '1.5',
          whiteSpace: 'pre-wrap',
        }}
      >
        {quickNote.text}
      </p>

      {/* Actions — only show for INBOX notes */}
      {isInbox && (
        <div data-testid="quick-note-actions" style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button
            type="button"
            onClick={() => onAttach(quickNote.id)}
            data-testid="quick-note-attach-btn"
            aria-label="Attach to 1:1"
            style={{
              padding: '4px 10px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-primary-muted)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'transparent',
              color: 'var(--color-primary)',
              cursor: 'pointer',
            }}
          >
            Attach to 1:1
          </button>
          <button
            type="button"
            onClick={() => onConvert(quickNote.id)}
            data-testid="quick-note-convert-btn"
            aria-label="Convert to action item"
            style={{
              padding: '4px 10px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-secondary)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'transparent',
              color: 'var(--color-secondary)',
              cursor: 'pointer',
            }}
          >
            → Action Item
          </button>
          <button
            type="button"
            onClick={() => onArchive(quickNote.id)}
            data-testid="quick-note-archive-btn"
            aria-label="Archive"
            style={{
              padding: '4px 10px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'transparent',
              color: 'var(--color-text-secondary)',
              cursor: 'pointer',
            }}
          >
            Archive
          </button>
          <button
            type="button"
            onClick={() => onDelete(quickNote.id)}
            data-testid="quick-note-delete-btn"
            aria-label="Delete quick note"
            style={{
              padding: '4px 10px',
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
      )}

      {/* Non-inbox notes only show delete */}
      {!isInbox && (
        <div data-testid="quick-note-actions" style={{ display: 'flex', gap: '8px' }}>
          <button
            type="button"
            onClick={() => onDelete(quickNote.id)}
            data-testid="quick-note-delete-btn"
            aria-label="Delete quick note"
            style={{
              padding: '4px 10px',
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
      )}
    </div>
  );
}
