'use client';

import { PdpGoal } from '@/types/pdp-goal';
import PdpGoalStatusBadge from './PdpGoalStatusBadge';

interface PdpGoalCardProps {
  goal: PdpGoal;
  onAchieve?: (id: string) => void;
  onPause?: (id: string) => void;
  onDrop?: (id: string) => void;
  onResume?: (id: string) => void;
  onDelete?: (id: string) => void;
  onEdit?: (id: string) => void;
  onViewUpdates?: (id: string) => void;
}

/**
 * Displays a single PDP goal card with title, target date, status,
 * and action buttons for status transitions.
 */
export default function PdpGoalCard({
  goal,
  onAchieve,
  onPause,
  onDrop,
  onResume,
  onDelete,
  onEdit,
  onViewUpdates,
}: PdpGoalCardProps) {
  const isActive = goal.status === 'ACTIVE';
  const isPaused = goal.status === 'PAUSED';

  const formattedTargetDate = goal.targetDate
    ? new Date(goal.targetDate + 'T00:00:00').toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      })
    : null;

  return (
    <div
      data-testid="pdp-goal-card"
      style={{
        padding: 'var(--space-4)',
        border: `1px solid var(--color-border)`,
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
        transition: 'border-color 0.2s, box-shadow 0.2s',
      }}
    >
      {/* Header: title + status */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
        <h4
          data-testid="pdp-goal-title"
          style={{
            margin: 0,
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-semibold)',
            color: goal.status === 'DROPPED' ? 'var(--color-text-muted)' : 'var(--color-text-primary)',
            textDecoration: goal.status === 'DROPPED' ? 'line-through' : 'none',
            flex: 1,
            marginRight: '12px',
          }}
        >
          {goal.title}
        </h4>
        <PdpGoalStatusBadge status={goal.status} />
      </div>

      {/* Description */}
      {goal.description && (
        <p
          data-testid="pdp-goal-description"
          style={{
            margin: '0 0 8px',
            fontSize: 'var(--text-small)',
            color: 'var(--color-text-secondary)',
            lineHeight: '1.4',
          }}
        >
          {goal.description}
        </p>
      )}

      {/* Meta row: target date */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: (isActive || isPaused) ? '12px' : '0' }}>
        {formattedTargetDate && (
          <span
            data-testid="pdp-goal-target-date"
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            🎯 Target: {formattedTargetDate}
          </span>
        )}
        {onViewUpdates && (
          <button
            type="button"
            onClick={() => onViewUpdates(goal.id)}
            data-testid="pdp-goal-view-updates-btn"
            style={{
              padding: '2px 8px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: 'none',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'transparent',
              color: 'var(--color-primary)',
              cursor: 'pointer',
              textDecoration: 'underline',
            }}
          >
            View Updates
          </button>
        )}
      </div>

      {/* Action buttons */}
      {(isActive || isPaused) && (
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {isActive && onAchieve && (
            <button
              type="button"
              onClick={() => onAchieve(goal.id)}
              data-testid="pdp-goal-achieve-btn"
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
              🏆 Achieve
            </button>
          )}
          {isActive && onPause && (
            <button
              type="button"
              onClick={() => onPause(goal.id)}
              data-testid="pdp-goal-pause-btn"
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-morale-yellow)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'rgba(245, 158, 11, 0.1)',
                color: 'var(--color-morale-yellow)',
                cursor: 'pointer',
              }}
            >
              ⏸ Pause
            </button>
          )}
          {isPaused && onResume && (
            <button
              type="button"
              onClick={() => onResume(goal.id)}
              data-testid="pdp-goal-resume-btn"
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-primary)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-primary-muted)',
                color: 'var(--color-primary)',
                cursor: 'pointer',
              }}
            >
              ▶ Resume
            </button>
          )}
          {isActive && onDrop && (
            <button
              type="button"
              onClick={() => onDrop(goal.id)}
              data-testid="pdp-goal-drop-btn"
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
              ✕ Drop
            </button>
          )}
          {onEdit && (
            <button
              type="button"
              onClick={() => onEdit(goal.id)}
              data-testid="pdp-goal-edit-btn"
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
              onClick={() => onDelete(goal.id)}
              data-testid="pdp-goal-delete-btn"
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
