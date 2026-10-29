'use client';

import { useEffect, useState, useCallback } from 'react';
import { ActionItemStatus, PaginatedActionItemResponse } from '@/types/action-item';
import {
  listAllActionItems,
  completeActionItemById,
  cancelActionItemById,
  deleteActionItemById,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import ActionItemCard from '@/components/action-items/ActionItemCard';
import LoadingScreen from '@/components/LoadingScreen';
import Pagination from '@/components/Pagination';
import EmptyState from '@/components/EmptyState';

/**
 * Cross-person "Action Items" page — a manager-wide list of every action item, including
 * person-less TODOs (e.g. notes converted to a personal TODO). This is the destination the
 * dashboard "Today's Priorities" widget links person-less items to, so it must exist for
 * those links to resolve. All data is userId-scoped by the backend.
 */
export default function ActionItemsPage() {
  const { getToken, isAuthenticated, status } = useStableToken();

  const [items, setItems] = useState<PaginatedActionItemResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<ActionItemStatus | null>(null);
  const [page, setPage] = useState(0);

  const fetchItems = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await listAllActionItems(token, {
        status: statusFilter || undefined,
        page,
        size: 20,
      });
      setItems(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load action items');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated, statusFilter, page]);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  const handleComplete = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await completeActionItemById(token, id);
      fetchItems();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to complete action item');
    }
  };

  const handleCancel = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await cancelActionItemById(token, id);
      fetchItems();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to cancel action item');
    }
  };

  const handleDelete = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await deleteActionItemById(token, id);
      fetchItems();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete action item');
    }
  };

  const handleStatusFilterChange = (newStatus: ActionItemStatus | null) => {
    setStatusFilter(newStatus);
    setPage(0);
  };

  if (status === 'loading') {
    return <LoadingScreen message="Loading action items" />;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  const statuses: (ActionItemStatus | null)[] = [null, 'OPEN', 'DONE', 'CANCELED'];
  const statusLabels: Record<string, string> = {
    '': 'All',
    OPEN: 'Open',
    DONE: 'Done',
    CANCELED: 'Canceled',
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: 'var(--space-6)' }}>
      <h1
        style={{
          margin: '0 0 var(--space-2)',
          fontSize: 'var(--text-h2)',
          fontWeight: '700',
          fontFamily: 'var(--font-heading)',
          color: 'var(--color-text-primary)',
          letterSpacing: '-0.3px',
        }}
      >
        Action Items
      </h1>
      <p style={{ margin: '0 0 var(--space-6)', color: 'var(--color-text-secondary)', fontSize: 'var(--text-body)' }}>
        Every follow-up across your reports, plus your own TODOs.
      </p>

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-alert)', marginBottom: 'var(--space-4)' }}>
          {error}
        </div>
      )}

      {/* Status filter */}
      <div
        data-testid="action-items-status-filter"
        style={{ display: 'flex', gap: '8px', marginBottom: 'var(--space-4)', flexWrap: 'wrap' }}
      >
        {statuses.map((s) => (
          <button
            key={s ?? 'all'}
            type="button"
            onClick={() => handleStatusFilterChange(s)}
            data-testid={`filter-${s ?? 'all'}`}
            style={{
              padding: '4px 12px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: statusFilter === s ? '1px solid var(--color-primary)' : '1px solid var(--color-border)',
              borderRadius: 'var(--radius-full)',
              backgroundColor: statusFilter === s ? 'var(--color-primary-muted)' : 'transparent',
              color: statusFilter === s ? 'var(--color-primary)' : 'var(--color-text-secondary)',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            {statusLabels[s ?? '']}
          </button>
        ))}
      </div>

      {loading && !items ? (
        <div data-testid="action-items-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
          Loading action items...
        </div>
      ) : (
        <>
          {items && items.content.length === 0 ? (
            <EmptyState message="No action items yet — convert a note or add a follow-up from a 1:1." />
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              {items?.content.map((item) => (
                <ActionItemCard
                  key={item.id}
                  item={item}
                  onComplete={handleComplete}
                  onCancel={handleCancel}
                  onDelete={handleDelete}
                />
              ))}
            </div>
          )}

          {items && items.totalPages > 1 && (
            <div style={{ marginTop: 'var(--space-4)' }}>
              <Pagination
                currentPage={items.page}
                totalPages={items.totalPages}
                onPageChange={setPage}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}
