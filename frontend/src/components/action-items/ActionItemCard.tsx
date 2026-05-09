'use client';

import { ActionItem } from '@/types/action-item';
import ActionItemStatusBadge from './ActionItemStatusBadge';

interface ActionItemCardProps {
  item: ActionItem;
  onComplete?: (id: string) => void;
  onCancel?: (id: string) => void;
  onDelete?: (id: string) => void;
  onEdit?: (id: string) => void;
}

/**
 * Displays a single action item card with title, due date, owner badge, status,
 * and action buttons (complete, cancel, delete).
 * Overdue items are visually highlighted with alert styling.
 */
export default function ActionItemCard({ item, onComplete, onCancel, onDelete, onEdit }: ActionItemCardProps) {
  const isOverdue = item.status === 'OPEN' && item.dueDate && new Date(item.dueDate) < new Date();
  const isOpen = item.status === 'OPEN';

  const formattedDueDate = item.dueDate
    ? new Date(item.dueDate + 'T00:00:00').toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      })
    : null;

  return (
    <div
      data-testid="action-item-card"
      style={{
        padding: 'var(--space-4)',
        border: `1px solid ${isOverdue ? 'var(--color-alert)' : 'var(--color-border)'}`,
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
        boxShadow: isOverdue ? 'var(--glow-alert)' : 'none',
        transition: 'border-color 0.2s, box-shadow 0.2s',
      }}
    >
      {/* Header: title + status */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
        <h4
          data-testid="action-item-title"
          style={{
            margin: 0,
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-semibold)',
            color: item.status === 'DONE' ? 'var(--color-text-muted)' : 'var(--color-text-primary)',
            textDecoration: item.status === 'DONE' ? 'line-through' : 'none',
            flex: 1,
            marginRight: '12px',
          }}
        >
          {item.title}
        </h4>
        <ActionItemStatusBadge status={item.status} />
      </div>

      {/* Description */}
      {item.description && (
        <p
          data-testid="action-item-description"
          style={{
            margin: '0 0 8px',
            fontSize: 'var(--text-small)',
            color: 'var(--color-text-secondary)',
            lineHeight: '1.4',
          }}
        >
          {item.description}
        </p>
      )}

      {/* Meta row: due date + owner */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: isOpen ? '12px' : '0' }}>
        {formattedDueDate && (
          <span
            data-testid="action-item-due-date"
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: isOverdue ? 'var(--color-alert)' : 'var(--color-text-muted)',
              fontWeight: isOverdue ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            }}
          >
            {isOverdue ? '⚠ ' : ''}Due: {formattedDueDate}
          </span>
        )}
        <span
          data-testid="action-item-owner"
          style={{
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            backgroundColor: 'var(--color-bg-elevated)',
            padding: '1px 8px',
            borderRadius: 'var(--radius-full)',
          }}
        >
          {item.ownerType === 'MANAGER' ? '👤 Manager' : '👥 Report'}
        </span>
      </div>

      {/* Action buttons */}
      {isOpen && (
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {onComplete && (
            <button
              type="button"
              onClick={() => onComplete(item.id)}
              data-testid="action-item-complete-btn"
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-morale-green)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                color: 'var(--color-morale-green)',
                cursor: 'pointer',
                fontWeight: 'var(--weight-medium)',
              }}
            >
              ✓ Complete
            </button>
          )}
          {onCancel && (
            <button
              type="button"
              onClick={() => onCancel(item.id)}
              data-testid="action-item-cancel-btn"
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-bg-elevated)',
                color: 'var(--color-text-muted)',
                cursor: 'pointer',
              }}
            >
              ✕ Cancel
            </button>
          )}
          {onEdit && (
            <button
              type="button"
              onClick={() => onEdit(item.id)}
              data-testid="action-item-edit-btn"
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-bg-elevated)',
                color: 'var(--color-text-secondary)',
                cursor: 'pointer',
              }}
            >
              ✎ Edit
            </button>
          )}
          {onDelete && (
            <button
              type="button"
              onClick={() => onDelete(item.id)}
              data-testid="action-item-delete-btn"
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-alert-muted)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-alert-muted)',
                color: 'var(--color-alert)',
                cursor: 'pointer',
              }}
            >
              🗑 Delete
            </button>
          )}
        </div>
      )}
    </div>
  );
}
