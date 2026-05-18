'use client';

import { useState, useEffect, useCallback } from 'react';
import { ActionItem, CreateActionItemRequest } from '@/types/action-item';
import {
  listActionItemsByPerson,
  createActionItem,
  completeActionItem,
} from '@/lib/api-client';

interface OneOnOneActionItemsProps {
  /** Auth token for API calls */
  token: string;
  /** The person this 1:1 is with */
  personId: string;
  /** The current 1:1 entry ID (for linking new action items). Null on create page before save. */
  entryId?: string | null;
}

/**
 * Inline action items section for the 1:1 entry page.
 * Styled to match AgendaItemList — same label, input, and list patterns.
 * Shows open action items for the person, highlights items from this session,
 * and provides a quick-add input (title + optional due date).
 * When entryId is null (create page), items are created without a link.
 */
export default function OneOnOneActionItems({ token, personId, entryId }: OneOnOneActionItemsProps) {
  const [openItems, setOpenItems] = useState<ActionItem[]>([]);
  const [sessionItems, setSessionItems] = useState<ActionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Quick-add form state
  const [title, setTitle] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const fetchActionItems = useCallback(async () => {
    try {
      setLoading(true);
      // Fetch open action items for this person
      const openResult = await listActionItemsByPerson(token, personId, { status: 'OPEN', size: 50 });
      setOpenItems(openResult.content);

      // Fetch action items created in this session (only if we have an entryId)
      if (entryId) {
        const sessionResult = await listActionItemsByPerson(token, personId, { originatingEntryId: entryId, size: 50 });
        setSessionItems(sessionResult.content);
      } else {
        setSessionItems([]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load action items');
    } finally {
      setLoading(false);
    }
  }, [token, personId, entryId]);

  useEffect(() => {
    fetchActionItems();
  }, [fetchActionItems]);

  const handleQuickAdd = async (e?: React.FormEvent | React.MouseEvent) => {
    if (e) e.preventDefault();
    setFormError(null);

    if (!title.trim()) {
      setFormError('Title is required');
      return;
    }

    setIsSubmitting(true);
    try {
      const request: CreateActionItemRequest = {
        title: title.trim(),
        dueDate: dueDate || null,
        originatingEntryId: entryId || null,
      };
      await createActionItem(token, personId, request);
      setTitle('');
      setDueDate('');
      await fetchActionItems();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Failed to create action item');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleComplete = async (actionItemId: string) => {
    try {
      await completeActionItem(token, personId, actionItemId);
      await fetchActionItems();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to complete action item');
    }
  };

  // Merge session items into open items for display, avoiding duplicates
  const sessionItemIds = new Set(sessionItems.map((i) => i.id));
  const otherOpenItems = openItems.filter((i) => !sessionItemIds.has(i.id));
  // Session items that are still open
  const openSessionItems = sessionItems.filter((i) => i.status === 'OPEN');
  // Session items that are done/canceled (show them too for context)
  const completedSessionItems = sessionItems.filter((i) => i.status !== 'OPEN');

  const isOverdue = (item: ActionItem): boolean =>
    item.status === 'OPEN' && item.dueDate != null && new Date(item.dueDate) < new Date();

  const formatDueDate = (dateStr: string) =>
    new Date(dateStr + 'T00:00:00').toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
    });

  const allItems = [...openSessionItems, ...completedSessionItems, ...otherOpenItems];

  return (
    <div data-testid="one-on-one-action-items">
      <label
        style={{
          display: 'block',
          fontSize: 'var(--text-caption)',
          fontWeight: 'var(--weight-medium)',
          fontFamily: 'var(--font-mono)',
          marginBottom: '8px',
          color: 'var(--color-text-secondary)',
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
        }}
      >
        Action Items
      </label>

      {error && (
        <p
          data-testid="action-items-error"
          role="alert"
          style={{ margin: '0 0 8px', fontSize: 'var(--text-caption)', color: 'var(--color-alert)' }}
        >
          {error}
        </p>
      )}

      {/* Existing items list */}
      {loading ? (
        <div data-testid="action-items-loading" style={{ padding: '8px 0', color: 'var(--color-text-muted)', fontSize: 'var(--text-body)' }}>
          Loading action items...
        </div>
      ) : allItems.length > 0 ? (
        <ol
          style={{ listStyle: 'none', padding: 0, margin: '0 0 12px 0' }}
          aria-label="Action items"
        >
          {/* Session items */}
          {openSessionItems.length > 0 && (
            <li data-testid="session-action-items">
              <p
                style={{
                  margin: '0 0 4px',
                  padding: '4px 8px 0',
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  color: 'var(--color-primary)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                }}
              >
                From this session
              </p>
              {openSessionItems.map((item) => (
                <ActionItemRow
                  key={item.id}
                  item={item}
                  isOverdue={isOverdue(item)}
                  formatDueDate={formatDueDate}
                  onComplete={handleComplete}
                />
              ))}
            </li>
          )}

          {/* Completed session items */}
          {completedSessionItems.length > 0 && (
            <li data-testid="completed-session-items">
              {completedSessionItems.map((item) => (
                <ActionItemRow
                  key={item.id}
                  item={item}
                  isOverdue={false}
                  formatDueDate={formatDueDate}
                  onComplete={handleComplete}
                />
              ))}
            </li>
          )}

          {/* Other open items */}
          {otherOpenItems.length > 0 && (
            <li data-testid="other-open-action-items">
              {(openSessionItems.length > 0 || completedSessionItems.length > 0) && (
                <p
                  style={{
                    margin: '8px 0 4px',
                    padding: '4px 8px 0',
                    fontSize: 'var(--text-caption)',
                    fontFamily: 'var(--font-mono)',
                    color: 'var(--color-text-muted)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px',
                  }}
                >
                  Open items
                </p>
              )}
              {otherOpenItems.map((item) => (
                <ActionItemRow
                  key={item.id}
                  item={item}
                  isOverdue={isOverdue(item)}
                  formatDueDate={formatDueDate}
                  onComplete={handleComplete}
                />
              ))}
            </li>
          )}
        </ol>
      ) : (
        <p
          data-testid="no-action-items"
          style={{
            margin: '0 0 12px',
            padding: '8px 0',
            fontSize: 'var(--text-body)',
            color: 'var(--color-text-muted)',
          }}
        >
          No action items yet.
        </p>
      )}

      {/* Add new action item */}
      <div
        data-testid="quick-add-action-item-form"
        style={{ display: 'flex', gap: '8px', alignItems: 'flex-start', flexWrap: 'wrap' }}
      >
        <div style={{ flex: 1, minWidth: '180px' }}>
          <input
            id="quick-action-title"
            type="text"
            value={title}
            onChange={(e) => { setTitle(e.target.value); if (formError) setFormError(null); }}
            onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleQuickAdd(); } }}
            placeholder="Add action item..."
            data-testid="quick-action-title-input"
            aria-label="New action item title"
            aria-required="true"
            aria-invalid={!!formError}
            style={{
              width: '100%',
              height: '36px',
              padding: '0 12px',
              border: `1px solid ${formError ? 'var(--color-alert)' : 'var(--color-border)'}`,
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              boxSizing: 'border-box',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              transition: 'border-color 0.2s, box-shadow 0.2s',
            }}
          />
          {formError && (
            <p
              data-testid="quick-add-error"
              role="alert"
              style={{ margin: '4px 0 0', fontSize: 'var(--text-caption)', color: 'var(--color-alert)' }}
            >
              {formError}
            </p>
          )}
        </div>
        <input
          id="quick-action-due-date"
          type="date"
          value={dueDate}
          onChange={(e) => setDueDate(e.target.value)}
          data-testid="quick-action-due-date-input"
          aria-label="Due date"
          style={{
            height: '36px',
            padding: '0 12px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            boxSizing: 'border-box',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            transition: 'border-color 0.2s, box-shadow 0.2s',
          }}
        />
        <button
          type="button"
          onClick={handleQuickAdd}
          disabled={isSubmitting}
          data-testid="quick-add-submit-btn"
          style={{
            height: '36px',
            padding: '0 16px',
            backgroundColor: isSubmitting ? 'var(--color-secondary-muted, rgba(168, 85, 247, 0.3))' : 'var(--color-secondary)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-medium)',
            fontFamily: 'var(--font-mono)',
            cursor: isSubmitting ? 'not-allowed' : 'pointer',
            whiteSpace: 'nowrap',
            boxShadow: isSubmitting ? 'none' : '0 0 8px rgba(168, 85, 247, 0.2)',
            transition: 'box-shadow 0.2s',
          }}
        >
          {isSubmitting ? 'Adding...' : 'Add'}
        </button>
      </div>
    </div>
  );
}

/** Row for an action item — matches AgendaItemList row style */
function ActionItemRow({
  item,
  isOverdue,
  formatDueDate,
  onComplete,
}: {
  item: ActionItem;
  isOverdue: boolean;
  formatDueDate: (d: string) => string;
  onComplete: (id: string) => void;
}) {
  const isDone = item.status === 'DONE';
  const isCanceled = item.status === 'CANCELED';
  const isOpen = item.status === 'OPEN';

  return (
    <div
      data-testid="action-item-row"
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        padding: '8px',
        borderBottom: '1px solid var(--color-border-subtle)',
      }}
    >
      {/* Checkbox */}
      {isOpen ? (
        <button
          type="button"
          onClick={() => onComplete(item.id)}
          data-testid="action-item-complete-checkbox"
          aria-label={`Mark "${item.title}" as done`}
          style={{
            width: '18px',
            height: '18px',
            minWidth: '18px',
            borderRadius: '4px',
            border: '2px solid var(--color-morale-green)',
            backgroundColor: 'transparent',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 0,
            boxShadow: '0 0 6px rgba(16, 185, 129, 0.3)',
            transition: 'background-color 0.15s, box-shadow 0.15s',
          }}
          onMouseEnter={(e) => {
            const el = e.currentTarget;
            el.style.backgroundColor = 'rgba(16, 185, 129, 0.2)';
            el.style.boxShadow = '0 0 10px rgba(16, 185, 129, 0.5)';
          }}
          onMouseLeave={(e) => {
            const el = e.currentTarget;
            el.style.backgroundColor = 'transparent';
            el.style.boxShadow = '0 0 6px rgba(16, 185, 129, 0.3)';
          }}
        />
      ) : (
        <span
          style={{
            width: '18px',
            height: '18px',
            minWidth: '18px',
            borderRadius: '4px',
            border: `2px solid ${isDone ? 'var(--color-morale-green)' : 'var(--color-text-muted)'}`,
            backgroundColor: isDone ? 'var(--color-morale-green)' : 'transparent',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '11px',
            color: '#fff',
            boxShadow: isDone ? '0 0 6px rgba(16, 185, 129, 0.3)' : 'none',
          }}
        >
          {isDone ? '✓' : isCanceled ? '✕' : ''}
        </span>
      )}

      {/* Title */}
      <span
        data-testid="action-item-row-title"
        style={{
          flex: 1,
          fontSize: 'var(--text-body)',
          textDecoration: isDone ? 'line-through' : 'none',
          color: isDone || isCanceled ? 'var(--color-text-muted)' : 'var(--color-text-primary)',
        }}
      >
        {item.title}
      </span>

      {/* Due date */}
      {item.dueDate && (
        <span
          data-testid="action-item-row-due-date"
          style={{
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: isOverdue ? 'var(--color-alert)' : 'var(--color-text-muted)',
            fontWeight: isOverdue ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            whiteSpace: 'nowrap',
          }}
        >
          {isOverdue ? '⚠ ' : ''}{formatDueDate(item.dueDate)}
        </span>
      )}

      {/* Status indicator for non-open items */}
      {isCanceled && (
        <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>
          canceled
        </span>
      )}
    </div>
  );
}
