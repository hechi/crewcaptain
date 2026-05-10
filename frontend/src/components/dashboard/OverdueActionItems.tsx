'use client';

import { DashboardActionItem } from '@/types/dashboard';
import Link from 'next/link';

interface OverdueActionItemsProps {
  items: DashboardActionItem[];
}

/**
 * Displays overdue action items on the dashboard with alert styling.
 * Each item links to the person's detail page.
 */
export default function OverdueActionItems({ items }: OverdueActionItemsProps) {
  if (items.length === 0) {
    return (
      <div data-testid="overdue-items-empty" style={{ color: 'var(--color-text-muted)', fontSize: '14px' }}>
        No overdue items — you&apos;re all caught up!
      </div>
    );
  }

  return (
    <div data-testid="overdue-items-list">
      {items.map((item) => (
        <div
          key={item.id}
          data-testid="overdue-item"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 16px',
            marginBottom: '8px',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-bg-surface)',
            boxShadow: 'var(--glow-alert)',
          }}
        >
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span
                data-testid="overdue-item-title"
                style={{
                  fontSize: '14px',
                  fontWeight: 'var(--weight-semibold)',
                  color: 'var(--color-text-primary)',
                }}
              >
                {item.title}
              </span>
              <span
                style={{
                  fontSize: '11px',
                  padding: '2px 6px',
                  borderRadius: 'var(--radius-small)',
                  backgroundColor: item.ownerType === 'MANAGER' ? 'var(--color-primary-muted)' : 'var(--color-accent-muted)',
                  color: item.ownerType === 'MANAGER' ? 'var(--color-primary)' : 'var(--color-accent)',
                }}
              >
                {item.ownerType === 'MANAGER' ? 'You' : 'Them'}
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '4px' }}>
              <Link
                href={`/people/${item.personId}`}
                data-testid="overdue-item-person"
                style={{
                  fontSize: '12px',
                  color: 'var(--color-text-secondary)',
                  textDecoration: 'none',
                }}
              >
                {item.personName}
              </Link>
              <span
                data-testid="overdue-item-due-date"
                style={{
                  fontSize: '12px',
                  color: 'var(--color-alert)',
                  fontWeight: 'var(--weight-medium)',
                }}
              >
                Due {new Date(item.dueDate + 'T00:00:00').toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
