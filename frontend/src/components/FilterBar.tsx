'use client';

import { MoraleStatus } from '@/types/person';
import { useState } from 'react';

interface FilterBarProps {
  onFilterChange: (filters: { tag: string; morale: MoraleStatus | '' }) => void;
  initialTag?: string;
  initialMorale?: MoraleStatus | '';
}

export default function FilterBar({ onFilterChange, initialTag = '', initialMorale = '' }: FilterBarProps) {
  const [tag, setTag] = useState(initialTag);
  const [morale, setMorale] = useState<MoraleStatus | ''>(initialMorale);

  const handleTagChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newTag = e.target.value;
    setTag(newTag);
    onFilterChange({ tag: newTag, morale });
  };

  const handleMoraleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newMorale = e.target.value as MoraleStatus | '';
    setMorale(newMorale);
    onFilterChange({ tag, morale: newMorale });
  };

  return (
    <div data-testid="filter-bar" style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
      <input
        type="text"
        placeholder="Filter by tag..."
        value={tag}
        onChange={handleTagChange}
        aria-label="Filter by tag"
        style={{
          padding: '8px 12px',
          border: '1px solid var(--color-neutral-border)',
          borderRadius: 'var(--radius-medium)',
          fontSize: 'var(--text-body)',
          backgroundColor: 'var(--color-neutral-surface)',
        }}
      />
      <select
        value={morale}
        onChange={handleMoraleChange}
        aria-label="Filter by morale status"
        style={{
          padding: '8px 12px',
          border: '1px solid var(--color-neutral-border)',
          borderRadius: 'var(--radius-medium)',
          fontSize: 'var(--text-body)',
          backgroundColor: 'var(--color-neutral-surface)',
        }}
      >
        <option value="">All morale statuses</option>
        <option value="GREEN">Green</option>
        <option value="YELLOW">Yellow</option>
        <option value="RED">Red</option>
        <option value="UNKNOWN">Unknown</option>
      </select>
    </div>
  );
}
