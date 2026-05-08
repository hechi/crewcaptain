'use client';

import { useState } from 'react';
import { PinnedRememberItem } from '@/types/person';

interface RememberItemsListProps {
  items: PinnedRememberItem[];
  onAdd: (text: string) => void;
  onRemove: (itemId: string) => void;
  onReorder: (orderedIds: string[]) => void;
}

export default function RememberItemsList({ items, onAdd, onRemove, onReorder }: RememberItemsListProps) {
  const [newItemText, setNewItemText] = useState('');

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

  const moveItem = (index: number, direction: 'up' | 'down') => {
    const newItems = [...items];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= newItems.length) return;

    [newItems[index], newItems[targetIndex]] = [newItems[targetIndex], newItems[index]];
    onReorder(newItems.map((item) => item.id));
  };

  return (
    <div data-testid="remember-items-list">
      <h4 style={{ margin: '0 0 12px', fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)', color: 'var(--color-primary)' }}>Pinned Remember Items</h4>

      {items.length === 0 && (
        <p style={{ color: 'var(--color-neutral-text-muted)', fontSize: 'var(--text-body)', fontStyle: 'italic' }}>
          No pinned items yet.
        </p>
      )}

      <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 12px' }}>
        {items.map((item, index) => (
          <li
            key={item.id}
            data-testid="remember-item"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px',
              borderBottom: '1px solid var(--color-neutral-border-light)',
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
              <button
                type="button"
                onClick={() => moveItem(index, 'up')}
                disabled={index === 0}
                aria-label={`Move "${item.text}" up`}
                style={{
                  padding: '2px 6px',
                  fontSize: '10px',
                  border: '1px solid var(--color-neutral-border)',
                  borderRadius: 'var(--radius-small)',
                  cursor: index === 0 ? 'not-allowed' : 'pointer',
                  opacity: index === 0 ? 0.5 : 1,
                  background: 'var(--color-neutral-surface)',
                }}
              >
                ▲
              </button>
              <button
                type="button"
                onClick={() => moveItem(index, 'down')}
                disabled={index === items.length - 1}
                aria-label={`Move "${item.text}" down`}
                style={{
                  padding: '2px 6px',
                  fontSize: '10px',
                  border: '1px solid var(--color-neutral-border)',
                  borderRadius: 'var(--radius-small)',
                  cursor: index === items.length - 1 ? 'not-allowed' : 'pointer',
                  opacity: index === items.length - 1 ? 0.5 : 1,
                  background: 'var(--color-neutral-surface)',
                }}
              >
                ▼
              </button>
            </div>
            <span style={{ flex: 1, fontSize: 'var(--text-body)' }}>{item.text}</span>
            <button
              type="button"
              onClick={() => onRemove(item.id)}
              aria-label={`Remove "${item.text}"`}
              style={{
                padding: '4px 8px',
                fontSize: 'var(--text-caption)',
                color: 'var(--color-error)',
                border: '1px solid var(--color-error-border)',
                borderRadius: 'var(--radius-small)',
                cursor: 'pointer',
                background: 'var(--color-neutral-surface)',
              }}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>

      <div style={{ display: 'flex', gap: '8px' }}>
        <input
          type="text"
          value={newItemText}
          onChange={(e) => setNewItemText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Add a remember item..."
          aria-label="New remember item"
          style={{
            flex: 1,
            padding: '8px 12px',
            border: '1px solid var(--color-neutral-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            backgroundColor: 'var(--color-neutral-surface)',
          }}
        />
        <button
          type="button"
          onClick={handleAdd}
          style={{
            padding: '8px 16px',
            backgroundColor: 'var(--color-secondary)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            cursor: 'pointer',
          }}
        >
          Add
        </button>
      </div>
    </div>
  );
}
