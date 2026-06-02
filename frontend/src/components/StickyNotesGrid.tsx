'use client';

import { useState, useCallback, useRef, useEffect } from 'react';
import { GripVertical, Trash2, X, Eye, EyeOff, Pencil } from 'lucide-react';
import { PinnedRememberItem, StickyNoteColor } from '@/types/person';

const STICKY_COLORS: { value: StickyNoteColor; bg: string; border: string; label: string }[] = [
  { value: 'cyan', bg: 'rgba(0, 240, 255, 0.12)', border: 'rgba(0, 240, 255, 0.3)', label: 'Cyan' },
  { value: 'purple', bg: 'rgba(168, 85, 247, 0.12)', border: 'rgba(168, 85, 247, 0.3)', label: 'Purple' },
  { value: 'green', bg: 'rgba(57, 255, 133, 0.12)', border: 'rgba(57, 255, 133, 0.3)', label: 'Green' },
  { value: 'amber', bg: 'rgba(255, 214, 0, 0.12)', border: 'rgba(255, 214, 0, 0.3)', label: 'Amber' },
  { value: 'pink', bg: 'rgba(255, 45, 123, 0.12)', border: 'rgba(255, 45, 123, 0.3)', label: 'Pink' },
  { value: 'slate', bg: 'rgba(122, 133, 153, 0.12)', border: 'rgba(122, 133, 153, 0.3)', label: 'Slate' },
];

const STARTER_TEMPLATES = [
  { text: 'Miro: Company Project Board — link', tag: 'Link' },
  { text: 'Has 2 kids — pick-up on Fridays', tag: 'Family' },
  { text: 'Building house — major move planned Oct 2026', tag: 'Life event' },
  { text: 'Shared: housing_budget.xlsx (notes on page 2)', tag: 'Docs' },
  { text: 'Retro board link (they run team retro)', tag: 'Manager' },
];

export function getColorStyles(color: StickyNoteColor) {
  return STICKY_COLORS.find((c) => c.value === color) || STICKY_COLORS[0];
}

interface StickyNotesGridProps {
  items: PinnedRememberItem[];
  onAdd: (data: { text: string; color?: string; tag?: string; sensitive?: boolean }) => void;
  onUpdate: (itemId: string, data: { text: string; color?: string; tag?: string; sensitive?: boolean }) => void;
  onRemove: (itemId: string) => void;
  onReorder: (orderedIds: string[]) => void;
}

export default function StickyNotesGrid({ items, onAdd, onUpdate, onRemove, onReorder }: StickyNotesGridProps) {
  const [showComposer, setShowComposer] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deletedItem, setDeletedItem] = useState<PinnedRememberItem | null>(null);
  const undoTimerRef = useRef<NodeJS.Timeout | null>(null);
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);

  // Clear undo timer on unmount
  useEffect(() => {
    return () => {
      if (undoTimerRef.current) clearTimeout(undoTimerRef.current);
    };
  }, []);

  const handleDelete = useCallback((item: PinnedRememberItem) => {
    setDeletedItem(item);
    onRemove(item.id);
    if (undoTimerRef.current) clearTimeout(undoTimerRef.current);
    undoTimerRef.current = setTimeout(() => setDeletedItem(null), 10000);
  }, [onRemove]);

  const handleUndo = useCallback(() => {
    if (deletedItem) {
      onAdd({
        text: deletedItem.text,
        color: deletedItem.color,
        tag: deletedItem.tag || undefined,
        sensitive: deletedItem.sensitive,
      });
      setDeletedItem(null);
      if (undoTimerRef.current) clearTimeout(undoTimerRef.current);
    }
  }, [deletedItem, onAdd]);

  const handleDragStart = useCallback((index: number) => { setDraggedIndex(index); }, []);
  const handleDragEnd = useCallback(() => { setDraggedIndex(null); setDragOverIndex(null); }, []);
  const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === index) return;
    setDragOverIndex(index);
  }, [draggedIndex]);

  const handleDrop = useCallback((e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === dropIndex) { handleDragEnd(); return; }
    const newItems = [...items];
    const [removed] = newItems.splice(draggedIndex, 1);
    newItems.splice(dropIndex, 0, removed);
    onReorder(newItems.map((item) => item.id));
    handleDragEnd();
  }, [draggedIndex, items, onReorder, handleDragEnd]);

  const moveItem = (index: number, direction: 'up' | 'down') => {
    const newItems = [...items];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= newItems.length) return;
    [newItems[index], newItems[targetIndex]] = [newItems[targetIndex], newItems[index]];
    onReorder(newItems.map((item) => item.id));
  };

  return (
    <div data-testid="sticky-notes-grid">
      <h4 style={{
        margin: '0 0 12px',
        fontSize: 'var(--text-caption)',
        fontWeight: 'var(--weight-semibold)',
        fontFamily: 'var(--font-mono)',
        color: 'var(--color-primary)',
        letterSpacing: '0.5px',
        textTransform: 'uppercase',
      }}>
        Sticky Notes
      </h4>

      {items.length === 0 && !showComposer && (
        <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--text-body)', fontStyle: 'italic', margin: '0 0 16px' }}>
          No sticky notes yet — pin quick reminders here (links, family notes, docs).
        </p>
      )}

      {/* Notes Grid */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '12px',
        marginBottom: '16px',
      }}>
        {items.map((item, index) => (
          editingId === item.id ? (
            <StickyNoteEditor
              key={item.id}
              item={item}
              onSave={(data) => { onUpdate(item.id, data); setEditingId(null); }}
              onCancel={() => setEditingId(null)}
            />
          ) : (
            <StickyNoteCard
              key={item.id}
              item={item}
              index={index}
              totalItems={items.length}
              draggedIndex={draggedIndex}
              dragOverIndex={dragOverIndex}
              onEdit={() => setEditingId(item.id)}
              onDelete={() => handleDelete(item)}
              onDragStart={handleDragStart}
              onDragEnd={handleDragEnd}
              onDragOver={handleDragOver}
              onDrop={handleDrop}
              onMoveItem={moveItem}
            />
          )
        ))}
      </div>

      {/* Soft limit warning */}
      {items.length >= 8 && items.length < 10 && (
        <p style={{ fontSize: 'var(--text-caption)', color: 'var(--color-warning)', margin: '0 0 8px' }}>
          {10 - items.length} sticky notes remaining before recommended limit.
        </p>
      )}
      {items.length >= 10 && (
        <p style={{ fontSize: 'var(--text-caption)', color: 'var(--color-warning)', margin: '0 0 8px' }}>
          You have {items.length} sticky notes. Consider consolidating for clarity.
        </p>
      )}

      {/* Add Button / Composer */}
      {showComposer ? (
        <StickyNoteComposer
          onSave={(data) => { onAdd(data); setShowComposer(false); }}
          onCancel={() => setShowComposer(false)}
        />
      ) : (
        <button
          type="button"
          onClick={() => setShowComposer(true)}
          data-testid="add-sticky-note-button"
          style={{
            padding: '10px 20px',
            backgroundColor: 'var(--color-primary)',
            color: '#0D0F14',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            fontWeight: 600,
            cursor: 'pointer',
            transition: 'all 0.2s',
          }}
        >
          Add sticky note
        </button>
      )}

      {/* Undo Toast */}
      {deletedItem && (
        <div
          data-testid="undo-toast"
          role="alert"
          style={{
            position: 'fixed',
            bottom: '24px',
            left: '50%',
            transform: 'translateX(-50%)',
            padding: '12px 20px',
            backgroundColor: 'var(--color-bg-elevated)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            boxShadow: '0 4px 16px rgba(0, 0, 0, 0.4)',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            zIndex: 9999,
          }}
        >
          <span style={{ color: 'var(--color-text-primary)', fontSize: 'var(--text-body)' }}>
            Sticky note deleted
          </span>
          <button
            type="button"
            onClick={handleUndo}
            data-testid="undo-button"
            style={{
              padding: '6px 14px',
              backgroundColor: 'var(--color-primary)',
              color: '#0D0F14',
              border: 'none',
              borderRadius: 'var(--radius-small)',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Undo
          </button>
        </div>
      )}
    </div>
  );
}

// --- Sub-components ---

interface StickyNoteCardProps {
  item: PinnedRememberItem;
  index: number;
  totalItems: number;
  draggedIndex: number | null;
  dragOverIndex: number | null;
  onEdit: () => void;
  onDelete: () => void;
  onDragStart: (index: number) => void;
  onDragEnd: () => void;
  onDragOver: (e: React.DragEvent, index: number) => void;
  onDrop: (e: React.DragEvent, index: number) => void;
  onMoveItem: (index: number, direction: 'up' | 'down') => void;
}

function StickyNoteCard({
  item, index, totalItems, draggedIndex, dragOverIndex,
  onEdit, onDelete, onDragStart, onDragEnd, onDragOver, onDrop, onMoveItem
}: StickyNoteCardProps) {
  const colorStyles = getColorStyles(item.color);
  const isSensitiveHidden = item.sensitive;
  const [revealed, setRevealed] = useState(false);

  const truncatedText = item.text.length > 100 ? item.text.slice(0, 100) + '…' : item.text;

  return (
    <div
      data-testid="sticky-note-card"
      draggable
      onDragStart={() => onDragStart(index)}
      onDragEnd={onDragEnd}
      onDragOver={(e) => onDragOver(e, index)}
      onDrop={(e) => onDrop(e, index)}
      style={{
        width: '200px',
        minHeight: '100px',
        padding: '12px',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: colorStyles.bg,
        border: dragOverIndex === index
          ? `2px dashed var(--color-primary)`
          : `1px solid ${colorStyles.border}`,
        opacity: draggedIndex === index ? 0.6 : 1,
        cursor: 'grab',
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
        transition: 'all 0.15s ease',
        position: 'relative',
        boxShadow: draggedIndex === index
          ? '0 4px 12px rgba(0,0,0,0.3)'
          : '0 2px 6px rgba(0,0,0,0.15)',
      }}
    >
      {/* Header row: tag + drag handle */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        {item.tag && (
          <span style={{
            fontSize: '10px',
            fontFamily: 'var(--font-mono)',
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
            color: 'var(--color-text-secondary)',
          }}>
            {item.tag}
          </span>
        )}
        {!item.tag && <span />}
        <button
          type="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === 'ArrowUp' && index > 0) { e.preventDefault(); onMoveItem(index, 'up'); }
            else if (e.key === 'ArrowDown' && index < totalItems - 1) { e.preventDefault(); onMoveItem(index, 'down'); }
          }}
          aria-label={`Reorder sticky note. Use arrow keys to move.`}
          style={{
            background: 'transparent', border: 'none', padding: '2px',
            color: 'var(--color-text-muted)', cursor: 'grab', display: 'flex',
          }}
        >
          <GripVertical size={14} />
        </button>
      </div>

      {/* Content */}
      <div style={{ flex: 1, fontSize: 'var(--text-body)', color: 'var(--color-text-primary)', lineHeight: 1.4 }}>
        {isSensitiveHidden && !revealed ? (
          <span style={{ fontStyle: 'italic', color: 'var(--color-text-muted)' }}>
            Sensitive note — view to reveal
          </span>
        ) : (
          <span>{truncatedText}</span>
        )}
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', gap: '4px', justifyContent: 'flex-end' }}>
        {isSensitiveHidden && (
          <button
            type="button"
            onClick={() => setRevealed(!revealed)}
            aria-label={revealed ? 'Hide sensitive content' : 'Reveal sensitive content'}
            style={{
              background: 'transparent', border: 'none', padding: '4px',
              color: 'var(--color-text-muted)', cursor: 'pointer',
            }}
          >
            {revealed ? <EyeOff size={14} /> : <Eye size={14} />}
          </button>
        )}
        <button
          type="button"
          onClick={onEdit}
          aria-label="Edit sticky note"
          data-testid="edit-sticky-note"
          style={{
            background: 'transparent', border: 'none', padding: '4px',
            color: 'var(--color-text-muted)', cursor: 'pointer',
          }}
        >
          <Pencil size={14} />
        </button>
        <button
          type="button"
          onClick={onDelete}
          aria-label="Delete sticky note"
          data-testid="delete-sticky-note"
          style={{
            background: 'transparent', border: 'none', padding: '4px',
            color: 'var(--color-text-muted)', cursor: 'pointer',
          }}
        >
          <Trash2 size={14} />
        </button>
      </div>
    </div>
  );
}

interface StickyNoteComposerProps {
  onSave: (data: { text: string; color?: string; tag?: string; sensitive?: boolean }) => void;
  onCancel: () => void;
}

function StickyNoteComposer({ onSave, onCancel }: StickyNoteComposerProps) {
  const [text, setText] = useState('');
  const [color, setColor] = useState<StickyNoteColor>('cyan');
  const [tag, setTag] = useState('');
  const [sensitive, setSensitive] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => { textareaRef.current?.focus(); }, []);

  const handleSave = () => {
    if (!text.trim()) return;
    onSave({ text: text.trim(), color, tag: tag.trim() || undefined, sensitive });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSave(); }
    if (e.key === 'Escape') { e.preventDefault(); onCancel(); }
  };

  return (
    <div
      data-testid="sticky-note-composer"
      style={{
        padding: '16px',
        border: '1px dashed var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
      }}
    >
      <textarea
        ref={textareaRef}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Write a sticky note..."
        aria-label="Sticky note text"
        rows={3}
        style={{
          width: '100%',
          padding: '10px 14px',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
          fontSize: 'var(--text-body)',
          backgroundColor: 'var(--color-bg-elevated)',
          color: 'var(--color-text-primary)',
          resize: 'vertical',
          fontFamily: 'inherit',
          outline: 'none',
        }}
      />

      {/* Starter templates */}
      <div style={{ margin: '8px 0', display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
        {STARTER_TEMPLATES.map((tpl) => (
          <button
            key={tpl.text}
            type="button"
            onClick={() => { setText(tpl.text); setTag(tpl.tag); }}
            style={{
              padding: '4px 8px',
              fontSize: '11px',
              fontFamily: 'var(--font-mono)',
              backgroundColor: 'var(--color-bg-elevated)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-small)',
              color: 'var(--color-text-secondary)',
              cursor: 'pointer',
            }}
          >
            {tpl.tag}
          </button>
        ))}
      </div>

      {/* Color picker */}
      <div style={{ display: 'flex', gap: '6px', margin: '8px 0', alignItems: 'center' }}>
        <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', marginRight: '4px' }}>Color:</span>
        {STICKY_COLORS.map((c) => (
          <button
            key={c.value}
            type="button"
            onClick={() => setColor(c.value)}
            aria-label={`Color ${c.label}`}
            aria-pressed={color === c.value}
            style={{
              width: '20px',
              height: '20px',
              borderRadius: 'var(--radius-full)',
              backgroundColor: c.bg,
              border: color === c.value ? `2px solid ${c.border}` : '1px solid var(--color-border)',
              cursor: 'pointer',
              boxShadow: color === c.value ? `0 0 6px ${c.border}` : 'none',
            }}
          />
        ))}
      </div>

      {/* Tag + Sensitive */}
      <div style={{ display: 'flex', gap: '8px', alignItems: 'center', margin: '8px 0' }}>
        <input
          type="text"
          value={tag}
          onChange={(e) => setTag(e.target.value)}
          placeholder="Tag (e.g., Family, Docs, Board)"
          aria-label="Sticky note tag"
          maxLength={30}
          style={{
            flex: 1,
            padding: '8px 12px',
            fontSize: 'var(--text-caption)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            outline: 'none',
          }}
        />
        <label style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: 'var(--text-caption)', color: 'var(--color-text-secondary)', cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={sensitive}
            onChange={(e) => setSensitive(e.target.checked)}
            aria-label="Mark as sensitive (hide in previews)"
          />
          Sensitive
        </label>
      </div>

      {/* Save / Cancel */}
      <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
        <button
          type="button"
          onClick={onCancel}
          style={{
            padding: '8px 16px',
            backgroundColor: 'transparent',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-caption)',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
          }}
        >
          Cancel
        </button>
        <button
          type="button"
          onClick={handleSave}
          disabled={!text.trim()}
          data-testid="save-sticky-note"
          style={{
            padding: '8px 16px',
            backgroundColor: text.trim() ? 'var(--color-primary)' : 'var(--color-bg-elevated)',
            color: text.trim() ? '#0D0F14' : 'var(--color-text-muted)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            fontWeight: 600,
            cursor: text.trim() ? 'pointer' : 'not-allowed',
          }}
        >
          Save
        </button>
      </div>
      <p style={{ margin: '8px 0 0', fontSize: '11px', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>
        Ctrl+Enter to save · Esc to cancel
      </p>
    </div>
  );
}

interface StickyNoteEditorProps {
  item: PinnedRememberItem;
  onSave: (data: { text: string; color?: string; tag?: string; sensitive?: boolean }) => void;
  onCancel: () => void;
}

function StickyNoteEditor({ item, onSave, onCancel }: StickyNoteEditorProps) {
  const [text, setText] = useState(item.text);
  const [color, setColor] = useState<StickyNoteColor>(item.color);
  const [tag, setTag] = useState(item.tag || '');
  const [sensitive, setSensitive] = useState(item.sensitive);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => { textareaRef.current?.focus(); }, []);

  const handleSave = () => {
    if (!text.trim()) return;
    onSave({ text: text.trim(), color, tag: tag.trim() || undefined, sensitive });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSave(); }
    if (e.key === 'Escape') { e.preventDefault(); onCancel(); }
  };

  const colorStyles = getColorStyles(color);

  return (
    <div
      data-testid="sticky-note-editor"
      style={{
        width: '200px',
        padding: '12px',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: colorStyles.bg,
        border: `1px solid ${colorStyles.border}`,
      }}
    >
      <textarea
        ref={textareaRef}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        rows={3}
        aria-label="Edit sticky note text"
        style={{
          width: '100%',
          padding: '8px',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-small)',
          fontSize: 'var(--text-body)',
          backgroundColor: 'var(--color-bg-elevated)',
          color: 'var(--color-text-primary)',
          resize: 'vertical',
          fontFamily: 'inherit',
          outline: 'none',
        }}
      />
      <div style={{ display: 'flex', gap: '4px', margin: '6px 0', flexWrap: 'wrap' }}>
        {STICKY_COLORS.map((c) => (
          <button
            key={c.value}
            type="button"
            onClick={() => setColor(c.value)}
            aria-label={`Color ${c.label}`}
            aria-pressed={color === c.value}
            style={{
              width: '16px', height: '16px', borderRadius: 'var(--radius-full)',
              backgroundColor: c.bg,
              border: color === c.value ? `2px solid ${c.border}` : '1px solid var(--color-border)',
              cursor: 'pointer',
            }}
          />
        ))}
      </div>
      <input
        type="text"
        value={tag}
        onChange={(e) => setTag(e.target.value)}
        placeholder="Tag"
        maxLength={30}
        aria-label="Sticky note tag"
        style={{
          width: '100%',
          padding: '6px 8px',
          fontSize: '11px',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-small)',
          backgroundColor: 'var(--color-bg-elevated)',
          color: 'var(--color-text-primary)',
          outline: 'none',
          marginBottom: '6px',
        }}
      />
      <label style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', color: 'var(--color-text-secondary)', cursor: 'pointer', marginBottom: '8px' }}>
        <input type="checkbox" checked={sensitive} onChange={(e) => setSensitive(e.target.checked)} />
        Sensitive
      </label>
      <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
        <button type="button" onClick={onCancel} aria-label="Cancel edit" style={{ background: 'transparent', border: 'none', padding: '4px', color: 'var(--color-text-muted)', cursor: 'pointer' }}>
          <X size={14} />
        </button>
        <button
          type="button"
          onClick={handleSave}
          disabled={!text.trim()}
          data-testid="save-edit-sticky-note"
          style={{
            padding: '4px 10px',
            backgroundColor: text.trim() ? 'var(--color-primary)' : 'var(--color-bg-elevated)',
            color: text.trim() ? '#0D0F14' : 'var(--color-text-muted)',
            border: 'none', borderRadius: 'var(--radius-small)',
            fontSize: '11px', fontFamily: 'var(--font-mono)', fontWeight: 600, cursor: 'pointer',
          }}
        >
          Save
        </button>
      </div>
    </div>
  );
}
