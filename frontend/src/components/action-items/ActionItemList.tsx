'use client';

import { ActionItem, ActionItemStatus, PaginatedActionItemResponse } from '@/types/action-item';
import ActionItemCard from './ActionItemCard';
import Pagination from '@/components/Pagination';
import EmptyState from '@/components/EmptyState';

interface ActionItemListProps {
  data: PaginatedActionItemResponse;
  onComplete?: (id: string) => void;
  onCancel?: (id: string) => void;
  onDelete?: (id: string) => void;
  onEdit?: (id: string) => void;
  onPageChange?: (page: number) => void;
  statusFilter?: ActionItemStatus | null;
  onStatusFilterChange?: (status: ActionItemStatus | null) => void;
  showCreateButton?: boolean;
  onCreateClick?: () => void;
  emptyMessage?: string;
}

/**
 * Displays a list of action items with pagination, status filter, and action buttons.
 * Shows an empty state when no items exist.
 */
export default function ActionItemList({
  data,
  onComplete,
  onCancel,
  onDelete,
  onEdit,
  onPageChange,
  statusFilter,
  onStatusFilterChange,
  showCreateButton = true,
  onCreateClick,
  emptyMessage = 'No action items yet',
}: ActionItemListProps) {
  return (
    <div data-testid="action-item-list">
      {/* Filter bar */}
      {onStatusFilterChange && (
        <div
          data-testid="action-item-filter-bar"
          style={{
            display: 'flex',
            gap: '8px',
            marginBottom: 'var(--space-4)',
            alignItems: 'center',
            flexWrap: 'wrap',
          }}
        >
          <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>
            Filter:
          </span>
          {(['ALL', 'OPEN', 'DONE', 'CANCELED'] as const).map((filterValue) => {
            const isActive = filterValue === 'ALL' ? statusFilter === null : statusFilter === filterValue;
            return (
              <button
                key={filterValue}
                type="button"
                onClick={() => onStatusFilterChange(filterValue === 'ALL' ? null : filterValue as ActionItemStatus)}
                data-testid={`filter-${filterValue.toLowerCase()}`}
                style={{
                  padding: '4px 12px',
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  border: `1px solid ${isActive ? 'var(--color-primary)' : 'var(--color-border)'}`,
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: isActive ? 'var(--color-primary-muted)' : 'var(--color-bg-elevated)',
                  color: isActive ? 'var(--color-primary)' : 'var(--color-text-muted)',
                  cursor: 'pointer',
                  fontWeight: isActive ? 'var(--weight-medium)' : 'var(--weight-regular)',
                }}
              >
                {filterValue === 'ALL' ? 'All' : filterValue.charAt(0) + filterValue.slice(1).toLowerCase()}
              </button>
            );
          })}
        </div>
      )}

      {/* Items */}
      {data.content.length === 0 ? (
        <EmptyState
          message={emptyMessage}
          actionLabel={showCreateButton ? 'Create Action Item' : undefined}
          onAction={onCreateClick}
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {data.content.map((item) => (
            <ActionItemCard
              key={item.id}
              item={item}
              onComplete={onComplete}
              onCancel={onCancel}
              onDelete={onDelete}
              onEdit={onEdit}
            />
          ))}
        </div>
      )}

      {/* Pagination */}
      {data.totalPages > 1 && onPageChange && (
        <div style={{ marginTop: 'var(--space-4)' }}>
          <Pagination
            currentPage={data.page}
            totalPages={data.totalPages}
            onPageChange={onPageChange}
          />
        </div>
      )}
    </div>
  );
}
