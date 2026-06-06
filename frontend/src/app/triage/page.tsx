'use client';

import { useEffect, useState, useCallback, useRef } from 'react';
import { useStableToken } from '@/lib/useStableToken';
import { getTriageQueue, getTriageHint, snoozeTriageItem, completeActionItem, cancelActionItem } from '@/lib/api-client';
import { TriageItem, TriageQueueResponse, TriageFilters, OwnerScope, TriageItemType } from '@/types/triage';
import LoadingScreen from '@/components/LoadingScreen';
import TriageItemRow from '@/components/triage/TriageItemRow';
import TriageFilterBar from '@/components/triage/TriageFilterBar';
import TriageEmptyState from '@/components/triage/TriageEmptyState';

export default function TriagePage() {
  const { getToken, isAuthenticated, status } = useStableToken();
  const [items, setItems] = useState<TriageItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [filters, setFilters] = useState<TriageFilters>({ scope: 'ALL' });
  const [toast, setToast] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const fetchQueue = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await getTriageQueue(token, filters);
      setItems(result.items);
      setSelectedIndex(0);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load triage queue');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated, filters]);

  useEffect(() => {
    fetchQueue();
  }, [fetchQueue]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Don't intercept if user is in an input/textarea
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;

      switch (e.key) {
        case 'j':
        case 'ArrowDown':
          e.preventDefault();
          setSelectedIndex((prev) => Math.min(prev + 1, items.length - 1));
          break;
        case 'k':
        case 'ArrowUp':
          e.preventDefault();
          setSelectedIndex((prev) => Math.max(prev - 1, 0));
          break;
        case 'Home':
          e.preventDefault();
          setSelectedIndex(0);
          break;
        case 'End':
          e.preventDefault();
          setSelectedIndex(items.length - 1);
          break;
        case 'd':
          e.preventDefault();
          handleMarkDone();
          break;
        case 'c':
          e.preventDefault();
          handleCancel();
          break;
        case 's':
          e.preventDefault();
          handleSnooze(3);
          break;
      }
    };

    const container = containerRef.current;
    if (container) {
      container.addEventListener('keydown', handleKeyDown);
      return () => container.removeEventListener('keydown', handleKeyDown);
    }
  }, [items, selectedIndex]);

  const showToast = (message: string) => {
    setToast(message);
    setTimeout(() => setToast(null), 3000);
  };

  const handleMarkDone = async () => {
    const item = items[selectedIndex];
    if (!item || !item.sourceActionItemId) return;
    const token = getToken();
    if (!token) return;

    try {
      await completeActionItem(token, item.personId, item.sourceActionItemId);
      showToast('Marked as done');
      fetchQueue();
    } catch {
      showToast('Failed to mark as done');
    }
  };

  const handleCancel = async () => {
    const item = items[selectedIndex];
    if (!item || !item.sourceActionItemId) return;
    const token = getToken();
    if (!token) return;

    try {
      await cancelActionItem(token, item.personId, item.sourceActionItemId);
      showToast('Cancelled');
      fetchQueue();
    } catch {
      showToast('Failed to cancel');
    }
  };

  const handleSnooze = async (days: number) => {
    const item = items[selectedIndex];
    if (!item || !item.sourceActionItemId) return;
    const token = getToken();
    if (!token) return;

    try {
      await snoozeTriageItem(token, item.personId, item.sourceActionItemId, { days });
      showToast(`Snoozed for ${days}d`);
      fetchQueue();
    } catch {
      showToast('Failed to snooze');
    }
  };

  const handleComplete = async (item: TriageItem) => {
    if (!item.sourceActionItemId) return;
    const token = getToken();
    if (!token) return;

    try {
      await completeActionItem(token, item.personId, item.sourceActionItemId);
      showToast('Marked as done');
      fetchQueue();
    } catch {
      showToast('Failed to mark as done');
    }
  };

  const handleSnoozeItem = async (item: TriageItem, days: number) => {
    if (!item.sourceActionItemId) return;
    const token = getToken();
    if (!token) return;

    try {
      await snoozeTriageItem(token, item.personId, item.sourceActionItemId, { days });
      showToast(`Snoozed for ${days}d`);
      fetchQueue();
    } catch {
      showToast('Failed to snooze');
    }
  };

  if (status === 'loading' || loading) {
    return <LoadingScreen message="Loading triage queue" />;
  }

  return (
    <div
      ref={containerRef}
      tabIndex={0}
      data-testid="triage-page"
      style={{
        padding: 'var(--space-6)',
        maxWidth: '1000px',
        margin: '0 auto',
        fontFamily: 'var(--font-ui)',
        outline: 'none',
      }}
    >
      {/* Header */}
      <div style={{ marginBottom: 'var(--space-5)' }}>
        <h1
          data-testid="triage-title"
          style={{
            fontSize: 'var(--text-h2)',
            fontFamily: 'var(--font-heading)',
            fontWeight: 'var(--weight-bold)',
            color: 'var(--color-text-primary)',
            margin: '0 0 4px 0',
          }}
        >
          Triage Queue
        </h1>
        <p style={{
          fontSize: 'var(--text-small)',
          color: 'var(--color-text-secondary)',
          margin: 0,
          fontFamily: 'var(--font-mono)',
        }}>
          {items.length} {items.length === 1 ? 'item' : 'items'} · j/k to navigate · d/c/s for actions
        </p>
      </div>

      {/* Filter Bar */}
      <TriageFilterBar filters={filters} onFiltersChange={setFilters} />

      {/* Error State */}
      {error && (
        <div
          data-testid="triage-error"
          style={{
            padding: 'var(--space-4)',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-alert-muted)',
            color: 'var(--color-alert)',
            marginBottom: 'var(--space-4)',
            fontSize: 'var(--text-small)',
          }}
        >
          {error}
        </div>
      )}

      {/* Queue List */}
      {items.length === 0 && !error ? (
        <TriageEmptyState />
      ) : (
        <div
          data-testid="triage-list"
          role="list"
          style={{
            background: 'var(--glass-bg)',
            backdropFilter: 'var(--glass-blur)',
            border: 'var(--glass-border)',
            borderRadius: 'var(--radius-large)',
            overflow: 'hidden',
            boxShadow: 'var(--glow-primary)',
          }}
        >
          {items.map((item, index) => (
            <TriageItemRow
              key={item.id}
              item={item}
              isSelected={index === selectedIndex}
              onSelect={() => setSelectedIndex(index)}
              onComplete={() => handleComplete(item)}
              onSnooze={(days) => handleSnoozeItem(item, days)}
            />
          ))}
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div
          data-testid="triage-toast"
          style={{
            position: 'fixed',
            bottom: 'var(--space-6)',
            right: 'var(--space-6)',
            padding: 'var(--space-3) var(--space-5)',
            borderRadius: 'var(--radius-medium)',
            background: 'var(--glass-elevated-bg)',
            border: 'var(--glass-elevated-border)',
            backdropFilter: 'var(--glass-elevated-blur)',
            color: 'var(--color-primary)',
            fontFamily: 'var(--font-mono)',
            fontSize: 'var(--text-small)',
            boxShadow: 'var(--glow-primary-strong)',
            zIndex: 1000,
          }}
        >
          {toast}
        </div>
      )}
    </div>
  );
}
