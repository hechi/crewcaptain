'use client';

import { useState } from 'react';
import { CreateQuickNoteRequest } from '@/types/quick-note';

interface QuickNoteFormProps {
  onSubmit: (data: CreateQuickNoteRequest) => void;
  isSubmitting?: boolean;
}

/**
 * Quick capture form for creating new quick notes.
 * Minimal friction — just text + optional sensitive toggle.
 */
export default function QuickNoteForm({ onSubmit, isSubmitting = false }: QuickNoteFormProps) {
  const [text, setText] = useState('');
  const [sensitive, setSensitive] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;

    onSubmit({
      text: text.trim(),
      sensitive: sensitive || undefined,
    });

    setText('');
    setSensitive(false);
  };

  return (
    <form
      onSubmit={handleSubmit}
      data-testid="quick-note-form"
      style={{
        padding: 'var(--space-4)',
        border: '1px solid var(--color-border-glow)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
      }}
    >
      <div style={{ marginBottom: '12px' }}>
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Quick capture — what's on your mind?"
          data-testid="quick-note-text-input"
          aria-label="Quick note text"
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
          }}
        />
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <label
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
          }}
        >
          <input
            type="checkbox"
            checked={sensitive}
            onChange={(e) => setSensitive(e.target.checked)}
            data-testid="quick-note-sensitive-checkbox"
            aria-label="Mark as sensitive"
          />
          Sensitive
        </label>

        <button
          type="submit"
          disabled={!text.trim() || isSubmitting}
          data-testid="quick-note-submit-btn"
          style={{
            padding: '8px 16px',
            backgroundColor: text.trim() && !isSubmitting ? 'var(--color-primary)' : 'var(--color-bg-elevated)',
            color: text.trim() && !isSubmitting ? 'var(--color-bg-base)' : 'var(--color-text-muted)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: '600',
            fontFamily: 'var(--font-mono)',
            cursor: text.trim() && !isSubmitting ? 'pointer' : 'not-allowed',
            boxShadow: text.trim() && !isSubmitting ? 'var(--glow-primary)' : 'none',
            transition: 'all 0.2s',
          }}
        >
          {isSubmitting ? 'Saving...' : 'Capture'}
        </button>
      </div>
    </form>
  );
}
