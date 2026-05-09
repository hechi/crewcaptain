'use client';

import { useState } from 'react';
import { ActionItemOwnerType, CreateActionItemRequest, UpdateActionItemRequest } from '@/types/action-item';

interface ActionItemFormProps {
  mode: 'create' | 'edit';
  initialData?: {
    title?: string;
    description?: string | null;
    ownerType?: ActionItemOwnerType;
    dueDate?: string | null;
  };
  onSubmit: (data: CreateActionItemRequest | UpdateActionItemRequest) => void;
  onCancel?: () => void;
  isSubmitting?: boolean;
}

/**
 * Form for creating or editing an action item.
 * Supports title, description, owner type, and due date fields.
 */
export default function ActionItemForm({ mode, initialData, onSubmit, onCancel, isSubmitting = false }: ActionItemFormProps) {
  const [title, setTitle] = useState(initialData?.title || '');
  const [description, setDescription] = useState(initialData?.description || '');
  const [ownerType, setOwnerType] = useState<ActionItemOwnerType>(initialData?.ownerType || 'MANAGER');
  const [dueDate, setDueDate] = useState(initialData?.dueDate || '');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!title.trim()) {
      setError('Title is required');
      return;
    }

    const data: CreateActionItemRequest | UpdateActionItemRequest = {
      title: title.trim(),
      description: description.trim() || null,
      ownerType,
      dueDate: dueDate || null,
    };

    onSubmit(data);
  };

  return (
    <form onSubmit={handleSubmit} data-testid="action-item-form">
      {error && (
        <div
          data-testid="form-error"
          style={{
            color: 'var(--color-alert)',
            fontSize: 'var(--text-small)',
            marginBottom: '12px',
            padding: '8px 12px',
            backgroundColor: 'var(--color-alert-muted)',
            borderRadius: 'var(--radius-medium)',
          }}
        >
          {error}
        </div>
      )}

      {/* Title */}
      <div style={{ marginBottom: '16px' }}>
        <label
          htmlFor="action-item-title"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
            fontFamily: 'var(--font-mono)',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Title *
        </label>
        <input
          id="action-item-title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="What needs to be done?"
          data-testid="action-item-title-input"
          style={{
            width: '100%',
            padding: '10px 12px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        />
      </div>

      {/* Description */}
      <div style={{ marginBottom: '16px' }}>
        <label
          htmlFor="action-item-description"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
            fontFamily: 'var(--font-mono)',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Description
        </label>
        <textarea
          id="action-item-description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Additional details (optional)"
          rows={3}
          data-testid="action-item-description-input"
          style={{
            width: '100%',
            padding: '10px 12px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            resize: 'vertical',
          }}
        />
      </div>

      {/* Owner Type + Due Date row */}
      <div style={{ display: 'flex', gap: '16px', marginBottom: '16px', flexWrap: 'wrap' }}>
        <div style={{ flex: 1, minWidth: '150px' }}>
          <label
            htmlFor="action-item-owner"
            style={{
              display: 'block',
              fontSize: 'var(--text-caption)',
              color: 'var(--color-text-muted)',
              marginBottom: '4px',
              fontFamily: 'var(--font-mono)',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
            }}
          >
            Owner
          </label>
          <select
            id="action-item-owner"
            value={ownerType}
            onChange={(e) => setOwnerType(e.target.value as ActionItemOwnerType)}
            data-testid="action-item-owner-select"
            style={{
              width: '100%',
              padding: '10px 12px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
            }}
          >
            <option value="MANAGER">Manager (me)</option>
            <option value="PERSON">Report</option>
          </select>
        </div>

        <div style={{ flex: 1, minWidth: '150px' }}>
          <label
            htmlFor="action-item-due-date"
            style={{
              display: 'block',
              fontSize: 'var(--text-caption)',
              color: 'var(--color-text-muted)',
              marginBottom: '4px',
              fontFamily: 'var(--font-mono)',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
            }}
          >
            Due Date
          </label>
          <input
            id="action-item-due-date"
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            data-testid="action-item-due-date-input"
            style={{
              width: '100%',
              padding: '10px 12px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
            }}
          />
        </div>
      </div>

      {/* Buttons */}
      <div style={{ display: 'flex', gap: '12px' }}>
        <button
          type="submit"
          disabled={isSubmitting}
          data-testid="action-item-submit-btn"
          style={{
            padding: '10px 20px',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-semibold)',
            fontFamily: 'var(--font-mono)',
            cursor: isSubmitting ? 'not-allowed' : 'pointer',
            opacity: isSubmitting ? 0.6 : 1,
            boxShadow: 'var(--glow-primary)',
          }}
        >
          {isSubmitting ? 'Saving...' : mode === 'create' ? 'Create Action Item' : 'Save Changes'}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            data-testid="action-item-cancel-btn"
            style={{
              padding: '10px 20px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-secondary)',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              cursor: 'pointer',
            }}
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
