'use client';

import { useState } from 'react';

export interface AgendaItemInput {
  id: string;
  text: string;
  checked: boolean;
}

interface AgendaItemListProps {
  items: AgendaItemInput[];
  onChange: (items: AgendaItemInput[]) => void;
}

/**
 * Ordered list of agenda items with checkboxes.
 * Supports adding new items, removing items, and toggling checked state.
 * Validates non-blank text before adding.
 */
export default function AgendaItemList({ items, onChange }: AgendaItemListProps) {
  const [newItemText, setNewItemText] = useState('');
  const [error, setError] = useState<string | null>(null);

  function handleAdd() {
    const trimmed = newItemText.trim();
    if (!trimmed) {
      setError('Agenda item text cannot be blank');
      return;
    }
    setError(null);
    const newItem: AgendaItemInput = {
      id: crypto.randomUUID(),
      text: trimmed,
      checked: false,
    };
    onChange([...items, newItem]);
    setNewItemText('');
  }

  function handleRemove(id: string) {
    onChange(items.filter((item) => item.id !== id));
  }

  function handleToggle(id: string) {
    onChange(
      items.map((item) =>
        item.id === id ? { ...item, checked: !item.checked } : item
      )
    );
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAdd();
    }
  }

  return (
    <div data-testid="agenda-item-list">
      <label
        style={{
          display: 'block',
          fontSize: 'var(--text-caption)',
          fontWeight: 'var(--weight-medium)',
          fontFamily: 'var(--font-mono)',
          marginBottom: '8px',
          color: 'var(--color-text-secondary)',
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
        }}
      >
        Agenda Items
      </label>

      {/* Existing items */}
      {items.length > 0 && (
        <ol
          data-testid="agenda-items"
          style={{ listStyle: 'none', padding: 0, margin: '0 0 12px 0' }}
          aria-label="Agenda items"
        >
          {items.map((item, index) => (
            <li
              key={item.id}
              data-testid={`agenda-item-${index}`}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '8px',
                borderBottom: '1px solid var(--color-border-subtle)',
              }}
            >
              <input
                type="checkbox"
                checked={item.checked}
                onChange={() => handleToggle(item.id)}
                aria-label={`Mark "${item.text}" as ${item.checked ? 'unchecked' : 'checked'}`}
                data-testid={`agenda-item-checkbox-${index}`}
                style={{ width: '16px', height: '16px', cursor: 'pointer' }}
              />
              <span
                style={{
                  flex: 1,
                  fontSize: 'var(--text-body)',
                  textDecoration: item.checked ? 'line-through' : 'none',
                  color: item.checked ? 'var(--color-text-muted)' : 'var(--color-text-primary)',
                }}
              >
                {item.text}
              </span>
              <button
                type="button"
                onClick={() => handleRemove(item.id)}
                aria-label={`Remove "${item.text}"`}
                data-testid={`agenda-item-remove-${index}`}
                style={{
                  padding: '4px 8px',
                  border: 'none',
                  background: 'none',
                  color: 'var(--color-alert)',
                  cursor: 'pointer',
                  fontSize: 'var(--text-body)',
                  borderRadius: 'var(--radius-small)',
                }}
              >
                ✕
              </button>
            </li>
          ))}
        </ol>
      )}

      {/* Add new item */}
      <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-start' }}>
        <div style={{ flex: 1 }}>
          <input
            type="text"
            value={newItemText}
            onChange={(e) => {
              setNewItemText(e.target.value);
              if (error) setError(null);
            }}
            onKeyDown={handleKeyDown}
            placeholder="Add agenda item..."
            aria-label="New agenda item text"
            data-testid="agenda-item-input"
            style={{
              width: '100%',
              padding: '8px 12px',
              border: `1px solid ${error ? 'var(--color-alert)' : 'var(--color-border)'}`,
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              boxSizing: 'border-box',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              transition: 'border-color 0.2s, box-shadow 0.2s',
            }}
          />
          {error && (
            <p
              data-testid="agenda-item-error"
              role="alert"
              style={{ margin: '4px 0 0', fontSize: 'var(--text-caption)', color: 'var(--color-alert)' }}
            >
              {error}
            </p>
          )}
        </div>
        <button
          type="button"
          onClick={handleAdd}
          data-testid="agenda-item-add-button"
          style={{
            padding: '8px 16px',
            backgroundColor: 'var(--color-secondary)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-medium)',
            fontFamily: 'var(--font-mono)',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
            boxShadow: '0 0 8px rgba(168, 85, 247, 0.2)',
            transition: 'box-shadow 0.2s',
          }}
        >
          Add
        </button>
      </div>
    </div>
  );
}
