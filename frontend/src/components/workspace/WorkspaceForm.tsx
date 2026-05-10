'use client';

import { useState } from 'react';
import { Workspace } from '@/types/workspace';

interface WorkspaceFormProps {
  workspace?: Workspace;
  onSubmit: (data: { name: string; description?: string }) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export default function WorkspaceForm({
  workspace,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: WorkspaceFormProps) {
  const [name, setName] = useState(workspace?.name || '');
  const [description, setDescription] = useState(workspace?.description || '');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Name is required');
      return;
    }
    if (name.length > 100) {
      setError('Name must not exceed 100 characters');
      return;
    }
    if (description.length > 500) {
      setError('Description must not exceed 500 characters');
      return;
    }
    setError(null);
    onSubmit({ name: name.trim(), description: description.trim() || undefined });
  };

  const inputStyle = {
    width: '100%',
    padding: '10px 14px',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-medium)',
    fontSize: 'var(--text-body)',
    backgroundColor: 'var(--color-bg-elevated)',
    color: 'var(--color-text-primary)',
  };

  const buttonStyle = {
    padding: '10px 20px',
    borderRadius: 'var(--radius-medium)',
    fontSize: 'var(--text-body)',
    fontWeight: '500' as const,
    cursor: isSubmitting ? 'not-allowed' : 'pointer',
    border: 'none',
    transition: 'background-color 0.2s',
  };

  return (
    <form onSubmit={handleSubmit} data-testid="workspace-form">
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        <div>
          <label htmlFor="workspace-name" style={{ display: 'block', marginBottom: '4px', color: 'var(--color-text-secondary)', fontSize: 'var(--text-small)' }}>
            Name *
          </label>
          <input
            id="workspace-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g., My Team, Mentees, Skip-levels"
            style={inputStyle}
            maxLength={100}
            disabled={isSubmitting}
            aria-required="true"
          />
        </div>
        <div>
          <label htmlFor="workspace-description" style={{ display: 'block', marginBottom: '4px', color: 'var(--color-text-secondary)', fontSize: 'var(--text-small)' }}>
            Description
          </label>
          <textarea
            id="workspace-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional description for this workspace"
            style={{ ...inputStyle, minHeight: '80px', resize: 'vertical' }}
            maxLength={500}
            disabled={isSubmitting}
          />
        </div>
        {error && (
          <p data-testid="workspace-form-error" style={{ color: 'var(--color-danger)', fontSize: 'var(--text-small)', margin: 0 }}>
            {error}
          </p>
        )}
        <div style={{ display: 'flex', gap: 'var(--space-2)', justifyContent: 'flex-end' }}>
          <button
            type="button"
            onClick={onCancel}
            disabled={isSubmitting}
            style={{ ...buttonStyle, backgroundColor: 'var(--color-bg-elevated)', color: 'var(--color-text-secondary)', border: '1px solid var(--color-border)' }}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            style={{ ...buttonStyle, backgroundColor: 'var(--color-accent)', color: 'var(--color-bg-base)' }}
          >
            {isSubmitting ? 'Saving...' : workspace ? 'Update' : 'Create'}
          </button>
        </div>
      </div>
    </form>
  );
}
