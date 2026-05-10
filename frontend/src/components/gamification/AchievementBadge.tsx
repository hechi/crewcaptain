'use client';

import { Achievement, AchievementType } from '@/types/gamification';

interface AchievementBadgeProps {
  achievement: Achievement;
}

const achievementIcons: Record<AchievementType, string> = {
  FIRST_ONE_ON_ONE: '🎯',
  TEN_ONE_ON_ONES: '🔟',
  FIFTY_ONE_ON_ONES: '🏆',
  FIRST_ACTION_ITEM_CLOSED: '✅',
  TEN_ACTION_ITEMS_CLOSED: '📋',
  FIFTY_ACTION_ITEMS_CLOSED: '⚡',
  HUNDRED_ACTION_ITEMS_CLOSED: '💯',
  FIRST_PDP_GOAL_ACHIEVED: '🌱',
  FIVE_PDP_GOALS_ACHIEVED: '🌳',
  FIRST_KUDOS_GIVEN: '👏',
  TEN_KUDOS_GIVEN: '🌟',
  STREAK_SEVEN: '🔥',
  STREAK_THIRTY: '💎',
};

const achievementColors: Record<AchievementType, string> = {
  FIRST_ONE_ON_ONE: 'var(--color-primary)',
  TEN_ONE_ON_ONES: 'var(--color-primary)',
  FIFTY_ONE_ON_ONES: 'var(--color-warning)',
  FIRST_ACTION_ITEM_CLOSED: 'var(--color-success)',
  TEN_ACTION_ITEMS_CLOSED: 'var(--color-success)',
  FIFTY_ACTION_ITEMS_CLOSED: 'var(--color-success)',
  HUNDRED_ACTION_ITEMS_CLOSED: 'var(--color-warning)',
  FIRST_PDP_GOAL_ACHIEVED: 'var(--color-secondary)',
  FIVE_PDP_GOALS_ACHIEVED: 'var(--color-secondary)',
  FIRST_KUDOS_GIVEN: 'var(--color-warning)',
  TEN_KUDOS_GIVEN: 'var(--color-warning)',
  STREAK_SEVEN: 'var(--color-alert)',
  STREAK_THIRTY: 'var(--color-primary)',
};

/**
 * Displays a single achievement badge with icon, label, and description.
 * Uses glow effects matching the achievement category color.
 */
export default function AchievementBadge({ achievement }: AchievementBadgeProps) {
  const icon = achievementIcons[achievement.type];
  const color = achievementColors[achievement.type];

  return (
    <div
      data-testid="achievement-badge"
      aria-label={`Achievement: ${achievement.label} - ${achievement.description}`}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        padding: '10px 14px',
        borderRadius: 'var(--radius-medium)',
        border: `1px solid ${color}`,
        backgroundColor: 'var(--color-bg-surface)',
        boxShadow: `0 0 8px ${color}33`,
        transition: 'box-shadow 0.2s, border-color 0.2s',
      }}
    >
      {/* Icon */}
      <span
        data-testid="achievement-icon"
        style={{
          fontSize: '20px',
          lineHeight: 1,
          flexShrink: 0,
        }}
        role="img"
        aria-hidden="true"
      >
        {icon}
      </span>

      {/* Text */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', minWidth: 0 }}>
        <span
          data-testid="achievement-label"
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 'var(--text-caption)',
            fontWeight: 'var(--weight-semibold)',
            color: color,
            letterSpacing: '0.3px',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {achievement.label}
        </span>
        <span
          data-testid="achievement-description"
          style={{
            fontFamily: 'var(--font-ui)',
            fontSize: '11px',
            color: 'var(--color-text-muted)',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {achievement.description}
        </span>
      </div>
    </div>
  );
}
