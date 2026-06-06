'use client';

import { useEffect, useState, useCallback, useRef } from 'react';
import { useStableToken } from '@/lib/useStableToken';
import {
  getTriageQueue, getTriageHint, snoozeTriageItem,
  completeActionItem, cancelActionItem, updateActionItem,
  createQuickNote, getUserSettings,
} from '@/lib/api-client';
import { TriageItem, TriageFilters } from '@/types/triage';
import LoadingScreen from '@/components/LoadingScreen';
import TriageItemRow from '@/components/triage/TriageItemRow';
import TriageFilterBar from '@/components/triage/TriageFilterBar';
import TriageEmptyState from '@/components/triage/TriageEmptyState';
import QuickPeekDrawer from '@/components/triage/QuickPeekDrawer';

export default function TriagePage() {
  const { getToken, isAuthenticated, status } = useStableToken();
  const [items, setItems] = useState<TriageItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [filters, setFilters] = useState<TriageFilters>({ scope: 'ALL' });
  const [toast, setToast] = useState<{ message: string; undoAction?: () => void } | null>(null);
  const [aiEnabled, setAiEnabled] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [dueDateInput, setDueDateInput] = useState<{ itemIndex: number; value: string } | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const toastTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const fetchQueue = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const [result, settings] = await Promise.all([
        getTriageQueue(token, filters),
        getUserSettings(token),
      ]);
      setItems(result.items);
      setAiEnabled(settings.aiEnabled);
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
        case 'Enter':
          e.preventDefault();
          setDrawerOpen(true);
          break;
        case 'Escape':
          e.preventDefault();
          setDrawerOpen(false);
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
        case 'a':
          e.preventDefault();
          handleAddTo1on1();
          break;
        case 'q':
          e.preventDefault();
          handleSaveAsNote();
          break;
        case 'r':
        case 'o':
          e.preventDefault();
          handleToggleOwner();
          break;
        case 't':
          e.preventDefault();
          setDueDateInput({ itemIndex: selectedIndex, value: '' });
          break;
      }
    };

    const container = containerRef.current;
    if (container) {
      container.addEventListener('keydown', handleKeyDown);
      return () => container.removeEventListener('keydown', handleKeyDown);
    }
  }, [items, selectedIndex]);

  // Global Cmd/Ctrl+J shortcut
  useEffect(() => {
    const handleGlobalShortcut = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'j') {
        e.preventDefault();
        containerRef.current?.focus();
      }
    };
    document.addEventListener('keydown', handleGlobalShortcut);
    return () => document.removeEventListener('keydown', handleGlobalShortcut);
  }, []);

  const showToast = (message: string, undoAction?: () => void) => {
    if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current);
    setToast({ message, undoAction });
    const timeout = undoAction ? 10000 : 3000;
    toastTimeoutRef.current = setTimeout(() => setToast(null), timeout);
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

  const handleToggleOwner = async () => {
    const item = items[selectedIndex];
    if (!item || !item.sourceActionItemId || !item.ownerType) return;
    const token = getToken();
    if (!token) return;

    const newOwner = item.ownerType === 'MANAGER' ? 'PERSON' : 'MANAGER';
    try {
      await updateActionItem(token, item.personId, item.sourceActionItemId, { ownerType: newOwner });
      showToast(`Reassigned to ${newOwner === 'MANAGER' ? 'you' : 'them'}`);
      fetchQueue();
    } catch {
      showToast('Failed to reassign');
    }
  };

  const handleSetDue = async (dateStr: string) => {
    const item = items[selectedIndex];
    if (!item || !item.sourceActionItemId || !dateStr) return;
    const token = getToken();
    if (!token) return;

    try {
      await updateActionItem(token, item.personId, item.sourceActionItemId, { dueDate: dateStr });
      showToast(`Due date set to ${dateStr}`);
      setDueDateInput(null);
      fetchQueue();
    } catch {
      showToast('Failed to set due date');
    }
  };

  const handleAddTo1on1 = async () => {
    const item = items[selectedIndex];
    if (!item) return;
    // Navigate to person's 1:1 page — the full "find or create draft" logic
    // would require complex API orchestration. For now, we create a quick note
    // prefixed with [Triage] that links to the person.
    const token = getToken();
    if (!token) return;

    try {
      await createQuickNote(token, {
        text: `[Triage] ${item.title}`,
        personId: item.personId,
        sensitive: item.sensitive,
      });
      showToast('Added to next 1:1 (via Quick Note)');
    } catch {
      showToast('Failed to add to 1:1');
    }
  };

  const handleSaveAsNote = async () => {
    const item = items[selectedIndex];
    if (!item) return;
    const token = getToken();
    if (!token) return;

    try {
      await createQuickNote(token, {
        text: `[Triage] ${item.title} — ${item.personName}`,
        personId: item.personId,
        sensitive: item.sensitive,
      });
      showToast('Saved as Quick Note');
    } catch {
      showToast('Failed to save as note');
    }
  };

  // Item-level handlers (for click actions on rows)
  const handleItemComplete = async (item: TriageItem) => {
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

  const handleItemCancel = async (item: TriageItem) => {
    if (!item.sourceActionItemId) return;
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

  const handleItemSnooze = async (item: TriageItem, days: number) => {
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

  const handleItemToggleOwner = async (item: TriageItem) => {
    if (!item.sourceActionItemId || !item.ownerType) return;
    const token = getToken();
    if (!token) return;
    const newOwner = item.ownerType === 'MANAGER' ? 'PERSON' : 'MANAGER';
    try {
      await updateActionItem(token, item.personId, item.sourceActionItemId, { ownerType: newOwner });
      showToast(`Reassigned to ${newOwner === 'MANAGER' ? 'you' : 'them'}`);
      fetchQueue();
    } catch {
      showToast('Failed to reassign');
    }
  };

  const handleItemAddTo1on1 = async (item: TriageItem) => {
    const token = getToken();
    if (!token) return;
    try {
      await createQuickNote(token, {
        text: `[Triage] ${item.title}`,
        personId: item.personId,
        sensitive: item.sensitive,
      });
      showToast('Added to next 1:1 (via Quick Note)');
    } catch {
      showToast('Failed to add to 1:1');
    }
  };

  const handleItemSaveAsNote = async (item: TriageItem) => {
    const token = getToken();
    if (!token) return;
    try {
      await createQuickNote(token, {
        text: `[Triage] ${item.title} — ${item.personName}`,
        personId: item.personId,
        sensitive: item.sensitive,
      });
      showToast('Saved as Quick Note');
    } catch {
      showToast('Failed to save as note');
    }
  };

  const handleItemSetDue = (item: TriageItem) => {
    const idx = items.indexOf(item);
    if (idx >= 0) setDueDateInput({ itemIndex: idx, value: '' });
  };

  const handleRequestHint = async (itemId: string): Promise<string | null> => {
    const token = getToken();
    if (!token) return null;
    try {
      const result = await getTriageHint(token, itemId);
      return result.hint;
    } catch {
      return null;
    }
  };

  if (status === 'loading' || loading) {
    return <LoadingScreen message="Loading triage queue" />;
  }

  const selectedItem = items[selectedIndex] || null;

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
          {items.length} {items.length === 1 ? 'item' : 'items'} · j/k navigate · Enter peek · d done · c cancel · s snooze · a 1:1 · q note · r reassign · t due
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

      {/* Inline Date Picker */}
      {dueDateInput !== null && (
        <div
          data-testid="triage-due-date-input"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-2)',
            marginBottom: 'var(--space-3)',
            padding: 'var(--space-3)',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-border-glow)',
            backgroundColor: 'var(--color-bg-surface)',
          }}
        >
          <label style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-secondary)' }}>
            Set due date:
          </label>
          <input
            type="date"
            autoFocus
            value={dueDateInput.value}
            onChange={(e) => setDueDateInput({ ...dueDateInput, value: e.target.value })}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && dueDateInput.value) {
                handleSetDue(dueDateInput.value);
              } else if (e.key === 'Escape') {
                setDueDateInput(null);
              }
            }}
            style={{
              padding: '4px 8px',
              borderRadius: 'var(--radius-small)',
              border: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
            }}
          />
          <button
            onClick={() => dueDateInput.value && handleSetDue(dueDateInput.value)}
            disabled={!dueDateInput.value}
            style={{
              padding: '4px 12px',
              borderRadius: 'var(--radius-small)',
              border: '1px solid var(--color-primary)',
              backgroundColor: 'var(--color-primary-muted)',
              color: 'var(--color-primary)',
              cursor: 'pointer',
              fontSize: 'var(--text-small)',
              fontFamily: 'var(--font-mono)',
            }}
          >
            Set
          </button>
          <button
            onClick={() => setDueDateInput(null)}
            style={{
              padding: '4px 12px',
              borderRadius: 'var(--radius-small)',
              border: '1px solid var(--color-border)',
              backgroundColor: 'transparent',
              color: 'var(--color-text-muted)',
              cursor: 'pointer',
              fontSize: 'var(--text-small)',
            }}
          >
            Cancel
          </button>
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
              onComplete={() => handleItemComplete(item)}
              onCancel={() => handleItemCancel(item)}
              onSnooze={(days) => handleItemSnooze(item, days)}
              onToggleOwner={() => handleItemToggleOwner(item)}
              onAddTo1on1={() => handleItemAddTo1on1(item)}
              onSaveAsNote={() => handleItemSaveAsNote(item)}
              onSetDue={() => handleItemSetDue(item)}
              onRequestHint={handleRequestHint}
              aiEnabled={aiEnabled}
            />
          ))}
        </div>
      )}

      {/* QuickPeek Drawer */}
      {drawerOpen && selectedItem && (
        <QuickPeekDrawer
          item={selectedItem}
          token={getToken() || ''}
          onClose={() => setDrawerOpen(false)}
        />
      )}

      {/* Toast with optional Undo */}
      {toast && (
        <div
          data-testid="triage-toast"
          style={{
            position: 'fixed',
            bottom: 'var(--space-6)',
            right: 'var(--space-6)',
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-3)',
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
          <span>{toast.message}</span>
          {toast.undoAction && (
            <button
              onClick={() => { toast.undoAction?.(); setToast(null); }}
              style={{
                padding: '2px 8px',
                borderRadius: 'var(--radius-small)',
                border: '1px solid var(--color-primary)',
                backgroundColor: 'transparent',
                color: 'var(--color-primary)',
                cursor: 'pointer',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
              }}
            >
              Undo
            </button>
          )}
        </div>
      )}
    </div>
  );
}
