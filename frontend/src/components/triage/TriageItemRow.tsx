'use client';

import { useState } from 'react';
import { Check, X, Clock, Lock, Sparkles, ArrowLeftRight, Calendar, MessageSquare, StickyNote } from 'lucide-react';
import { TriageItem } from '@/types/triage';

interface TriageItemRowProps {
  item: TriageItem;
  isSelected: boolean;
  onSelect: () => void;
  onComplete: () => void;
  onCancel: () => void;
  onSnooze: (days: number) => void;
  onToggleOwner: () => void;
  onAddTo1on1: () => void;
  onSaveAsNote: () => void;
  onSetDue: () => void;
  onRequestHint: (itemId: string) => Promise<string | null>;
  aiEnabled: boolean;
}

export default function TriageItemRow({
  item,
  isSelected,
  onSelect,
  onComplete,
  onCancel,
  onSnooze,
  onToggleOwner,
  onAddTo1on1,
  onSaveAsNote,
  onSetDue,
  onRequestHint,
  aiEnabled,
}: TriageItemRowProps) {
  const [hint, setHint] = useState<string | null>(null);
  const [hintLoading, setHintLoading] = useState(false);
  const [hintError, setHintError] = useState(false);
  const [showSnoozeMenu, setShowSnoozeMenu] = useState(false);

  const getIcon = () => {
    switch (item.type) {
      case 'ACTION_ITEM_OVERDUE':
        return '⚠️';
      case 'ACTION_ITEM_DUE_SOON':
        return '⏰';
      case 'STALE_ONE_ON_ONE':
        return '📅';
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
  const isActionItem = item.sourceActionItemId != null;

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
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
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
            {item.sensitive ? (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                <Lock size={12} /> [Sensitive]
              </span>
            ) : item.title}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)', marginTop: '2px', flexWrap: 'wrap' }}>
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
          {/* Workspace chip */}
          {item.workspaceName && (
            <span
              data-testid="triage-item-workspace"
              style={{
                fontSize: 'var(--text-caption)',
                color: 'var(--color-secondary)',
                padding: '1px 6px',
                borderRadius: 'var(--radius-small)',
                backgroundColor: 'var(--color-secondary-muted)',
                border: '1px solid rgba(168, 85, 247, 0.2)',
              }}
            >
              {item.workspaceName}
            </span>
          )}
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
          {/* AI Hint pill */}
          {hint && (
            <span
              data-testid="triage-hint-pill"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
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
              <Sparkles size={10} /> {hint}
            </span>
          )}
        </div>
      </div>

      {/* Right section */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)', flexShrink: 0 }}>
        {/* AI Hint button */}
        {showHintButton && (
          <button
            data-testid="triage-hint-btn"
            onClick={handleHintClick}
            disabled={hintLoading}
            title="Get AI suggestion"
            aria-label="Get AI suggestion"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '3px',
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
            <Sparkles size={12} />
            {hintLoading && <span>...</span>}
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

        {/* Inline action menu (visible when selected) */}
        {isSelected && (
          <div
            data-testid="triage-item-actions"
            style={{ display: 'flex', gap: '3px', position: 'relative' }}
          >
            {isActionItem && (
              <>
                <button
                  onClick={(e) => { e.stopPropagation(); onComplete(); }}
                  title="Mark Done (d)"
                  aria-label="Mark Done"
                  style={actionBtnStyle('var(--color-success)')}
                >
                  <Check size={12} />
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); onCancel(); }}
                  title="Cancel (c)"
                  aria-label="Cancel"
                  style={actionBtnStyle('var(--color-alert)')}
                >
                  <X size={12} />
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); setShowSnoozeMenu(!showSnoozeMenu); }}
                  title="Snooze (s)"
                  aria-label="Snooze"
                  data-testid="triage-snooze-btn"
                  style={actionBtnStyle('var(--color-warning)')}
                >
                  <Clock size={12} />
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); onToggleOwner(); }}
                  title="Reassign (r)"
                  aria-label="Reassign Owner"
                  style={actionBtnStyle('var(--color-text-secondary)')}
                >
                  <ArrowLeftRight size={12} />
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); onSetDue(); }}
                  title="Set Due (t)"
                  aria-label="Set Due Date"
                  style={actionBtnStyle('var(--color-text-secondary)')}
                >
                  <Calendar size={12} />
                </button>
              </>
            )}
            <button
              onClick={(e) => { e.stopPropagation(); onAddTo1on1(); }}
              title="Add to 1:1 (a)"
              aria-label="Add to next 1:1"
              style={actionBtnStyle('var(--color-primary)')}
            >
              <MessageSquare size={12} />
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); onSaveAsNote(); }}
              title="Save as Note (q)"
              aria-label="Save as Quick Note"
              style={actionBtnStyle('var(--color-secondary)')}
            >
              <StickyNote size={12} />
            </button>

            {/* Snooze sub-menu */}
            {showSnoozeMenu && (
              <div
                data-testid="triage-snooze-menu"
                style={{
                  position: 'absolute',
                  top: '100%',
                  right: 0,
                  marginTop: '4px',
                  padding: '4px',
                  background: 'var(--glass-elevated-bg)',
                  backdropFilter: 'var(--glass-elevated-blur)',
                  border: '1px solid var(--color-border-glow)',
                  borderRadius: 'var(--radius-medium)',
                  boxShadow: 'var(--glow-primary)',
                  zIndex: 10,
                  display: 'flex',
                  gap: '4px',
                }}
              >
                {[1, 3, 7].map((d) => (
                  <button
                    key={d}
                    onClick={(e) => { e.stopPropagation(); onSnooze(d); setShowSnoozeMenu(false); }}
                    style={{
                      padding: '4px 8px',
                      fontSize: 'var(--text-caption)',
                      fontFamily: 'var(--font-mono)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-small)',
                      background: 'transparent',
                      color: 'var(--color-warning)',
                      cursor: 'pointer',
                    }}
                  >
                    {d}d
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function actionBtnStyle(color: string): React.CSSProperties {
  return {
    display: 'flex',
    alignItems: 'center',
    background: 'none',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-small)',
    padding: '3px 5px',
    cursor: 'pointer',
    color,
  };
}
