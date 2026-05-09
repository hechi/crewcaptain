'use client';

import Link from 'next/link';
import { OneOnOneEntry } from '@/types/one-on-one';
import SensitiveBadge from './SensitiveBadge';

interface OneOnOneEntryCardProps {
  entry: OneOnOneEntry;
  personId: string;
  hideSensitiveContent?: boolean;
}

/**
 * Displays a 1:1 entry summary card.
 * Shows meeting date, notes preview (~100 chars), agenda count, and sensitive badge.
 * Clickable — navigates to entry detail/edit page.
 * Visually distinguishes sensitive entries with lock icon and muted text.
 */
export default function OneOnOneEntryCard({ entry, personId, hideSensitiveContent = false }: OneOnOneEntryCardProps) {
  const meetingDate = new Date(entry.meetingDate);
  const formattedDate = meetingDate.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
  const formattedTime = meetingDate.toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
  });

  const notesPreview = entry.notesMarkdown
    ? entry.notesMarkdown.length > 100
      ? entry.notesMarkdown.substring(0, 100) + '…'
      : entry.notesMarkdown
    : null;

  const agendaCount = entry.agendaItems.length;
  const isSensitive = entry.sensitive;
  const shouldHideContent = isSensitive && hideSensitiveContent;

  return (
    <Link
      href={`/people/${personId}/one-on-ones/${entry.id}`}
      data-testid="one-on-one-entry-card"
      style={{
        display: 'block',
        padding: 'var(--space-4)',
        border: `1px solid ${isSensitive ? 'rgba(255, 214, 0, 0.3)' : 'var(--color-border)'}`,
        borderRadius: 'var(--radius-medium)',
        textDecoration: 'none',
        color: 'inherit',
        cursor: 'pointer',
        backgroundColor: isSensitive ? 'var(--color-warning-muted)' : 'var(--color-bg-surface)',
        transition: 'border-color 0.2s, box-shadow 0.2s',
      }}
      aria-label={`1:1 entry from ${formattedDate}${isSensitive ? ' (sensitive)' : ''}`}
    >
      {/* Header row: date + badges */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <div>
          <span
            data-testid="entry-card-date"
            style={{ fontSize: '15px', fontWeight: 'var(--weight-semibold)', fontFamily: 'var(--font-mono)', color: 'var(--color-primary)' }}
          >
            {formattedDate}
          </span>
          <span style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-muted)', marginLeft: '8px', fontFamily: 'var(--font-mono)' }}>
            {formattedTime}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {agendaCount > 0 && (
            <span
              data-testid="entry-card-agenda-count"
              style={{
                fontSize: 'var(--text-caption)',
                color: 'var(--color-text-secondary)',
                backgroundColor: 'var(--color-bg-elevated)',
                padding: '2px 8px',
                borderRadius: 'var(--radius-full)',
                fontFamily: 'var(--font-mono)',
              }}
            >
              {agendaCount} item{agendaCount !== 1 ? 's' : ''}
            </span>
          )}
          {isSensitive && <SensitiveBadge />}
        </div>
      </div>

      {/* Notes preview */}
      {shouldHideContent ? (
        <p
          data-testid="entry-card-hidden"
          style={{ margin: 0, fontSize: 'var(--text-body)', color: 'var(--color-text-muted)', fontStyle: 'italic' }}
        >
          Sensitive content hidden
        </p>
      ) : notesPreview ? (
        <p
          data-testid="entry-card-notes-preview"
          style={{
            margin: 0,
            fontSize: 'var(--text-body)',
            color: isSensitive ? 'var(--color-warning)' : 'var(--color-text-secondary)',
            lineHeight: '1.4',
          }}
        >
          {notesPreview}
        </p>
      ) : (
        <p
          data-testid="entry-card-no-notes"
          style={{ margin: 0, fontSize: 'var(--text-body)', color: 'var(--color-text-muted)', fontStyle: 'italic' }}
        >
          No notes
        </p>
      )}
    </Link>
  );
}
