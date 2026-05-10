'use client';

import { UpcomingAnniversary } from '@/types/dashboard';
import Link from 'next/link';

interface UpcomingAnniversariesProps {
  anniversaries: UpcomingAnniversary[];
}

/**
 * Displays upcoming work anniversaries on the dashboard.
 */
export default function UpcomingAnniversaries({ anniversaries }: UpcomingAnniversariesProps) {
  if (anniversaries.length === 0) {
    return (
      <div data-testid="anniversaries-empty" style={{ color: 'var(--color-text-muted)', fontSize: '14px' }}>
        No upcoming anniversaries.
      </div>
    );
  }

  return (
    <div data-testid="anniversaries-list">
      {anniversaries.map((anniversary) => (
        <div
          key={anniversary.personId}
          data-testid="anniversary-item"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 16px',
            marginBottom: '8px',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-border)',
            backgroundColor: 'var(--color-bg-surface)',
          }}
        >
          <div style={{ flex: 1 }}>
            <Link
              href={`/people/${anniversary.personId}`}
              data-testid="anniversary-person"
              style={{
                fontSize: '14px',
                fontWeight: 'var(--weight-semibold)',
                color: 'var(--color-text-primary)',
                textDecoration: 'none',
              }}
            >
              {anniversary.personName}
            </Link>
            <div style={{ marginTop: '4px', fontSize: '12px', color: 'var(--color-text-secondary)' }}>
              <span data-testid="anniversary-years">
                {anniversary.yearsCompleted} {anniversary.yearsCompleted === 1 ? 'year' : 'years'}
              </span>
              <span style={{ margin: '0 8px', color: 'var(--color-border)' }}>·</span>
              <span data-testid="anniversary-date">
                {new Date(anniversary.anniversaryDate + 'T00:00:00').toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
              </span>
            </div>
          </div>
          <div
            data-testid="anniversary-days-until"
            style={{
              fontSize: '11px',
              padding: '4px 8px',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'var(--color-primary-muted)',
              color: 'var(--color-primary)',
              fontWeight: 'var(--weight-medium)',
              whiteSpace: 'nowrap',
            }}
          >
            {anniversary.daysUntil === 0 ? 'Today!' : `in ${anniversary.daysUntil}d`}
          </div>
        </div>
      ))}
    </div>
  );
}
