'use client';

import { DashboardActionItem } from '@/types/dashboard';
import Link from 'next/link';

interface DueSoonActionItemsProps {
  items: DashboardActionItem[];
}

/**
 * Displays action items due soon on the dashboard with warning styling.
 * Each item links to the person's detail page.
 */
export default function DueSoonActionItems({ items }: DueSoonActionItemsProps) {
  if (items.length === 0) {
    return (
      <div data-testid="due-soon-items-empty" style={{ color: 'var(--color-text-muted)', fontSize: '14px' }}>
        No items due soon.
      </div>
    );
  }

  return (
    <div data-testid="due-soon-items-list">
      {items.map((item) => (
        <div
          key={item.id}
          data-testid="due-soon-item"
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
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span
                data-testid="due-soon-item-title"
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
                data-testid="due-soon-item-person"
                style={{
                  fontSize: '12px',
                  color: 'var(--color-text-secondary)',
                  textDecoration: 'none',
                }}
              >
                {item.personName}
              </Link>
              <span
                data-testid="due-soon-item-due-date"
                style={{
                  fontSize: '12px',
                  color: 'var(--color-warning)',
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
