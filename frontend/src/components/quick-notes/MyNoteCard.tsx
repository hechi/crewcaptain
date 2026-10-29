'use client';

import { useState } from 'react';
import { QuickNote } from '@/types/quick-note';

export interface MyNoteCardProps {
  note: QuickNote;
  /** Position flags so the move-up/down arrows can disable at the list edges. */
  isFirst?: boolean;
  isLast?: boolean;
  /** Drag-and-drop visual state. */
  isDragging?: boolean;
  isDropTarget?: boolean;
  /** Action callbacks. A control is only rendered when its callback is provided. */
  onMove?: (id: string, direction: 'UP' | 'DOWN') => void;
  onEdit?: (id: string, text: string, tag: string | null) => void;
  onConvert?: (id: string, dueDate?: string) => void;
  onArchive?: (id: string) => void;
  onDelete?: (id: string) => void;
  /** Drag-and-drop handlers. Dragging is enabled only when all are provided. */
  onDragStart?: (id: string) => void;
  onDragOverNote?: (id: string) => void;
  onDragEnd?: () => void;
  onDropOnNote?: (id: string) => void;
}

/**
 * The single, canonical personal-note card used by BOTH the `/my-notes` page and the
 * slide-in `MyNotesPanel`, so the two surfaces look and behave identically.
 *
 * Shows the note's date, sensitive/tag/status badges, the text (with an inline edit
 * mode for text + tag), and the full action set: reorder (↑/↓ + mouse drag-and-drop),
 * convert-to-TODO (with an optional due date), archive, edit, and delete. Each action
 * renders only when its callback is supplied. Drag-and-drop is enabled when the drag
 * handlers are supplied and the card is not in edit mode (so text selection still works).
 */
export default function MyNoteCard({
  note,
  isFirst = false,
  isLast = false,
  isDragging = false,
  isDropTarget = false,
  onMove,
  onEdit,
  onConvert,
  onArchive,
  onDelete,
  onDragStart,
  onDragOverNote,
  onDragEnd,
  onDropOnNote,
}: MyNoteCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState(note.text);
  const [editTag, setEditTag] = useState(note.tag ?? '');
  const [dueDate, setDueDate] = useState('');

  const formattedDate = new Date(note.createdAt).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  const isInbox = note.status === 'INBOX';
  const draggable = Boolean(onDragStart && onDragOverNote && onDropOnNote && onDragEnd) && !isEditing;

  const startEditing = () => {
    setEditText(note.text);
    setEditTag(note.tag ?? '');
    setIsEditing(true);
  };

  const cancelEditing = () => setIsEditing(false);

  const saveEditing = () => {
    const trimmed = editText.trim();
    if (!trimmed || !onEdit) return;
    onEdit(note.id, trimmed, editTag.trim() || null);
    setIsEditing(false);
  };

  const smallBtn = (variant: 'default' | 'primary' | 'alert' = 'default', disabled = false): React.CSSProperties => ({
    padding: '4px 10px',
    fontSize: 'var(--text-caption)',
    fontFamily: 'var(--font-mono)',
    border:
      variant === 'alert'
        ? '1px solid var(--color-alert-muted)'
        : variant === 'primary'
        ? '1px solid var(--color-primary)'
        : '1px solid var(--color-border)',
    borderRadius: 'var(--radius-small)',
    backgroundColor: variant === 'primary' ? 'var(--color-primary-muted)' : 'transparent',
    color: disabled
      ? 'var(--color-text-muted)'
      : variant === 'alert'
      ? 'var(--color-alert)'
      : variant === 'primary'
      ? 'var(--color-primary)'
      : 'var(--color-text-secondary)',
    cursor: disabled ? 'not-allowed' : 'pointer',
  });

  return (
    <div
      data-testid={`my-note-card-${note.id}`}
      draggable={draggable}
      onDragStart={
        draggable
          ? (e) => {
              e.dataTransfer.setData('text/plain', note.id);
              e.dataTransfer.effectAllowed = 'move';
              onDragStart!(note.id);
            }
          : undefined
      }
      onDragOver={
        draggable
          ? (e) => {
              e.preventDefault();
              e.dataTransfer.dropEffect = 'move';
              onDragOverNote!(note.id);
            }
          : undefined
      }
      onDrop={
        draggable
          ? (e) => {
              e.preventDefault();
              onDropOnNote!(note.id);
            }
          : undefined
      }
      onDragEnd={draggable ? onDragEnd : undefined}
      style={{
        padding: 'var(--space-4)',
        border: isDropTarget
          ? '1px solid var(--color-primary)'
          : note.sensitive
          ? '1px solid var(--color-warning-muted)'
          : '1px solid var(--color-border)',
        borderTop: isDropTarget ? '3px solid var(--color-primary)' : undefined,
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
        opacity: isDragging ? 0.5 : 1,
        cursor: draggable ? 'grab' : 'default',
        transition: 'border-color 0.2s, opacity 0.15s',
      }}
    >
      {/* Header: drag handle + date + badges + status */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {onDragStart && (
            <span
              aria-hidden="true"
              title="Drag to reorder"
              data-testid={`my-note-drag-handle-${note.id}`}
              style={{
                fontFamily: 'var(--font-mono)',
                color: 'var(--color-text-muted)',
                userSelect: 'none',
                cursor: draggable ? 'grab' : 'default',
              }}
            >
              ⠿
            </span>
          )}
          <span
            data-testid="my-note-date"
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            {formattedDate}
          </span>
          {note.sensitive && (
            <span
              data-testid="my-note-sensitive-badge"
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
          {note.tag && (
            <span
              data-testid="my-note-tag"
              style={{
                padding: '2px 8px',
                fontSize: '10px',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-secondary)',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--color-secondary-hover)',
                color: 'var(--color-bg-base)',
                letterSpacing: '0.5px',
              }}
            >
              #{note.tag}
            </span>
          )}
        </div>
        <span
          data-testid="my-note-status"
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
          {note.status}
        </span>
      </div>

      {/* Text (view mode) */}
      {!isEditing && (
        <p
          data-testid="my-note-text"
          style={{
            margin: '0 0 12px',
            fontSize: 'var(--text-body)',
            color: 'var(--color-text-primary)',
            lineHeight: '1.5',
            whiteSpace: 'pre-wrap',
          }}
        >
          {note.text}
        </p>
      )}

      {/* Edit mode */}
      {isEditing && (
        <div data-testid="my-note-edit" style={{ marginBottom: '12px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <textarea
            value={editText}
            onChange={(e) => setEditText(e.target.value)}
            data-testid="my-note-edit-text-input"
            aria-label="Edit note text"
            rows={3}
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
              minHeight: '60px',
              boxSizing: 'border-box',
            }}
          />
          <input
            type="text"
            value={editTag}
            onChange={(e) => setEditTag(e.target.value)}
            placeholder="#tag (optional)"
            maxLength={30}
            data-testid="my-note-edit-tag-input"
            aria-label="Edit tag"
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
              onClick={saveEditing}
              disabled={!editText.trim()}
              data-testid="my-note-edit-save-btn"
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
              onClick={cancelEditing}
              data-testid="my-note-edit-cancel-btn"
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

      {/* Actions */}
      {!isEditing && (
        <div data-testid="my-note-actions" style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
          {onMove && (
            <>
              <button
                type="button"
                onClick={() => onMove(note.id, 'UP')}
                disabled={isFirst}
                data-testid="my-note-move-up-btn"
                aria-label="Move note up"
                style={smallBtn('default', isFirst)}
              >
                ↑
              </button>
              <button
                type="button"
                onClick={() => onMove(note.id, 'DOWN')}
                disabled={isLast}
                data-testid="my-note-move-down-btn"
                aria-label="Move note down"
                style={smallBtn('default', isLast)}
              >
                ↓
              </button>
            </>
          )}
          {isInbox && onConvert && (
            <>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                data-testid="my-note-due-date-input"
                aria-label="Due date (optional)"
                style={{
                  padding: '4px 8px',
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-small)',
                  backgroundColor: 'var(--color-bg-elevated)',
                  color: 'var(--color-text-primary)',
                }}
              />
              <button
                type="button"
                onClick={() => onConvert(note.id, dueDate || undefined)}
                data-testid="my-note-convert-btn"
                aria-label="Convert to TODO"
                style={smallBtn('primary')}
              >
                Convert to TODO
              </button>
            </>
          )}
          {isInbox && onArchive && (
            <button
              type="button"
              onClick={() => onArchive(note.id)}
              data-testid="my-note-archive-btn"
              aria-label="Archive"
              style={smallBtn('default')}
            >
              Archive
            </button>
          )}
          {isInbox && onEdit && (
            <button
              type="button"
              onClick={startEditing}
              data-testid="my-note-edit-btn"
              aria-label="Edit note"
              style={smallBtn('default')}
            >
              Edit
            </button>
          )}
          {onDelete && (
            <button
              type="button"
              onClick={() => onDelete(note.id)}
              data-testid="my-note-delete-btn"
              aria-label="Delete note"
              style={smallBtn('alert')}
            >
              Delete
            </button>
          )}
        </div>
      )}
    </div>
  );
}
