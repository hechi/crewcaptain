'use client';

import { useState, useEffect, useCallback } from 'react';
import { QuickNote } from '@/types/quick-note';
import {
  listQuickNotes,
  attachQuickNote,
  archiveQuickNote,
} from '@/lib/api-client';

interface OneOnOnePrepNotesProps {
  /** Auth token for API calls */
  token: string;
  /** The person this 1:1 is with */
  personId: string;
  /** The current 1:1 entry ID (for attaching). Null on create page before save. */
  entryId?: string | null;
}

/**
 * Prep notes panel for the 1:1 entry page.
 * Displays INBOX quick notes assigned to the person as "talking points" gathered
 * throughout the week. Allows attaching them to the current 1:1 (adds as agenda item)
 * or dismissing (archiving) them.
 *
 * Hidden when no prep notes exist to avoid visual noise.
 */
export default function OneOnOnePrepNotes({ token, personId, entryId }: OneOnOnePrepNotesProps) {
  const [notes, setNotes] = useState<QuickNote[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [collapsed, setCollapsed] = useState(false);

  const fetchNotes = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await listQuickNotes(token, {
        personId,
        status: 'INBOX',
        size: 50,
      });
      setNotes(result.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load prep notes');
    } finally {
      setLoading(false);
    }
  }, [token, personId]);

  useEffect(() => {
    fetchNotes();
  }, [fetchNotes]);

  const handleAttach = async (noteId: string) => {
    if (!entryId) return;
    try {
      await attachQuickNote(token, noteId, { entryId });
      await fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to attach note');
    }
  };

  const handleDismiss = async (noteId: string) => {
    try {
      await archiveQuickNote(token, noteId);
      await fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to archive note');
    }
  };

  const formatRelativeDate = (dateStr: string): string => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  };

  if (loading) {
    return (
      <div data-testid="prep-notes-loading" style={{ padding: 'var(--space-3)', color: 'var(--color-text-secondary)' }}>
        Loading prep notes...
      </div>
    );
  }

  if (error) {
    return (
      <div data-testid="prep-notes-error" style={{ padding: 'var(--space-3)', color: 'var(--color-alert)' }}>
        {error}
      </div>
    );
  }

  // Don't render if no notes to show
  if (notes.length === 0) {
    return null;
  }

  return (
    <div data-testid="prep-notes-panel" style={{ marginBottom: 'var(--space-4)' }}>
      {/* Header */}
      <button
        type="button"
        data-testid="prep-notes-toggle"
        onClick={() => setCollapsed(!collapsed)}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 'var(--space-2)',
          width: '100%',
          padding: '8px 12px',
          background: 'var(--color-bg-elevated)',
          border: '1px solid var(--color-border)',
          borderRadius: collapsed ? 'var(--radius-medium)' : 'var(--radius-medium) var(--radius-medium) 0 0',
          cursor: 'pointer',
          fontFamily: 'var(--font-mono)',
          fontSize: 'var(--text-caption)',
          color: 'var(--color-primary)',
          textTransform: 'uppercase',
          letterSpacing: '0.05em',
          transition: 'border-color 0.2s',
        }}
      >
        <span style={{ transform: collapsed ? 'rotate(-90deg)' : 'rotate(0deg)', transition: 'transform 0.2s', display: 'inline-block' }}>
          ▼
        </span>
        <span>Prep Notes</span>
        <span
          data-testid="prep-notes-count"
          style={{
            marginLeft: 'auto',
            padding: '2px 8px',
            background: 'var(--color-primary-muted)',
            borderRadius: 'var(--radius-full)',
            fontSize: 'var(--text-caption)',
            color: 'var(--color-primary)',
          }}
        >
          {notes.length}
        </span>
      </button>

      {/* Notes list */}
      <div
        style={{
          display: collapsed ? 'none' : 'flex',
          flexDirection: 'column',
          gap: '1px',
          border: '1px solid var(--color-border)',
          borderTop: 'none',
          borderRadius: '0 0 var(--radius-medium) var(--radius-medium)',
          overflow: 'hidden',
        }}
      >
        {notes.map((note) => (
          <div
            key={note.id}
            data-testid={`prep-note-item-${note.id}`}
            style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: 'var(--space-3)',
              padding: '10px 12px',
              background: 'var(--color-bg-elevated)',
            }}
          >
            {/* Note content */}
            <div style={{ flex: 1, minWidth: 0 }}>
              <p style={{
                margin: 0,
                fontSize: 'var(--text-body)',
                color: 'var(--color-text-primary)',
                wordBreak: 'break-word',
              }}>
                {note.text}
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)', marginTop: '4px' }}>
                <span
                  data-testid={`prep-note-date-${note.id}`}
                  style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-secondary)' }}
                >
                  {formatRelativeDate(note.createdAt)}
                </span>
                {note.sensitive && (
                  <span
                    data-testid={`prep-note-sensitive-${note.id}`}
                    style={{
                      fontSize: 'var(--text-caption)',
                      padding: '1px 6px',
                      background: 'var(--color-alert-muted)',
                      color: 'var(--color-alert)',
                      borderRadius: 'var(--radius-full)',
                    }}
                  >
                    Sensitive
                  </span>
                )}
              </div>
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', gap: 'var(--space-2)', flexShrink: 0 }}>
              {entryId && (
                <button
                  type="button"
                  data-testid={`prep-note-add-${note.id}`}
                  onClick={() => handleAttach(note.id)}
                  title="Add to Agenda"
                  style={{
                    padding: '4px 10px',
                    fontSize: 'var(--text-caption)',
                    fontFamily: 'var(--font-mono)',
                    background: 'var(--color-primary-muted)',
                    color: 'var(--color-primary)',
                    border: '1px solid var(--color-primary)',
                    borderRadius: 'var(--radius-medium)',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    whiteSpace: 'nowrap',
                  }}
                >
                  + Agenda
                </button>
              )}
              <button
                type="button"
                data-testid={`prep-note-dismiss-${note.id}`}
                onClick={() => handleDismiss(note.id)}
                title="Archive note"
                style={{
                  padding: '4px 10px',
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  background: 'transparent',
                  color: 'var(--color-text-secondary)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  whiteSpace: 'nowrap',
                }}
              >
                Dismiss
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
