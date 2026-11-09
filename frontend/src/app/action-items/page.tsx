'use client';

import { useEffect, useState, useCallback } from 'react';
import { ActionItem, ActionItemStatus, PaginatedActionItemResponse } from '@/types/action-item';
import {
  listAllActionItems,
  completeActionItemById,
  cancelActionItemById,
  deleteActionItemById,
  listPersons,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import ActionItemCard from '@/components/action-items/ActionItemCard';
import LoadingScreen from '@/components/LoadingScreen';
import EmptyState from '@/components/EmptyState';

type Scope = 'mine' | 'team';

/**
 * Cross-person "Action Items" page, split into two tabs so ownership is obvious:
 *  - Mine: items the manager owns (including person-less TODOs made from notes).
 *  - My team: items owned by reports, grouped by person name.
 * Person-less TODOs from converted notes land here, so dashboard links must resolve to it.
 * All data is userId-scoped by the backend; the split is done client-side on `ownerType`.
 */
export default function ActionItemsPage() {
  const { getToken, isAuthenticated, status } = useStableToken();

  const [items, setItems] = useState<PaginatedActionItemResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<ActionItemStatus | null>(null);
  const [scope, setScope] = useState<Scope>('mine');
  const [personNames, setPersonNames] = useState<Record<string, string>>({});

  const fetchItems = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await listAllActionItems(token, {
        status: statusFilter || undefined,
        page: 0,
        size: 100,
      });
      setItems(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load action items');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated, statusFilter]);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  // Resolve person names once so team items can show who they belong to.
  useEffect(() => {
    const token = getToken();
    if (!isAuthenticated || !token) return;
    listPersons(token, { page: 0, size: 100 })
      .then((res) => {
        const map: Record<string, string> = {};
        res.content.forEach((p) => {
          map[p.id] = p.name;
        });
        setPersonNames(map);
      })
      .catch(() => {
        /* names are a nicety; the list still works without them */
      });
  }, [getToken, isAuthenticated]);

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

  if (status === 'loading') {
    return <LoadingScreen message="Loading action items" />;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  const all = items?.content ?? [];
  const mine = all.filter((i) => i.ownerType === 'MANAGER');
  const team = all.filter((i) => i.ownerType === 'PERSON');

  const teamGroups = new Map<string, ActionItem[]>();
  team.forEach((i) => {
    const key = i.personId ?? 'unknown';
    teamGroups.set(key, [...(teamGroups.get(key) ?? []), i]);
  });
  const sortedTeamGroups = Array.from(teamGroups.entries()).sort(([a], [b]) =>
    (personNames[a] ?? '\uffff').localeCompare(personNames[b] ?? '\uffff')
  );

  const renderCard = (item: ActionItem) => (
    <ActionItemCard
      key={item.id}
      item={item}
      personName={item.personId ? personNames[item.personId] : undefined}
      onComplete={handleComplete}
      onCancel={handleCancel}
      onDelete={handleDelete}
    />
  );

  const tabs: { key: Scope; label: string; count: number }[] = [
    { key: 'mine', label: '👤 Mine', count: mine.length },
    { key: 'team', label: '👥 My team', count: team.length },
  ];

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
        What you need to do, and what your reports need to do.
      </p>

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-alert)', marginBottom: 'var(--space-4)' }}>
          {error}
        </div>
      )}

      {/* Scope tabs: whose action items */}
      <div
        role="tablist"
        aria-label="Whose action items"
        data-testid="action-items-scope-tabs"
        style={{ display: 'flex', gap: '8px', marginBottom: 'var(--space-4)', borderBottom: '1px solid var(--color-border)' }}
      >
        {tabs.map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={scope === t.key}
            onClick={() => setScope(t.key)}
            data-testid={`scope-tab-${t.key}`}
            style={{
              padding: '8px 16px',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              border: 'none',
              borderBottom: scope === t.key ? '2px solid var(--color-primary)' : '2px solid transparent',
              backgroundColor: 'transparent',
              color: scope === t.key ? 'var(--color-primary)' : 'var(--color-text-secondary)',
              cursor: 'pointer',
            }}
          >
            {t.label} ({t.count})
          </button>
        ))}
      </div>

      {/* Status filter */}
      <div
        data-testid="action-items-status-filter"
        style={{ display: 'flex', gap: '8px', marginBottom: 'var(--space-4)', flexWrap: 'wrap' }}
      >
        {statuses.map((s) => (
          <button
            key={s ?? 'all'}
            type="button"
            onClick={() => setStatusFilter(s)}
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
      ) : scope === 'mine' ? (
        mine.length === 0 ? (
          <EmptyState message="Nothing on your plate. Make an action item from a note, or add a follow-up in a 1:1." />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
            {mine.map(renderCard)}
          </div>
        )
      ) : team.length === 0 ? (
        <EmptyState message="No action items owned by your reports yet." />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)' }}>
          {sortedTeamGroups.map(([personId, groupItems]) => (
            <section key={personId} data-testid={`team-group-${personId}`}>
              <h2
                style={{
                  margin: '0 0 var(--space-3)',
                  fontSize: '16px',
                  fontFamily: 'var(--font-heading)',
                  color: 'var(--color-text-primary)',
                }}
              >
                👥 {personNames[personId] ?? 'Unknown person'} ({groupItems.length})
              </h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                {groupItems.map(renderCard)}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
