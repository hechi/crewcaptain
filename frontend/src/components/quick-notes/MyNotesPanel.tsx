'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import {
  listQuickNotes,
  createQuickNote,
  updateQuickNote,
  deleteQuickNote,
  moveQuickNote,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import { QuickNote } from '@/types/quick-note';

/**
 * My Notes panel — a personal notes surface that slides/fades in from the LEFT.
 *
 * This lives alongside the full `/my-notes` page (it does not replace it). It is a
 * lightweight, always-available drawer for the manager's own (self-assigned) notes:
 * capture, tag, edit, reorder up/down, and delete. Opens via a left-edge trigger or
 * Ctrl+Shift+N.
 *
 * Accessibility (per common dialog/drawer guidance): role="dialog", aria-modal, a
 * labelled title, Escape to close, backdrop click to dismiss, focus moves into the
 * panel on open and returns to the trigger on close, and the slide animation respects
 * prefers-reduced-motion.
 */
export default function MyNotesPanel() {
  const { getToken, isAuthenticated } = useStableToken();

  const [isOpen, setIsOpen] = useState(false);
  const [notes, setNotes] = useState<QuickNote[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tagFilter, setTagFilter] = useState<string | null>(null);
  const [newText, setNewText] = useState('');
  const [newTag, setNewTag] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const triggerRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const newTextRef = useRef<HTMLTextAreaElement>(null);

  const fetchNotes = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const result = await listQuickNotes(token, {
        selfAssigned: true,
        status: 'INBOX',
        tag: tagFilter || undefined,
        page: 0,
        size: 100,
      });
      setNotes(result.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load notes');
    } finally {
      setLoading(false);
    }
  }, [getToken, tagFilter]);

  const open = useCallback(() => {
    if (!isAuthenticated) return;
    setIsOpen(true);
  }, [isAuthenticated]);

  const close = useCallback(() => {
    setIsOpen(false);
    // Restore focus to the trigger that opened the panel.
    triggerRef.current?.focus();
  }, []);

  // Fetch whenever the panel is open or the tag filter changes.
  useEffect(() => {
    if (isOpen) {
      fetchNotes();
    }
  }, [isOpen, fetchNotes]);

  // Keyboard: Ctrl+Shift+N toggles, Escape closes.
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && (e.key === 'N' || e.key === 'n')) {
        e.preventDefault();
        if (isOpen) {
          close();
        } else {
          open();
        }
      }
      if (e.key === 'Escape' && isOpen) {
        e.preventDefault();
        close();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, open, close]);

  // Move focus into the panel when it opens.
  useEffect(() => {
    if (isOpen && newTextRef.current) {
      const timer = setTimeout(() => newTextRef.current?.focus(), 50);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  // Simple focus trap: keep Tab within the panel while it is open.
  const handlePanelKeyDown = (e: React.KeyboardEvent) => {
    if (e.key !== 'Tab' || !panelRef.current) return;
    const focusable = panelRef.current.querySelectorAll<HTMLElement>(
      'button, [href], input, textarea, select, [tabindex]:not([tabindex="-1"])'
    );
    const visible = Array.from(focusable).filter((el) => !el.hasAttribute('disabled'));
    if (visible.length === 0) return;
    const first = visible[0];
    const last = visible[visible.length - 1];
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first.focus();
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = newText.trim();
    if (!trimmed) return;
    const token = getToken();
    if (!token) return;
    setSubmitting(true);
    setError(null);
    try {
      await createQuickNote(token, {
        text: trimmed,
        selfAssigned: true,
        tag: newTag.trim() || undefined,
      });
      setNewText('');
      setNewTag('');
      await fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create note');
    } finally {
      setSubmitting(false);
    }
  };

  const handleMove = async (id: string, direction: 'UP' | 'DOWN') => {
    const token = getToken();
    if (!token) return;
    try {
      await moveQuickNote(token, id, direction);
      await fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to move note');
    }
  };

  const handleEdit = async (id: string, text: string, tag: string | null) => {
    const token = getToken();
    if (!token) return;
    try {
      await updateQuickNote(token, id, { text, tag, updateTag: true });
      await fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update note');
    }
  };

  const handleDelete = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await deleteQuickNote(token, id);
      await fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete note');
    }
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) close();
  };

  if (!isAuthenticated) return null;

  const availableTags = Array.from(
    new Set(notes.map((n) => n.tag).filter((t): t is string => !!t))
  );
  const visibleTags = tagFilter && !availableTags.includes(tagFilter)
    ? [tagFilter, ...availableTags]
    : availableTags;

  return (
    <>
      {/* Left-edge trigger */}
      <button
        ref={triggerRef}
        type="button"
        onClick={open}
        data-testid="my-notes-panel-trigger"
        aria-label="Open my notes (Ctrl+Shift+N)"
        title="My notes (Ctrl+Shift+N)"
        style={{
          position: 'fixed',
          top: '50%',
          left: 0,
          transform: 'translateY(-50%)',
          padding: '12px 6px',
          border: '1px solid var(--color-border-glow)',
          borderLeft: 'none',
          borderRadius: '0 var(--radius-medium) var(--radius-medium) 0',
          backgroundColor: 'var(--color-bg-elevated)',
          color: 'var(--color-primary)',
          fontSize: '12px',
          fontFamily: 'var(--font-mono)',
          fontWeight: '700',
          letterSpacing: '1px',
          writingMode: 'vertical-rl',
          cursor: 'pointer',
          boxShadow: 'var(--glow-primary)',
          zIndex: 850,
        }}
      >
        NOTES
      </button>

      {isOpen && (
        <div
          data-testid="my-notes-panel-overlay"
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 1050,
            animation: 'mnp-fade-in 0.25s ease-out forwards',
          }}
        >
          {/* Backdrop */}
          <div
            data-testid="my-notes-panel-backdrop"
            onClick={handleBackdropClick}
            style={{
              position: 'absolute',
              inset: 0,
              backgroundColor: 'rgba(0, 0, 0, 0.6)',
              backdropFilter: 'blur(3px)',
            }}
          />

          {/* Sliding panel (from the left) */}
          <div
            ref={panelRef}
            data-testid="my-notes-panel"
            role="dialog"
            aria-modal="true"
            aria-labelledby="my-notes-panel-title"
            onKeyDown={handlePanelKeyDown}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              bottom: 0,
              width: '100%',
              maxWidth: '420px',
              display: 'flex',
              flexDirection: 'column',
              backgroundColor: 'var(--glass-elevated-bg, rgba(30, 35, 48, 0.95))',
              borderRight: '1px solid var(--color-border-glow, rgba(0, 240, 255, 0.2))',
              backdropFilter: 'var(--glass-elevated-blur, blur(16px))',
              boxShadow: '8px 0 40px rgba(0, 0, 0, 0.4)',
              animation: 'mnp-slide-in-left 0.28s ease-out forwards',
            }}
          >
            {/* Header */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: 'var(--space-4)',
                borderBottom: '1px solid var(--color-border)',
              }}
            >
              <h2
                id="my-notes-panel-title"
                style={{
                  margin: 0,
                  fontSize: '18px',
                  fontWeight: '600',
                  fontFamily: 'var(--font-heading, monospace)',
                  color: 'var(--color-primary)',
                }}
              >
                📝 My Notes
              </h2>
              <button
                type="button"
                onClick={close}
                data-testid="my-notes-panel-close"
                aria-label="Close my notes"
                style={{
                  padding: '4px 10px',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-small)',
                  backgroundColor: 'transparent',
                  color: 'var(--color-text-secondary)',
                  fontFamily: 'var(--font-mono)',
                  cursor: 'pointer',
                }}
              >
                ✕
              </button>
            </div>

            {/* Scrollable body */}
            <div style={{ flex: 1, overflowY: 'auto', padding: 'var(--space-4)' }}>
              {/* Create form */}
              <form onSubmit={handleCreate} style={{ marginBottom: 'var(--space-4)' }}>
                <textarea
                  ref={newTextRef}
                  value={newText}
                  onChange={(e) => setNewText(e.target.value)}
                  placeholder="Capture a note..."
                  data-testid="my-notes-panel-new-text"
                  aria-label="New note text"
                  rows={3}
                  onKeyDown={(e) => {
                    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                      e.preventDefault();
                      handleCreate(e);
                    }
                  }}
                  style={{
                    width: '100%',
                    padding: '10px 12px',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-medium)',
                    fontSize: 'var(--text-body)',
                    fontFamily: 'var(--font-ui)',
                    backgroundColor: 'var(--color-bg-elevated)',
                    color: 'var(--color-text-primary)',
                    resize: 'vertical',
                    boxSizing: 'border-box',
                  }}
                />
                <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                  <input
                    type="text"
                    value={newTag}
                    onChange={(e) => setNewTag(e.target.value)}
                    placeholder="#tag (optional)"
                    maxLength={30}
                    data-testid="my-notes-panel-new-tag"
                    aria-label="New note tag"
                    style={{
                      flex: 1,
                      padding: '6px 10px',
                      fontSize: 'var(--text-caption)',
                      fontFamily: 'var(--font-mono)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-full)',
                      backgroundColor: 'var(--color-bg-elevated)',
                      color: 'var(--color-text-primary)',
                    }}
                  />
                  <button
                    type="submit"
                    disabled={!newText.trim() || submitting}
                    data-testid="my-notes-panel-new-submit"
                    style={{
                      padding: '6px 16px',
                      border: 'none',
                      borderRadius: 'var(--radius-medium)',
                      backgroundColor: newText.trim() && !submitting ? 'var(--color-primary)' : 'var(--color-bg-elevated)',
                      color: newText.trim() && !submitting ? 'var(--color-bg-base)' : 'var(--color-text-muted)',
                      fontSize: 'var(--text-caption)',
                      fontWeight: '600',
                      fontFamily: 'var(--font-mono)',
                      cursor: newText.trim() && !submitting ? 'pointer' : 'not-allowed',
                    }}
                  >
                    {submitting ? 'Saving...' : 'Add'}
                  </button>
                </div>
              </form>

              {/* Tag filter */}
              {visibleTags.length > 0 && (
                <div
                  data-testid="my-notes-panel-tag-filter"
                  style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: 'var(--space-4)', alignItems: 'center' }}
                >
                  <span style={{ fontSize: 'var(--text-caption)', fontFamily: 'var(--font-mono)', color: 'var(--color-text-muted)' }}>
                    Tags:
                  </span>
                  {visibleTags.map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setTagFilter((cur) => (cur === t ? null : t))}
                      data-testid={`my-notes-panel-tag-filter-${t}`}
                      style={{
                        padding: '3px 10px',
                        fontSize: 'var(--text-caption)',
                        fontFamily: 'var(--font-mono)',
                        border: tagFilter === t ? '1px solid var(--color-secondary)' : '1px solid var(--color-border)',
                        borderRadius: 'var(--radius-full)',
                        backgroundColor: tagFilter === t ? 'var(--color-secondary-hover)' : 'transparent',
                        color: tagFilter === t ? 'var(--color-bg-base)' : 'var(--color-text-secondary)',
                        cursor: 'pointer',
                      }}
                    >
                      #{t}
                    </button>
                  ))}
                </div>
              )}

              {error && (
                <div data-testid="my-notes-panel-error" style={{ color: 'var(--color-alert)', marginBottom: 'var(--space-3)', fontSize: 'var(--text-caption)' }}>
                  {error}
                </div>
              )}

              {/* Notes list */}
              {loading && notes.length === 0 ? (
                <div data-testid="my-notes-panel-loading" style={{ color: 'var(--color-text-muted)', textAlign: 'center', padding: 'var(--space-4)' }}>
                  Loading notes...
                </div>
              ) : notes.length === 0 ? (
                <div data-testid="my-notes-panel-empty" style={{ color: 'var(--color-text-muted)', textAlign: 'center', padding: 'var(--space-4)' }}>
                  No notes yet — capture one above.
                </div>
              ) : (
                <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                  {notes.map((note, index) => (
                    <PanelNoteCard
                      key={note.id}
                      note={note}
                      isFirst={index === 0}
                      isLast={index === notes.length - 1}
                      onMove={handleMove}
                      onEdit={handleEdit}
                      onDelete={handleDelete}
                    />
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>
      )}

      <style>{`
        @keyframes mnp-fade-in {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes mnp-slide-in-left {
          from { transform: translateX(-100%); }
          to { transform: translateX(0); }
        }
        @media (prefers-reduced-motion: reduce) {
          @keyframes mnp-fade-in { from { opacity: 1; } to { opacity: 1; } }
          @keyframes mnp-slide-in-left { from { transform: none; } to { transform: none; } }
        }
      `}</style>
    </>
  );
}

interface PanelNoteCardProps {
  note: QuickNote;
  isFirst: boolean;
  isLast: boolean;
  onMove: (id: string, direction: 'UP' | 'DOWN') => void;
  onEdit: (id: string, text: string, tag: string | null) => void;
  onDelete: (id: string) => void;
}

function PanelNoteCard({ note, isFirst, isLast, onMove, onEdit, onDelete }: PanelNoteCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState(note.text);
  const [editTag, setEditTag] = useState(note.tag ?? '');

  const startEdit = () => {
    setEditText(note.text);
    setEditTag(note.tag ?? '');
    setIsEditing(true);
  };

  const saveEdit = () => {
    const trimmed = editText.trim();
    if (!trimmed) return;
    onEdit(note.id, trimmed, editTag.trim() || null);
    setIsEditing(false);
  };

  const iconBtn = (disabled: boolean): React.CSSProperties => ({
    padding: '2px 8px',
    fontSize: 'var(--text-caption)',
    fontFamily: 'var(--font-mono)',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-small)',
    backgroundColor: 'transparent',
    color: disabled ? 'var(--color-text-muted)' : 'var(--color-text-secondary)',
    cursor: disabled ? 'not-allowed' : 'pointer',
  });

  return (
    <li
      data-testid={`my-notes-panel-card-${note.id}`}
      style={{
        padding: 'var(--space-3)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
      }}
    >
      {!isEditing ? (
        <>
          <p style={{ margin: '0 0 8px', fontSize: 'var(--text-body)', color: 'var(--color-text-primary)', whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>
            {note.text}
          </p>
          {note.tag && (
            <span
              data-testid={`my-notes-panel-tag-${note.id}`}
              style={{
                display: 'inline-block',
                marginBottom: '8px',
                padding: '2px 8px',
                fontSize: '10px',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-secondary)',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--color-secondary-hover)',
                color: 'var(--color-bg-base)',
              }}
            >
              #{note.tag}
            </span>
          )}
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', alignItems: 'center' }}>
            <button
              type="button"
              onClick={() => onMove(note.id, 'UP')}
              disabled={isFirst}
              data-testid={`my-notes-panel-move-up-${note.id}`}
              aria-label="Move note up"
              style={iconBtn(isFirst)}
            >
              ↑
            </button>
            <button
              type="button"
              onClick={() => onMove(note.id, 'DOWN')}
              disabled={isLast}
              data-testid={`my-notes-panel-move-down-${note.id}`}
              aria-label="Move note down"
              style={iconBtn(isLast)}
            >
              ↓
            </button>
            <button
              type="button"
              onClick={startEdit}
              data-testid={`my-notes-panel-edit-${note.id}`}
              aria-label="Edit note"
              style={iconBtn(false)}
            >
              Edit
            </button>
            <button
              type="button"
              onClick={() => onDelete(note.id)}
              data-testid={`my-notes-panel-delete-${note.id}`}
              aria-label="Delete note"
              style={{ ...iconBtn(false), border: '1px solid var(--color-alert-muted)', color: 'var(--color-alert)' }}
            >
              Delete
            </button>
          </div>
        </>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <textarea
            value={editText}
            onChange={(e) => setEditText(e.target.value)}
            data-testid={`my-notes-panel-edit-text-${note.id}`}
            aria-label="Edit note text"
            rows={3}
            style={{
              width: '100%',
              padding: '8px 10px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-ui)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              resize: 'vertical',
              boxSizing: 'border-box',
            }}
          />
          <input
            type="text"
            value={editTag}
            onChange={(e) => setEditTag(e.target.value)}
            placeholder="#tag (optional)"
            maxLength={30}
            data-testid={`my-notes-panel-edit-tag-${note.id}`}
            aria-label="Edit note tag"
            style={{
              padding: '4px 10px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-full)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              maxWidth: '200px',
            }}
          />
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              onClick={saveEdit}
              disabled={!editText.trim()}
              data-testid={`my-notes-panel-edit-save-${note.id}`}
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                fontWeight: '600',
                border: 'none',
                borderRadius: 'var(--radius-small)',
                backgroundColor: editText.trim() ? 'var(--color-primary)' : 'var(--color-bg-elevated)',
                color: editText.trim() ? 'var(--color-bg-base)' : 'var(--color-text-muted)',
                cursor: editText.trim() ? 'pointer' : 'not-allowed',
              }}
            >
              Save
            </button>
            <button
              type="button"
              onClick={() => setIsEditing(false)}
              data-testid={`my-notes-panel-edit-cancel-${note.id}`}
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-small)',
                backgroundColor: 'transparent',
                color: 'var(--color-text-secondary)',
                cursor: 'pointer',
              }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </li>
  );
}
