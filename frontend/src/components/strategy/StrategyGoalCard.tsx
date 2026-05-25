'use client';

import { StrategyGoal } from '@/types/strategy-goal';
import StrategyGoalStatusBadge from './StrategyGoalStatusBadge';
import { Users, Link2, Target } from 'lucide-react';

interface StrategyGoalCardProps {
  goal: StrategyGoal;
  onAchieve?: (id: string) => void;
  onDrop?: (id: string) => void;
  onEdit?: (id: string) => void;
  onDelete?: (id: string) => void;
  onManageLinks?: (id: string) => void;
  hideSensitiveContent?: boolean;
}

export default function StrategyGoalCard({
  goal,
  onAchieve,
  onDrop,
  onEdit,
  onDelete,
  onManageLinks,
  hideSensitiveContent = false,
}: StrategyGoalCardProps) {
  const isActive = goal.status === 'ACTIVE';
  const isSensitive = goal.sensitive;
  const shouldHideContent = isSensitive && hideSensitiveContent;

  const formattedTargetDate = goal.targetDate
    ? new Date(goal.targetDate + 'T00:00:00').toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      })
    : null;

  return (
    <div
      data-testid="strategy-goal-card"
      style={{
        padding: 'var(--space-4)',
        border: `1px solid ${isSensitive ? 'rgba(255, 214, 0, 0.3)' : 'var(--color-border)'}`,
        borderRadius: 'var(--radius-medium)',
        backgroundColor: isSensitive ? 'var(--color-warning-muted)' : 'var(--color-bg-surface)',
        transition: 'border-color 0.2s, box-shadow 0.2s',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
        <h4
          data-testid="strategy-goal-title"
          style={{
            margin: 0,
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-semibold)',
            color: goal.status === 'DROPPED' ? 'var(--color-text-muted)' : isSensitive ? 'var(--color-warning)' : 'var(--color-text-primary)',
            textDecoration: goal.status === 'DROPPED' ? 'line-through' : 'none',
            flex: 1,
            marginRight: '12px',
          }}
        >
          {shouldHideContent ? (
            <span style={{ fontStyle: 'italic' }}>Sensitive title hidden</span>
          ) : (
            goal.title
          )}
          {isSensitive && (
            <span style={{ marginLeft: '8px', fontSize: 'var(--text-caption)', color: 'var(--color-alert)' }}>
              🔒
            </span>
          )}
        </h4>
        <StrategyGoalStatusBadge status={goal.status} />
      </div>

      {goal.description && (
        <p
          data-testid="strategy-goal-description"
          style={{
            margin: '0 0 8px',
            fontSize: 'var(--text-small)',
            color: isSensitive ? 'var(--color-warning)' : 'var(--color-text-secondary)',
            lineHeight: '1.4',
          }}
        >
          {shouldHideContent ? 'Sensitive description hidden' : goal.description}
        </p>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: isActive ? '12px' : '0' }}>
        {formattedTargetDate && (
          <span
            data-testid="strategy-goal-target-date"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            <Target size={12} />
            Target: {formattedTargetDate}
          </span>
        )}
        {goal.linkedPdpGoalCount !== undefined && goal.linkedPdpGoalCount > 0 && (
          <span
            data-testid="strategy-goal-contributors"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-primary)',
              padding: '2px 8px',
              backgroundColor: 'var(--color-primary-muted)',
              borderRadius: 'var(--radius-full)',
            }}
          >
            <Users size={12} />
            {goal.linkedPdpGoalCount} contributor{goal.linkedPdpGoalCount !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      {isActive && (
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {onManageLinks && (
            <button
              type="button"
              onClick={() => onManageLinks(goal.id)}
              data-testid="strategy-goal-manage-links-btn"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-primary)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-primary-muted)',
                color: 'var(--color-primary)',
                cursor: 'pointer',
                fontWeight: 'var(--weight-medium)',
              }}
            >
              <Link2 size={12} />
              Manage Links
            </button>
          )}
          {onAchieve && (
            <button
              type="button"
              onClick={() => onAchieve(goal.id)}
              data-testid="strategy-goal-achieve-btn"
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
          {onDrop && (
            <button
              type="button"
              onClick={() => onDrop(goal.id)}
              data-testid="strategy-goal-drop-btn"
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
              data-testid="strategy-goal-edit-btn"
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
              data-testid="strategy-goal-delete-btn"
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
