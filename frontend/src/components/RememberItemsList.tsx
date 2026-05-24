'use client';

import { useState, useCallback } from 'react';
import { GripVertical, Trash2 } from 'lucide-react';
import { PinnedRememberItem } from '@/types/person';

interface RememberItemsListProps {
  items: PinnedRememberItem[];
  onAdd: (text: string) => void;
  onRemove: (itemId: string) => void;
  onReorder: (orderedIds: string[]) => void;
}

export default function RememberItemsList({ items, onAdd, onRemove, onReorder }: RememberItemsListProps) {
  const [newItemText, setNewItemText] = useState('');
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);

  const handleAdd = () => {
    const trimmed = newItemText.trim();
    if (trimmed) {
      onAdd(trimmed);
      setNewItemText('');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAdd();
    }
  };

  // Move item one position up or down (for keyboard users)
  const moveItem = (index: number, direction: 'up' | 'down') => {
    const newItems = [...items];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= newItems.length) return;

    [newItems[index], newItems[targetIndex]] = [newItems[targetIndex], newItems[index]];
    onReorder(newItems.map((item) => item.id));
  };

  // Drag and drop handlers
  const handleDragStart = useCallback((index: number) => {
    setDraggedIndex(index);
  }, []);

  const handleDragEnd = useCallback(() => {
    setDraggedIndex(null);
    setDragOverIndex(null);
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === index) return;
    setDragOverIndex(index);
  }, [draggedIndex]);

  const handleDrop = useCallback((e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === dropIndex) {
      setDraggedIndex(null);
      setDragOverIndex(null);
      return;
    }

    const newItems = [...items];
    const [removed] = newItems.splice(draggedIndex, 1);
    newItems.splice(dropIndex, 0, removed);
    onReorder(newItems.map((item) => item.id));
    setDraggedIndex(null);
    setDragOverIndex(null);
  }, [draggedIndex, items, onReorder]);

  return (
    <div data-testid="remember-items-list">
      <h4 style={{
        margin: '0 0 12px',
        fontSize: 'var(--text-caption)',
        fontWeight: 'var(--weight-semibold)',
        fontFamily: 'var(--font-mono)',
        color: 'var(--color-primary)',
        letterSpacing: '0.5px',
        textTransform: 'uppercase',
      }}>
        Pinned Remember Items
      </h4>

      {items.length === 0 && (
        <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--text-body)', fontStyle: 'italic' }}>
          No pinned items yet.
        </p>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }}>
        {items.map((item, index) => (
          <div
            key={item.id}
            data-testid="remember-item"
            draggable
            onDragStart={() => handleDragStart(index)}
            onDragEnd={handleDragEnd}
            onDragOver={(e) => handleDragOver(e, index)}
            onDrop={(e) => handleDrop(e, index)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '12px 16px',
              border: dragOverIndex === index
                ? '2px dashed var(--color-primary)'
                : draggedIndex === index
                  ? '1px solid var(--color-primary)'
                  : '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: draggedIndex === index
                ? 'var(--color-primary-muted)'
                : 'var(--color-bg-surface)',
              cursor: 'grab',
              opacity: draggedIndex === index ? 0.7 : 1,
              transition: 'all 0.15s ease',
              boxShadow: draggedIndex === index ? '0 4px 12px rgba(0, 0, 0, 0.3)' : 'none',
            }}
          >
            {/* Drag Handle */}
            <button
              type="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'ArrowUp' && index > 0) {
                  e.preventDefault();
                  moveItem(index, 'up');
                } else if (e.key === 'ArrowDown' && index < items.length - 1) {
                  e.preventDefault();
                  moveItem(index, 'down');
                }
              }}
              aria-label={`Reorder "${item.text}". Use arrow up and down keys to move.`}
              aria-grabbed={draggedIndex === index}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '4px',
                background: 'transparent',
                border: 'none',
                color: 'var(--color-text-muted)',
                cursor: 'grab',
                transition: 'color 0.2s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = 'var(--color-primary)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = 'var(--color-text-muted)';
              }}
            >
              <GripVertical size={18} />
            </button>

            {/* Note Content */}
            <span
              style={{
                flex: 1,
                fontSize: 'var(--text-body)',
                color: 'var(--color-text-primary)',
                lineHeight: '1.5',
              }}
            >
              {item.text}
            </span>

            {/* Delete Button */}
            <button
              type="button"
              onClick={() => onRemove(item.id)}
              aria-label={`Remove "${item.text}"`}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '6px',
                background: 'transparent',
                border: 'none',
                borderRadius: 'var(--radius-small)',
                color: 'var(--color-text-muted)',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = 'var(--color-alert)';
                e.currentTarget.style.backgroundColor = 'var(--color-alert-muted)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = 'var(--color-text-muted)';
                e.currentTarget.style.backgroundColor = 'transparent';
              }}
            >
              <Trash2 size={16} />
            </button>
          </div>
        ))}
      </div>

      {/* Add New Item Form */}
      <div style={{
        padding: '16px',
        border: '1px dashed var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
      }}>
        <div style={{
          display: 'flex',
          gap: '8px',
          alignItems: 'flex-start',
        }}>
          <input
            type="text"
            value={newItemText}
            onChange={(e) => setNewItemText(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Add a remember item..."
            aria-label="New remember item"
            style={{
              flex: 1,
              padding: '10px 14px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              transition: 'border-color 0.2s, box-shadow 0.2s',
              outline: 'none',
            }}
            onFocus={(e) => {
              e.currentTarget.style.borderColor = 'var(--color-primary)';
              e.currentTarget.style.boxShadow = '0 0 0 2px var(--color-primary-muted)';
            }}
            onBlur={(e) => {
              e.currentTarget.style.borderColor = 'var(--color-border)';
              e.currentTarget.style.boxShadow = 'none';
            }}
          />
          <button
            type="button"
            onClick={handleAdd}
            disabled={!newItemText.trim()}
            style={{
              padding: '10px 20px',
              backgroundColor: newItemText.trim() ? 'var(--color-primary)' : 'var(--color-bg-elevated)',
              color: newItemText.trim() ? '#0D0F14' : 'var(--color-text-muted)',
              border: 'none',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              fontWeight: 600,
              cursor: newItemText.trim() ? 'pointer' : 'not-allowed',
              transition: 'all 0.2s',
              whiteSpace: 'nowrap',
            }}
            onMouseEnter={(e) => {
              if (newItemText.trim()) {
                e.currentTarget.style.backgroundColor = 'var(--color-primary-hover)';
                e.currentTarget.style.boxShadow = '0 0 8px rgba(0, 240, 255, 0.3)';
              }
            }}
            onMouseLeave={(e) => {
              if (newItemText.trim()) {
                e.currentTarget.style.backgroundColor = 'var(--color-primary)';
                e.currentTarget.style.boxShadow = 'none';
              }
            }}
          >
            Add
          </button>
        </div>
        <p style={{
          margin: '8px 0 0',
          fontSize: 'var(--text-caption)',
          color: 'var(--color-text-muted)',
          fontFamily: 'var(--font-mono)',
        }}>
          Drag to reorder or use Tab + arrow keys
        </p>
      </div>
    </div>
  );
}
