'use client';

import Link from 'next/link';
import { TodayPriorityItem, PriorityKind } from '@/types/dashboard';

interface TodayPrioritiesProps {
  items: TodayPriorityItem[];
}

const KIND_META: Record<PriorityKind, { label: string; color: string; borderColor: string }> = {
  OVERDUE: { label: 'Overdue', color: 'var(--color-alert)', borderColor: 'var(--color-alert)' },
  DUE_SOON: { label: 'Due soon', color: 'var(--color-warning)', borderColor: 'var(--color-warning)' },
  INBOX_NOTE: { label: 'Note', color: 'var(--color-secondary)', borderColor: 'var(--color-border)' },
};

/**
 * "Today's Priorities" — a single ranked focus list combining overdue and due-soon action
 * items with the manager's self-assigned INBOX quick notes. Read-only; each row links to the
 * place to act (the person's page for action items, My Notes for notes).
 */
export default function TodayPriorities({ items }: TodayPrioritiesProps) {
  if (items.length === 0) {
    return (
      <div data-testid="today-priorities-empty" style={{ color: 'var(--color-text-muted)', fontSize: '14px' }}>
        Nothing urgent right now — your plate is clear.
      </div>
    );
  }

  return (
    <div data-testid="today-priorities-list">
      {items.map((item) => {
        const meta = KIND_META[item.kind];
        const href = item.kind === 'INBOX_NOTE'
          ? '/my-notes'
          : item.personId
            ? `/people/${item.personId}`
            : '/action-items';

        return (
          <div
            key={`${item.kind}-${item.id}`}
            data-testid="today-priority-item"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 16px',
              marginBottom: '8px',
              borderRadius: 'var(--radius-medium)',
              border: `1px solid ${meta.borderColor}`,
              backgroundColor: 'var(--color-bg-surface)',
            }}
          >
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span
                  data-testid="today-priority-kind"
                  style={{
                    fontSize: '11px',
                    padding: '2px 6px',
                    borderRadius: 'var(--radius-small)',
                    fontFamily: 'var(--font-mono)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px',
                    color: meta.color,
                    border: `1px solid ${meta.color}`,
                  }}
                >
                  {meta.label}
                </span>
                <Link
                  href={href}
                  data-testid="today-priority-title"
                  style={{
                    fontSize: '14px',
                    fontWeight: 'var(--weight-semibold)',
                    color: 'var(--color-text-primary)',
                    textDecoration: 'none',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {item.title}
                </Link>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '4px' }}>
                {item.personName && (
                  <span
                    data-testid="today-priority-person"
                    style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}
                  >
                    {item.personName}
                  </span>
                )}
                {item.dueDate && (
                  <span
                    data-testid="today-priority-due-date"
                    style={{
                      fontSize: '12px',
                      color: meta.color,
                      fontWeight: 'var(--weight-medium)',
                    }}
                  >
                    Due {new Date(item.dueDate + 'T00:00:00').toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                  </span>
                )}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
