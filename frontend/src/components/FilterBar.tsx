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
    <div data-testid="filter-bar" style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'center' }}>
      <input
        type="text"
        placeholder="Filter by tag..."
        value={tag}
        onChange={handleTagChange}
        aria-label="Filter by tag"
        style={{
          padding: '10px 12px',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
          fontSize: 'var(--text-body)',
          fontFamily: 'var(--font-mono)',
          backgroundColor: 'var(--glass-elevated-bg)',
          backdropFilter: 'var(--glass-blur)',
          WebkitBackdropFilter: 'var(--glass-blur)',
          color: 'var(--color-text-primary)',
          transition: 'border-color 0.2s, box-shadow 0.2s',
          letterSpacing: '0.2px',
        }}
      />
      <select
        value={morale}
        onChange={handleMoraleChange}
        aria-label="Filter by morale status"
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
