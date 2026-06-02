'use client';

import Link from 'next/link';
import { Person } from '@/types/person';
import MoraleIndicator from './MoraleIndicator';
import { getColorStyles } from './StickyNotesGrid';

interface PersonCardProps {
  person: Person;
}

export default function PersonCard({ person }: PersonCardProps) {
  const previewItems = person.pinnedRememberItems
    .filter((item) => !item.sensitive)
    .slice(0, 2);

  return (
    <Link
      href={`/people/${person.id}`}
      data-testid="person-card"
      style={{
        display: 'block',
        padding: 'var(--space-4)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        textDecoration: 'none',
        color: 'inherit',
        cursor: 'pointer',
        backgroundColor: 'var(--color-bg-surface)',
        backdropFilter: 'var(--glass-blur)',
        transition: 'border-color 0.2s, box-shadow 0.2s',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3 style={{
            margin: 0,
            fontSize: '16px',
            fontWeight: 'var(--weight-semibold)',
            fontFamily: 'var(--font-heading)',
            color: 'var(--color-text-primary)',
            letterSpacing: '-0.2px',
          }}>
            {person.name}
          </h3>
          {person.roleTitle && (
            <p style={{
              margin: '4px 0 0',
              fontSize: 'var(--text-body)',
              color: 'var(--color-text-secondary)',
            }}>
              {person.roleTitle}
            </p>
          )}
        </div>
        <MoraleIndicator moraleStatus={person.moraleStatus} />
      </div>

      {/* Sticky note previews */}
      {previewItems.length > 0 ? (
        <div data-testid="sticky-note-previews" style={{ display: 'flex', gap: '6px', marginTop: '8px', flexWrap: 'wrap' }}>
          {previewItems.map((item) => {
            const colorStyle = getColorStyles(item.color);
            const truncated = item.text.length > 40 ? item.text.slice(0, 40) + '…' : item.text;
            return (
              <span
                key={item.id}
                style={{
                  display: 'inline-block',
                  padding: '2px 8px',
                  fontSize: '11px',
                  fontFamily: 'var(--font-mono)',
                  backgroundColor: colorStyle.bg,
                  border: `1px solid ${colorStyle.border}`,
                  borderRadius: 'var(--radius-small)',
                  color: 'var(--color-text-secondary)',
                  maxWidth: '180px',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
              >
                {item.tag && <strong>{item.tag}: </strong>}
                {truncated}
              </span>
            );
          })}
          {person.pinnedRememberItems.some((i) => i.sensitive) && (
            <span style={{ fontSize: '11px', color: 'var(--color-text-muted)', fontStyle: 'italic' }}>
              +sensitive
            </span>
          )}
        </div>
      ) : person.pinnedRememberItems.length > 0 && person.pinnedRememberItems.every((i) => i.sensitive) ? (
        <div data-testid="sticky-note-previews" style={{ display: 'flex', gap: '6px', marginTop: '8px' }}>
          <span style={{ fontSize: '11px', color: 'var(--color-text-muted)', fontStyle: 'italic' }}>
            +sensitive
          </span>
        </div>
      ) : person.pinnedRememberItems.length === 0 ? (
        <p data-testid="add-sticky-cta" style={{ margin: '8px 0 0', fontSize: '11px', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>
          Add sticky note
        </p>
      ) : null}
    </Link>
  );
}
