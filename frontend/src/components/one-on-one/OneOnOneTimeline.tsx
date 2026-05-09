'use client';

import { useState } from 'react';
import { OneOnOneEntry } from '@/types/one-on-one';
import { PaginatedResponse } from '@/types/person';
import OneOnOneEntryCard from './OneOnOneEntryCard';
import Pagination from '../Pagination';

interface OneOnOneTimelineProps {
  entries: PaginatedResponse<OneOnOneEntry>;
  personId: string;
  onPageChange: (page: number) => void;
  onStartOneOnOne: () => void;
}

/**
 * Paginated list of OneOnOneEntryCard components.
 * Displays entries in reverse chronological order.
 * Includes empty state with "Start 1:1" button and "Hide sensitive" toggle.
 */
export default function OneOnOneTimeline({
  entries,
  personId,
  onPageChange,
  onStartOneOnOne,
}: OneOnOneTimelineProps) {
  const [hideSensitive, setHideSensitive] = useState(false);

  const isEmpty = entries.content.length === 0 && entries.page === 0;

  if (isEmpty) {
    return (
      <div
        data-testid="one-on-one-timeline-empty"
        style={{
          textAlign: 'center',
          padding: '48px 24px',
          border: '1px dashed var(--color-border)',
          borderRadius: 'var(--radius-large)',
          backgroundColor: 'var(--color-bg-surface)',
        }}
      >
        <p style={{ fontSize: '16px', color: 'var(--color-text-secondary)', marginBottom: 'var(--space-4)' }}>
          No 1:1s yet — start tracking your meetings.
        </p>
        <button
          type="button"
          onClick={onStartOneOnOne}
          data-testid="start-one-on-one-button"
          style={{
            padding: '10px 20px',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-semibold)',
            fontFamily: 'var(--font-mono)',
            cursor: 'pointer',
            boxShadow: 'var(--glow-primary)',
          }}
        >
          Start 1:1
        </button>
      </div>
    );
  }

  return (
    <div data-testid="one-on-one-timeline">
      {/* Toolbar: hide sensitive toggle */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          alignItems: 'center',
          marginBottom: 'var(--space-4)',
        }}
      >
        <label
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: 'var(--text-small)',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
            fontFamily: 'var(--font-mono)',
          }}
        >
          <input
            type="checkbox"
            checked={hideSensitive}
            onChange={(e) => setHideSensitive(e.target.checked)}
            data-testid="hide-sensitive-toggle"
            style={{ width: '14px', height: '14px', cursor: 'pointer' }}
          />
          Hide sensitive
        </label>
      </div>

      {/* Entry list */}
      <div
        style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}
        role="list"
        aria-label="1:1 entries"
      >
        {entries.content.map((entry) => (
          <div key={entry.id} role="listitem">
            <OneOnOneEntryCard
              entry={entry}
              personId={personId}
              hideSensitiveContent={hideSensitive}
            />
          </div>
        ))}
      </div>

      {/* Pagination */}
      <Pagination
        currentPage={entries.page}
        totalPages={entries.totalPages}
        onPageChange={onPageChange}
      />
    </div>
  );
}
