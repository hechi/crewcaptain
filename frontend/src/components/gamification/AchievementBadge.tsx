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
 * Displays a single achievement badge with icon, label, description, and progress.
 * Unlocked achievements show in full color with glow.
 * Locked achievements are dimmed with a progress indicator (current/target).
 */
export default function AchievementBadge({ achievement }: AchievementBadgeProps) {
  const IconComponent = achievementIcons[achievement.type];
  const color = achievementColors[achievement.type];
  const isUnlocked = achievement.unlocked;
  const progressText = `${Math.min(achievement.current, achievement.target)}/${achievement.target}`;

  return (
    <div
      data-testid="achievement-badge"
      aria-label={
        isUnlocked
          ? `Achievement unlocked: ${achievement.label} - ${achievement.description}`
          : `Achievement locked: ${achievement.label} - ${progressText} - ${achievement.description}`
      }
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        padding: '10px 14px',
        borderRadius: 'var(--radius-medium)',
        border: `1px solid ${isUnlocked ? color : 'var(--color-border)'}`,
        backgroundColor: 'var(--color-bg-surface)',
        boxShadow: isUnlocked ? `0 0 8px ${color}33` : 'none',
        opacity: isUnlocked ? 1 : 0.6,
        transition: 'box-shadow 0.2s, border-color 0.2s, opacity 0.2s',
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
        <IconComponent
          size={18}
          color={isUnlocked ? color : 'var(--color-text-muted)'}
          strokeWidth={1.5}
        />
      </span>

      {/* Text */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', minWidth: 0, flex: 1 }}>
        <span
          data-testid="achievement-label"
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 'var(--text-caption)',
            fontWeight: 'var(--weight-semibold)',
            color: isUnlocked ? color : 'var(--color-text-muted)',
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

      {/* Progress indicator */}
      <span
        data-testid="achievement-progress"
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: '11px',
          fontWeight: 'var(--weight-medium)',
          color: isUnlocked ? color : 'var(--color-text-secondary)',
          whiteSpace: 'nowrap',
          flexShrink: 0,
        }}
      >
        {progressText}
      </span>
    </div>
  );
}
