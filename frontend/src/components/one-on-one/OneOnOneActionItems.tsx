'use client';

import { useState, useEffect, useCallback } from 'react';
import { ActionItem, CreateActionItemRequest } from '@/types/action-item';
import {
  listActionItemsByPerson,
  createActionItem,
  completeActionItem,
} from '@/lib/api-client';
import ActionItemStatusBadge from '@/components/action-items/ActionItemStatusBadge';

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
 * Shows open action items for the person, highlights items from this session,
 * and provides a quick-add form (title + optional due date).
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

  const isOverdue = (item: ActionItem) =>
    item.status === 'OPEN' && item.dueDate && new Date(item.dueDate) < new Date();

  const formatDueDate = (dateStr: string) =>
    new Date(dateStr + 'T00:00:00').toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
    });

  return (
    <div data-testid="one-on-one-action-items" style={{ marginTop: 'var(--space-6)' }}>
      <h3
        style={{
          margin: '0 0 var(--space-4)',
          fontSize: 'var(--text-body)',
          fontWeight: 'var(--weight-semibold)',
          fontFamily: 'var(--font-heading)',
          color: 'var(--color-text-primary)',
          letterSpacing: '-0.2px',
        }}
      >
        Action Items
      </h3>

      {/* Quick-add form */}
      <div
        data-testid="quick-add-action-item-form"
        style={{
          display: 'flex',
          gap: '8px',
          alignItems: 'flex-end',
          marginBottom: 'var(--space-4)',
          flexWrap: 'wrap',
        }}
      >
        <div style={{ flex: 1, minWidth: '200px' }}>
          <label
            htmlFor="quick-action-title"
            style={{
              display: 'block',
              fontSize: 'var(--text-caption)',
              color: 'var(--color-text-muted)',
              marginBottom: '4px',
              fontFamily: 'var(--font-mono)',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
            }}
          >
            New Action Item
          </label>
          <input
            id="quick-action-title"
            type="text"
            value={title}
            onChange={(e) => { setTitle(e.target.value); if (formError) setFormError(null); }}
            onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleQuickAdd(); } }}
            placeholder="What needs to be done?"
            data-testid="quick-action-title-input"
            aria-required="true"
            aria-invalid={!!formError}
            style={{
              width: '100%',
              padding: '8px 12px',
              border: `1px solid ${formError ? 'var(--color-alert)' : 'var(--color-border)'}`,
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
            }}
          />
        </div>
        <div style={{ minWidth: '140px' }}>
          <label
            htmlFor="quick-action-due-date"
            style={{
              display: 'block',
              fontSize: 'var(--text-caption)',
              color: 'var(--color-text-muted)',
              marginBottom: '4px',
              fontFamily: 'var(--font-mono)',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
            }}
          >
            Due Date
          </label>
          <input
            id="quick-action-due-date"
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            data-testid="quick-action-due-date-input"
            style={{
              width: '100%',
              padding: '8px 12px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
            }}
          />
        </div>
        <button
          type="button"
          onClick={handleQuickAdd}
          disabled={isSubmitting}
          data-testid="quick-add-submit-btn"
          style={{
            padding: '8px 16px',
            backgroundColor: isSubmitting ? 'var(--color-primary-muted)' : 'var(--color-primary)',
            color: isSubmitting ? 'var(--color-primary)' : 'var(--color-bg-base)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-semibold)',
            fontFamily: 'var(--font-mono)',
            cursor: isSubmitting ? 'not-allowed' : 'pointer',
            boxShadow: isSubmitting ? 'none' : 'var(--glow-primary)',
            whiteSpace: 'nowrap',
          }}
        >
          {isSubmitting ? 'Adding...' : '+ Add'}
        </button>
      </div>

      {formError && (
        <p
          data-testid="quick-add-error"
          role="alert"
          style={{
            margin: '0 0 var(--space-3)',
            fontSize: 'var(--text-caption)',
            color: 'var(--color-alert)',
          }}
        >
          {formError}
        </p>
      )}

      {error && (
        <p
          data-testid="action-items-error"
          style={{
            color: 'var(--color-alert)',
            fontSize: 'var(--text-small)',
            marginBottom: 'var(--space-3)',
          }}
        >
          {error}
        </p>
      )}

      {loading ? (
        <div data-testid="action-items-loading" style={{ color: 'var(--color-text-muted)', fontSize: 'var(--text-small)' }}>
          Loading action items...
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {/* Session items (from this 1:1) */}
          {openSessionItems.length > 0 && (
            <div data-testid="session-action-items">
              <p
                style={{
                  margin: '0 0 6px',
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
                  highlight
                />
              ))}
            </div>
          )}

          {/* Completed session items */}
          {completedSessionItems.length > 0 && (
            <div data-testid="completed-session-items">
              {completedSessionItems.map((item) => (
                <ActionItemRow
                  key={item.id}
                  item={item}
                  isOverdue={false}
                  formatDueDate={formatDueDate}
                  onComplete={handleComplete}
                  highlight
                />
              ))}
            </div>
          )}

          {/* Other open items for this person */}
          {otherOpenItems.length > 0 && (
            <div data-testid="other-open-action-items">
              <p
                style={{
                  margin: `${openSessionItems.length > 0 || completedSessionItems.length > 0 ? 'var(--space-3)' : '0'} 0 6px`,
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  color: 'var(--color-text-muted)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                }}
              >
                Open items for this person
              </p>
              {otherOpenItems.map((item) => (
                <ActionItemRow
                  key={item.id}
                  item={item}
                  isOverdue={isOverdue(item)}
                  formatDueDate={formatDueDate}
                  onComplete={handleComplete}
                  highlight={false}
                />
              ))}
            </div>
          )}

          {/* Empty state */}
          {openSessionItems.length === 0 && completedSessionItems.length === 0 && otherOpenItems.length === 0 && (
            <p
              data-testid="no-action-items"
              style={{
                margin: 0,
                fontSize: 'var(--text-small)',
                color: 'var(--color-text-muted)',
                fontStyle: 'italic',
              }}
            >
              No action items yet — use the form above to add one.
            </p>
          )}
        </div>
      )}
    </div>
  );
}

/** Compact inline row for an action item with checkbox-style complete */
function ActionItemRow({
  item,
  isOverdue,
  formatDueDate,
  onComplete,
  highlight,
}: {
  item: ActionItem;
  isOverdue: boolean;
  formatDueDate: (d: string) => string;
  onComplete: (id: string) => void;
  highlight: boolean;
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
        padding: '8px 12px',
        borderRadius: 'var(--radius-medium)',
        border: `1px solid ${isOverdue ? 'var(--color-alert)' : highlight ? 'var(--color-primary-muted)' : 'var(--color-border)'}`,
        backgroundColor: highlight ? 'rgba(6, 182, 212, 0.05)' : 'var(--color-bg-surface)',
        boxShadow: isOverdue ? 'var(--glow-alert)' : 'none',
        marginBottom: '4px',
        transition: 'border-color 0.2s',
      }}
    >
      {/* Checkbox / complete button */}
      {isOpen ? (
        <button
          type="button"
          onClick={() => onComplete(item.id)}
          data-testid="action-item-complete-checkbox"
          aria-label={`Mark "${item.title}" as done`}
          style={{
            width: '20px',
            height: '20px',
            minWidth: '20px',
            borderRadius: '4px',
            border: '2px solid var(--color-morale-green)',
            backgroundColor: 'transparent',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 0,
            transition: 'background-color 0.15s',
          }}
          onMouseEnter={(e) => { (e.target as HTMLElement).style.backgroundColor = 'rgba(16, 185, 129, 0.2)'; }}
          onMouseLeave={(e) => { (e.target as HTMLElement).style.backgroundColor = 'transparent'; }}
        >
          {/* Empty — shows as unchecked */}
        </button>
      ) : (
        <span
          style={{
            width: '20px',
            height: '20px',
            minWidth: '20px',
            borderRadius: '4px',
            border: `2px solid ${isDone ? 'var(--color-morale-green)' : 'var(--color-text-muted)'}`,
            backgroundColor: isDone ? 'var(--color-morale-green)' : 'transparent',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '12px',
            color: '#fff',
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
          color: isDone || isCanceled ? 'var(--color-text-muted)' : 'var(--color-text-primary)',
          textDecoration: isDone ? 'line-through' : 'none',
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

      {/* Status badge for non-open items */}
      {!isOpen && <ActionItemStatusBadge status={item.status} />}
    </div>
  );
}
