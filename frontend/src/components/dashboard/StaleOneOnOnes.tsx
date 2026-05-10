'use client';

import { StaleOneOnOneReminder } from '@/types/dashboard';
import Link from 'next/link';

interface StaleOneOnOnesProps {
  reminders: StaleOneOnOneReminder[];
}

/**
 * Displays stale 1:1 reminders — persons whose last meeting exceeds their cadence.
 */
export default function StaleOneOnOnes({ reminders }: StaleOneOnOnesProps) {
  if (reminders.length === 0) {
    return (
      <div data-testid="stale-1on1s-empty" style={{ color: 'var(--color-text-muted)', fontSize: '14px' }}>
        All 1:1s are on track.
      </div>
    );
  }

  const cadenceLabel = (reminder: StaleOneOnOneReminder): string => {
    switch (reminder.cadenceType) {
      case 'WEEKLY': return 'weekly';
      case 'BIWEEKLY': return 'biweekly';
      case 'MONTHLY': return 'monthly';
      case 'CUSTOM': return `every ${reminder.customIntervalDays} days`;
    }
  };

  return (
    <div data-testid="stale-1on1s-list">
      {reminders.map((reminder) => (
        <div
          key={reminder.personId}
          data-testid="stale-1on1-item"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 16px',
            marginBottom: '8px',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-warning)',
            backgroundColor: 'var(--color-bg-surface)',
          }}
        >
          <div style={{ flex: 1 }}>
            <Link
              href={`/people/${reminder.personId}`}
              data-testid="stale-1on1-person"
              style={{
                fontSize: '14px',
                fontWeight: 'var(--weight-semibold)',
                color: 'var(--color-text-primary)',
                textDecoration: 'none',
              }}
            >
              {reminder.personName}
            </Link>
            <div style={{ marginTop: '4px', fontSize: '12px', color: 'var(--color-text-secondary)' }}>
              <span data-testid="stale-1on1-days">
                {reminder.daysSinceLastMeeting} days since last 1:1
              </span>
              <span style={{ margin: '0 8px', color: 'var(--color-border)' }}>·</span>
              <span data-testid="stale-1on1-cadence">
                Cadence: {cadenceLabel(reminder)}
              </span>
            </div>
          </div>
          <div
            data-testid="stale-1on1-overdue-badge"
            style={{
              fontSize: '11px',
              padding: '4px 8px',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'rgba(255, 107, 107, 0.1)',
              color: 'var(--color-alert)',
              fontWeight: 'var(--weight-medium)',
              whiteSpace: 'nowrap',
            }}
          >
            {reminder.daysSinceLastMeeting - reminder.expectedIntervalDays}d overdue
          </div>
        </div>
      ))}
    </div>
  );
}
