'use client';

import { useState } from 'react';
import { CreateKudosRequest } from '@/types/kudos';

interface KudosFormProps {
  onSubmit: (data: CreateKudosRequest) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

/**
 * Form for creating a new kudos entry with date, text, and optional tags.
 */
export default function KudosForm({ onSubmit, onCancel, isSubmitting = false }: KudosFormProps) {
  const [text, setText] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [tagsInput, setTagsInput] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;

    const tags = tagsInput
      .split(',')
      .map((t) => t.trim())
      .filter((t) => t.length > 0);

    onSubmit({
      text: text.trim(),
      date,
      tags: tags.length > 0 ? tags : undefined,
    });
  };

  return (
    <form
      onSubmit={handleSubmit}
      data-testid="kudos-form"
      style={{
        padding: 'var(--space-4)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
      }}
    >
      {/* Date */}
      <div style={{ marginBottom: '12px' }}>
        <label
          htmlFor="kudos-date"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
          }}
        >
          Date
        </label>
        <input
          id="kudos-date"
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          data-testid="kudos-date-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        />
      </div>

      {/* Text */}
      <div style={{ marginBottom: '12px' }}>
        <label
          htmlFor="kudos-text"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
          }}
        >
          Recognition *
        </label>
        <textarea
          id="kudos-text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="What did they do well? Be specific..."
          required
          rows={3}
          data-testid="kudos-text-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            resize: 'vertical',
            fontFamily: 'inherit',
          }}
        />
      </div>

      {/* Tags */}
      <div style={{ marginBottom: '16px' }}>
        <label
          htmlFor="kudos-tags"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
          }}
        >
          Tags (comma-separated, optional)
        </label>
        <input
          id="kudos-tags"
          type="text"
          value={tagsInput}
          onChange={(e) => setTagsInput(e.target.value)}
          placeholder="e.g. impact, collaboration, leadership"
          data-testid="kudos-tags-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        />
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', gap: '8px' }}>
        <button
          type="submit"
          disabled={isSubmitting || !text.trim()}
          data-testid="kudos-submit-btn"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            cursor: isSubmitting || !text.trim() ? 'not-allowed' : 'pointer',
            fontWeight: 'var(--weight-medium)',
            opacity: isSubmitting || !text.trim() ? 0.6 : 1,
          }}
        >
          {isSubmitting ? 'Saving...' : 'Give Kudos'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          data-testid="kudos-cancel-btn"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'transparent',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
          }}
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
