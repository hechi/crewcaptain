'use client';

import { TriageFilters, OwnerScope, TriageItemType } from '@/types/triage';

interface TriageFilterBarProps {
  filters: TriageFilters;
  onFiltersChange: (filters: TriageFilters) => void;
}

export default function TriageFilterBar({ filters, onFiltersChange }: TriageFilterBarProps) {
  return (
    <div
      data-testid="triage-filter-bar"
      style={{
        display: 'flex',
        gap: 'var(--space-3)',
        alignItems: 'center',
        marginBottom: 'var(--space-4)',
        flexWrap: 'wrap',
      }}
    >
      {/* Scope toggle */}
      <div
        style={{
          display: 'flex',
          borderRadius: 'var(--radius-medium)',
          overflow: 'hidden',
          border: '1px solid var(--color-border)',
        }}
      >
        <button
          data-testid="scope-all"
          onClick={() => onFiltersChange({ ...filters, scope: 'ALL' })}
          style={{
            padding: 'var(--space-2) var(--space-3)',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            fontWeight: 'var(--weight-medium)',
            border: 'none',
            cursor: 'pointer',
            backgroundColor: filters.scope === 'ALL' ? 'var(--color-primary-muted)' : 'transparent',
            color: filters.scope === 'ALL' ? 'var(--color-primary)' : 'var(--color-text-secondary)',
            transition: 'background-color 0.15s, color 0.15s',
          }}
        >
          All
        </button>
        <button
          data-testid="scope-mine"
          onClick={() => onFiltersChange({ ...filters, scope: 'MINE' })}
          style={{
            padding: 'var(--space-2) var(--space-3)',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            fontWeight: 'var(--weight-medium)',
            border: 'none',
            borderLeft: '1px solid var(--color-border)',
            cursor: 'pointer',
            backgroundColor: filters.scope === 'MINE' ? 'var(--color-primary-muted)' : 'transparent',
            color: filters.scope === 'MINE' ? 'var(--color-primary)' : 'var(--color-text-secondary)',
            transition: 'background-color 0.15s, color 0.15s',
          }}
        >
          Mine
        </button>
      </div>

      {/* Type filter */}
      <select
        data-testid="type-filter"
        value={filters.type || ''}
        onChange={(e) =>
          onFiltersChange({
            ...filters,
            type: (e.target.value || undefined) as TriageItemType | undefined,
          })
        }
        aria-label="Filter by type"
      >
        <option value="">All Types</option>
        <option value="ACTION_ITEM_OVERDUE">Overdue</option>
        <option value="ACTION_ITEM_DUE_SOON">Due Soon</option>
        <option value="STALE_ONE_ON_ONE">Stale 1:1s</option>
        <option value="UPCOMING_ANNIVERSARY">Anniversaries</option>
      </select>
    </div>
  );
}
