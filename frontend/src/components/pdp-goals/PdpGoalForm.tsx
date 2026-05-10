'use client';

import { useState } from 'react';
import { CreatePdpGoalRequest, UpdatePdpGoalRequest, PdpGoal } from '@/types/pdp-goal';

interface PdpGoalFormProps {
  /** If provided, the form is in edit mode */
  existingGoal?: PdpGoal;
  onSubmit: (data: CreatePdpGoalRequest | UpdatePdpGoalRequest) => void;
  onCancel: () => void;
}

/**
 * Form for creating or editing a PDP goal.
 * Supports title, description, and target date fields.
 */
export default function PdpGoalForm({ existingGoal, onSubmit, onCancel }: PdpGoalFormProps) {
  const [title, setTitle] = useState(existingGoal?.title || '');
  const [description, setDescription] = useState(existingGoal?.description || '');
  const [targetDate, setTargetDate] = useState(existingGoal?.targetDate || '');

  const isEditing = !!existingGoal;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    if (isEditing) {
      const data: UpdatePdpGoalRequest = {};
      if (title !== existingGoal.title) data.title = title;
      if (description !== (existingGoal.description || '')) data.description = description || null;
      if (targetDate !== (existingGoal.targetDate || '')) data.targetDate = targetDate || null;
      onSubmit(data);
    } else {
      const data: CreatePdpGoalRequest = {
        title: title.trim(),
        description: description.trim() || null,
        targetDate: targetDate || null,
      };
      onSubmit(data);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      data-testid="pdp-goal-form"
      style={{
        padding: 'var(--space-4)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
      }}
    >
      <div style={{ marginBottom: '12px' }}>
        <label
          htmlFor="pdp-goal-title"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Title *
        </label>
        <input
          id="pdp-goal-title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="e.g., Improve public speaking"
          required
          data-testid="pdp-goal-title-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            outline: 'none',
            boxSizing: 'border-box',
          }}
        />
      </div>

      <div style={{ marginBottom: '12px' }}>
        <label
          htmlFor="pdp-goal-description"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Description
        </label>
        <textarea
          id="pdp-goal-description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Describe the goal and success criteria..."
          rows={3}
          data-testid="pdp-goal-description-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            outline: 'none',
            resize: 'vertical',
            boxSizing: 'border-box',
            fontFamily: 'inherit',
          }}
        />
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label
          htmlFor="pdp-goal-target-date"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Target Date
        </label>
        <input
          id="pdp-goal-target-date"
          type="date"
          value={targetDate}
          onChange={(e) => setTargetDate(e.target.value)}
          data-testid="pdp-goal-target-date-input"
          style={{
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            outline: 'none',
          }}
        />
      </div>

      <div style={{ display: 'flex', gap: '8px' }}>
        <button
          type="submit"
          data-testid="pdp-goal-submit-btn"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-primary)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary-muted)',
            color: 'var(--color-primary)',
            cursor: 'pointer',
            fontWeight: 'var(--weight-medium)',
          }}
        >
          {isEditing ? 'Update Goal' : 'Create Goal'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          data-testid="pdp-goal-cancel-btn"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-muted)',
            cursor: 'pointer',
          }}
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
