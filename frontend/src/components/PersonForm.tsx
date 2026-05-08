'use client';

import { useState } from 'react';
import { Person } from '@/types/person';

interface PersonFormData {
  name: string;
  preferredName: string;
  roleTitle: string;
  timezone: string;
  startDate: string;
  email: string;
  tags: string;
}

interface PersonFormProps {
  mode: 'create' | 'edit';
  initialData?: Person;
  onSubmit: (data: {
    name: string;
    preferredName?: string;
    roleTitle?: string;
    timezone?: string;
    startDate?: string;
    email?: string;
    tags?: string[];
  }) => void;
  onCancel?: () => void;
}

export default function PersonForm({ mode, initialData, onSubmit, onCancel }: PersonFormProps) {
  const [formData, setFormData] = useState<PersonFormData>({
    name: initialData?.name || '',
    preferredName: initialData?.preferredName || '',
    roleTitle: initialData?.roleTitle || '',
    timezone: initialData?.timezone || '',
    startDate: initialData?.startDate || '',
    email: initialData?.email || '',
    tags: initialData?.tags?.join(', ') || '',
  });
  const [errors, setErrors] = useState<{ name?: string }>({});

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (name === 'name' && errors.name) {
      setErrors({});
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name.trim()) {
      setErrors({ name: 'Name is required' });
      return;
    }

    const tags = formData.tags
      .split(',')
      .map((t) => t.trim())
      .filter((t) => t.length > 0);

    onSubmit({
      name: formData.name.trim(),
      preferredName: formData.preferredName.trim() || undefined,
      roleTitle: formData.roleTitle.trim() || undefined,
      timezone: formData.timezone.trim() || undefined,
      startDate: formData.startDate || undefined,
      email: formData.email.trim() || undefined,
      tags: tags.length > 0 ? tags : undefined,
    });
  };

  const inputStyle = {
    width: '100%',
    padding: '8px 12px',
    border: '1px solid var(--color-neutral-border)',
    borderRadius: 'var(--radius-medium)',
    fontSize: 'var(--text-body)',
    backgroundColor: 'var(--color-neutral-surface)',
  };

  const labelStyle = {
    display: 'block' as const,
    marginBottom: '4px',
    fontSize: 'var(--text-body)',
    fontWeight: 500,
    color: 'var(--color-neutral-text)',
  };

  const fieldStyle = {
    marginBottom: 'var(--space-4)',
  };

  return (
    <form onSubmit={handleSubmit} data-testid="person-form">
      <div style={fieldStyle}>
        <label htmlFor="name" style={labelStyle}>
          Name <span style={{ color: 'var(--color-error)' }}>*</span>
        </label>
        <input
          id="name"
          name="name"
          type="text"
          value={formData.name}
          onChange={handleChange}
          style={{
            ...inputStyle,
            borderColor: errors.name ? 'var(--color-error)' : 'var(--color-neutral-border)',
          }}
          aria-required="true"
          aria-invalid={!!errors.name}
        />
        {errors.name && (
          <p data-testid="name-error" style={{ color: 'var(--color-error)', fontSize: 'var(--text-caption)', marginTop: '4px' }}>
            {errors.name}
          </p>
        )}
      </div>

      <div style={fieldStyle}>
        <label htmlFor="preferredName" style={labelStyle}>Preferred Name</label>
        <input
          id="preferredName"
          name="preferredName"
          type="text"
          value={formData.preferredName}
          onChange={handleChange}
          style={inputStyle}
        />
      </div>

      <div style={fieldStyle}>
        <label htmlFor="roleTitle" style={labelStyle}>Role / Title</label>
        <input
          id="roleTitle"
          name="roleTitle"
          type="text"
          value={formData.roleTitle}
          onChange={handleChange}
          style={inputStyle}
        />
      </div>

      <div style={fieldStyle}>
        <label htmlFor="timezone" style={labelStyle}>Timezone</label>
        <input
          id="timezone"
          name="timezone"
          type="text"
          value={formData.timezone}
          onChange={handleChange}
          style={inputStyle}
        />
      </div>

      <div style={fieldStyle}>
        <label htmlFor="startDate" style={labelStyle}>Start Date</label>
        <input
          id="startDate"
          name="startDate"
          type="date"
          value={formData.startDate}
          onChange={handleChange}
          style={inputStyle}
        />
      </div>

      <div style={fieldStyle}>
        <label htmlFor="email" style={labelStyle}>Email</label>
        <input
          id="email"
          name="email"
          type="email"
          value={formData.email}
          onChange={handleChange}
          style={inputStyle}
        />
      </div>

      <div style={fieldStyle}>
        <label htmlFor="tags" style={labelStyle}>Tags (comma-separated)</label>
        <input
          id="tags"
          name="tags"
          type="text"
          value={formData.tags}
          onChange={handleChange}
          placeholder="e.g. engineering, senior"
          style={inputStyle}
        />
      </div>

      <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
        <button
          type="submit"
          style={{
            padding: '10px 20px',
            backgroundColor: 'var(--color-accent)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-medium)',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          {mode === 'create' ? 'Create Person' : 'Save Changes'}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            style={{
              padding: '10px 20px',
              backgroundColor: 'var(--color-neutral-bg)',
              color: 'var(--color-neutral-text)',
              border: '1px solid var(--color-neutral-border)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              fontWeight: 'var(--weight-medium)',
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
