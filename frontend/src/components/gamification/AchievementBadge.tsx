'use client';

import { Achievement, AchievementType } from '@/types/gamification';
import {
  Target,
  Hash,
  Trophy,
  CheckCircle2,
  ClipboardCheck,
  Zap,
  Award,
  Sprout,
  TreePine,
  HandHeart,
  Star,
  Flame,
  Gem,
} from 'lucide-react';
import { ComponentType } from 'react';

interface AchievementBadgeProps {
  achievement: Achievement;
}

interface LucideIconProps {
  size?: number;
  color?: string;
  strokeWidth?: number;
}

const achievementIcons: Record<AchievementType, ComponentType<LucideIconProps>> = {
  FIRST_ONE_ON_ONE: Target,
  TEN_ONE_ON_ONES: Hash,
  FIFTY_ONE_ON_ONES: Trophy,
  FIRST_ACTION_ITEM_CLOSED: CheckCircle2,
  TEN_ACTION_ITEMS_CLOSED: ClipboardCheck,
  FIFTY_ACTION_ITEMS_CLOSED: Zap,
  HUNDRED_ACTION_ITEMS_CLOSED: Award,
  FIRST_PDP_GOAL_ACHIEVED: Sprout,
  FIVE_PDP_GOALS_ACHIEVED: TreePine,
  FIRST_KUDOS_GIVEN: HandHeart,
  TEN_KUDOS_GIVEN: Star,
  STREAK_SEVEN: Flame,
  STREAK_THIRTY: Gem,
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
 * Uses Lucide outlined icons for consistent rendering across all platforms.
 * Glow effects match the achievement category color.
 */
export default function AchievementBadge({ achievement }: AchievementBadgeProps) {
  const IconComponent = achievementIcons[achievement.type];
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
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
          width: '24px',
          height: '24px',
        }}
        aria-hidden="true"
      >
        <IconComponent size={18} color={color} strokeWidth={1.5} />
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
