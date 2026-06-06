'use client';

import { useState } from 'react';
import { TriageItem } from '@/types/triage';

interface TriageItemRowProps {
  item: TriageItem;
  isSelected: boolean;
  onSelect: () => void;
  onComplete: () => void;
  onSnooze: (days: number) => void;
  onRequestHint: (itemId: string) => Promise<string | null>;
  aiEnabled: boolean;
}

export default function TriageItemRow({
  item,
  isSelected,
  onSelect,
  onComplete,
  onSnooze,
  onRequestHint,
  aiEnabled,
}: TriageItemRowProps) {
  const [hint, setHint] = useState<string | null>(null);
  const [hintLoading, setHintLoading] = useState(false);
  const [hintError, setHintError] = useState(false);

  const getIcon = () => {
    switch (item.type) {
      case 'ACTION_ITEM_OVERDUE':
      case 'ACTION_ITEM_DUE_SOON':
        return '☐';
      case 'STALE_ONE_ON_ONE':
        return '⏰';
      case 'UPCOMING_ANNIVERSARY':
        return '🎉';
    }
  };

  const getStatusBadge = () => {
    if (item.daysOverdue && item.daysOverdue > 0) {
      return {
        text: `${item.daysOverdue}d overdue`,
        color: 'var(--color-alert)',
        bg: 'var(--color-alert-muted)',
      };
    }
    if (item.daysUntilDue !== null && item.daysUntilDue !== undefined) {
      if (item.type === 'UPCOMING_ANNIVERSARY') {
        return {
          text: `in ${item.daysUntilDue}d`,
          color: 'var(--color-primary)',
          bg: 'var(--color-primary-muted)',
        };
      }
      return {
        text: `due in ${item.daysUntilDue}d`,
        color: 'var(--color-warning)',
        bg: 'var(--color-warning-muted)',
      };
    }
    if (item.type === 'STALE_ONE_ON_ONE') {
      return {
        text: 'stale',
        color: 'var(--color-warning)',
        bg: 'var(--color-warning-muted)',
      };
    }
    return null;
  };

  const handleHintClick = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (hintLoading || hint) return;

    setHintLoading(true);
    setHintError(false);
    try {
      const result = await onRequestHint(item.id);
      setHint(result);
      if (!result) setHintError(true);
    } catch {
      setHintError(true);
    } finally {
      setHintLoading(false);
    }
  };

  const badge = getStatusBadge();
  const showHintButton = aiEnabled && !item.sensitive && !hint && !hintError;

  return (
    <div
      role="listitem"
      data-testid={`triage-item-${item.id}`}
      onClick={onSelect}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 'var(--space-3)',
        padding: 'var(--space-3) var(--space-4)',
        borderBottom: '1px solid var(--color-border-subtle)',
        cursor: 'pointer',
        transition: 'background-color 0.15s, box-shadow 0.15s',
        backgroundColor: isSelected ? 'var(--color-primary-muted)' : 'transparent',
        boxShadow: isSelected ? 'inset 0 0 0 1px var(--color-border-glow)' : 'none',
      }}
    >
      {/* Left icon */}
      <span
        style={{
          fontSize: '16px',
          width: '24px',
          textAlign: 'center',
          flexShrink: 0,
        }}
        aria-hidden="true"
      >
        {getIcon()}
      </span>

      {/* Body */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-2)',
          }}
        >
          <span
            data-testid="triage-item-title"
            style={{
              fontSize: 'var(--text-body)',
              color: 'var(--color-text-primary)',
              fontWeight: 'var(--weight-medium)',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
          >
            {item.sensitive ? '🔒 [Sensitive]' : item.title}
          </span>
        </div>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-2)',
            marginTop: '2px',
          }}
        >
          {/* Person chip */}
          <span
            data-testid="triage-item-person"
            style={{
              fontSize: 'var(--text-caption)',
              color: 'var(--color-text-secondary)',
              padding: '1px 6px',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'var(--color-bg-elevated)',
              border: '1px solid var(--color-border)',
            }}
          >
            {item.personName}
          </span>
          {/* Owner type */}
          {item.ownerType && (
            <span
              style={{
                fontSize: 'var(--text-caption)',
                color: 'var(--color-text-muted)',
                fontFamily: 'var(--font-mono)',
              }}
            >
              {item.ownerType === 'MANAGER' ? '→ you' : '→ them'}
            </span>
          )}
          {/* AI Hint pill (displayed after generation) */}
          {hint && (
            <span
              data-testid="triage-hint-pill"
              style={{
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                padding: '1px 8px',
                borderRadius: 'var(--radius-full)',
                border: '1px solid var(--color-primary)',
                color: 'var(--color-primary)',
                backgroundColor: 'var(--color-primary-muted)',
                maxWidth: '200px',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
              title={hint}
            >
              ✦ {hint}
            </span>
          )}
        </div>
      </div>

      {/* Right section: badge + hint button + actions */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 'var(--space-2)',
          flexShrink: 0,
        }}
      >
        {/* AI Hint button */}
        {showHintButton && (
          <button
            data-testid="triage-hint-btn"
            onClick={handleHintClick}
            disabled={hintLoading}
            title="Get AI suggestion"
            aria-label="Get AI suggestion"
            style={{
              background: 'none',
              border: '1px solid var(--color-primary)',
              borderRadius: 'var(--radius-full)',
              padding: '2px 8px',
              cursor: hintLoading ? 'wait' : 'pointer',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-primary)',
              opacity: hintLoading ? 0.6 : 1,
              animation: hintLoading ? 'pulse 1.5s infinite' : 'none',
              transition: 'opacity 0.2s, box-shadow 0.2s',
            }}
          >
            {hintLoading ? '✦ ...' : '✦'}
          </button>
        )}

        {badge && (
          <span
            data-testid="triage-item-badge"
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              fontWeight: 'var(--weight-medium)',
              padding: '2px 8px',
              borderRadius: 'var(--radius-full)',
              color: badge.color,
              backgroundColor: badge.bg,
            }}
          >
            {badge.text}
          </span>
        )}

        {/* Inline action buttons (visible on hover/selected) */}
        {isSelected && item.sourceActionItemId && (
          <div
            data-testid="triage-item-actions"
            style={{
              display: 'flex',
              gap: '4px',
            }}
          >
            <button
              onClick={(e) => { e.stopPropagation(); onComplete(); }}
              title="Mark Done (d)"
              aria-label="Mark Done"
              style={{
                background: 'none',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-small)',
                padding: '2px 6px',
                cursor: 'pointer',
                fontSize: 'var(--text-caption)',
                color: 'var(--color-success)',
              }}
            >
              ✓
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); onSnooze(1); }}
              title="Snooze 1d"
              aria-label="Snooze 1 day"
              style={{
                background: 'none',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-small)',
                padding: '2px 6px',
                cursor: 'pointer',
                fontSize: 'var(--text-caption)',
                color: 'var(--color-warning)',
              }}
            >
              💤1d
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); onSnooze(3); }}
              title="Snooze 3d"
              aria-label="Snooze 3 days"
              style={{
                background: 'none',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-small)',
                padding: '2px 6px',
                cursor: 'pointer',
                fontSize: 'var(--text-caption)',
                color: 'var(--color-warning)',
              }}
            >
              💤3d
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
