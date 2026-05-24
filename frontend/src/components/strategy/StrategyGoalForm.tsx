'use client';

import { useState } from 'react';
import { CreateStrategyGoalRequest, UpdateStrategyGoalRequest } from '@/types/strategy-goal';

interface StrategyGoalFormProps {
  initialData?: {
    title: string;
    description: string | null;
    targetDate: string | null;
    sensitive: boolean;
  };
  onSubmit: (data: CreateStrategyGoalRequest | UpdateStrategyGoalRequest) => void;
  onCancel: () => void;
  submitLabel: string;
}

export default function StrategyGoalForm({
  initialData,
  onSubmit,
  onCancel,
  submitLabel,
}: StrategyGoalFormProps) {
  const [title, setTitle] = useState(initialData?.title ?? '');
  const [description, setDescription] = useState(initialData?.description ?? '');
  const [targetDate, setTargetDate] = useState(initialData?.targetDate ?? '');
  const [sensitive, setSensitive] = useState(initialData?.sensitive ?? false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim()) {
      setError('Title is required');
      return;
    }

    if (title.length > 500) {
      setError('Title must not exceed 500 characters');
      return;
    }

    if (description && description.length > 5000) {
      setError('Description must not exceed 5000 characters');
      return;
    }

    onSubmit({
      title: title.trim(),
      description: description || null,
      targetDate: targetDate || null,
      sensitive,
    });
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {error && (
        <div
          style={{
            padding: '8px 12px',
            backgroundColor: 'var(--color-alert-muted)',
            border: '1px solid var(--color-alert)',
            borderRadius: 'var(--radius-medium)',
            color: 'var(--color-alert)',
            fontSize: 'var(--text-small)',
          }}
        >
          {error}
        </div>
      )}

      <div>
        <label
          htmlFor="title"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-secondary)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Title *
        </label>
        <input
          id="title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="e.g., Modernize Tech Stack"
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-ui)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        />
      </div>

      <div>
        <label
          htmlFor="description"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-secondary)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Description
        </label>
        <textarea
          id="description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Describe the strategy goal..."
          rows={4}
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-ui)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            resize: 'vertical',
          }}
        />
      </div>

      <div>
        <label
          htmlFor="targetDate"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-secondary)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Target Date
        </label>
        <input
          id="targetDate"
          type="date"
          value={targetDate}
          onChange={(e) => setTargetDate(e.target.value)}
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-ui)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        />
      </div>

      <div>
        <label
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: 'var(--text-small)',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
          }}
        >
          <input
            type="checkbox"
            checked={sensitive}
            onChange={(e) => setSensitive(e.target.checked)}
            style={{ cursor: 'pointer' }}
          />
          <span>Mark as sensitive (content will be encrypted)</span>
        </label>
      </div>

      <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
        <button
          type="submit"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            fontWeight: 'var(--weight-semibold)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            cursor: 'pointer',
          }}
        >
          {submitLabel}
        </button>
        <button
          type="button"
          onClick={onCancel}
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
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
