import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import AchievementBadge from '@/components/gamification/AchievementBadge';
import { Achievement } from '@/types/gamification';

describe('AchievementBadge', () => {
  const unlockedAchievement: Achievement = {
    type: 'FIRST_ONE_ON_ONE',
    unlocked: true,
    label: 'First 1:1',
    description: 'Hold your first 1:1 meeting',
    current: 5,
    target: 1,
  };

  const lockedAchievement: Achievement = {
    type: 'TEN_ACTION_ITEMS_CLOSED',
    unlocked: false,
    label: '10 Closed',
    description: 'Close 10 action items',
    current: 7,
    target: 10,
  };

  it('should render achievement badge', () => {
    render(<AchievementBadge achievement={unlockedAchievement} />);
    expect(screen.getByTestId('achievement-badge')).toBeInTheDocument();
  });

  it('should display achievement label', () => {
    render(<AchievementBadge achievement={unlockedAchievement} />);
    expect(screen.getByTestId('achievement-label')).toHaveTextContent('First 1:1');
  });

  it('should display achievement description', () => {
    render(<AchievementBadge achievement={unlockedAchievement} />);
    expect(screen.getByTestId('achievement-description')).toHaveTextContent('Hold your first 1:1 meeting');
  });

  it('should render SVG icon inside icon container', () => {
    render(<AchievementBadge achievement={unlockedAchievement} />);
    const iconContainer = screen.getByTestId('achievement-icon');
    const svg = iconContainer.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('should show progress indicator with current/target', () => {
    render(<AchievementBadge achievement={lockedAchievement} />);
    expect(screen.getByTestId('achievement-progress')).toHaveTextContent('7/10');
  });

  it('should show capped progress for unlocked achievements', () => {
    render(<AchievementBadge achievement={unlockedAchievement} />);
    expect(screen.getByTestId('achievement-progress')).toHaveTextContent('1/1');
  });

  it('should have full opacity when unlocked', () => {
    render(<AchievementBadge achievement={unlockedAchievement} />);
    const badge = screen.getByTestId('achievement-badge');
    expect(badge).toHaveStyle({ opacity: 1 });
  });

  it('should have reduced opacity when locked', () => {
    render(<AchievementBadge achievement={lockedAchievement} />);
    const badge = screen.getByTestId('achievement-badge');
    expect(badge).toHaveStyle({ opacity: 0.6 });
  });

  it('should have glow when unlocked', () => {
    render(<AchievementBadge achievement={unlockedAchievement} />);
    const badge = screen.getByTestId('achievement-badge');
    expect(badge.style.boxShadow).not.toBe('none');
  });

  it('should not have glow when locked', () => {
    render(<AchievementBadge achievement={lockedAchievement} />);
    const badge = screen.getByTestId('achievement-badge');
    expect(badge).toHaveStyle({ boxShadow: 'none' });
  });

  it('should have accessible aria-label for unlocked achievement', () => {
    render(<AchievementBadge achievement={unlockedAchievement} />);
    const badge = screen.getByTestId('achievement-badge');
    expect(badge).toHaveAttribute('aria-label', 'Achievement unlocked: First 1:1 - Hold your first 1:1 meeting');
  });

  it('should have accessible aria-label for locked achievement with progress', () => {
    render(<AchievementBadge achievement={lockedAchievement} />);
    const badge = screen.getByTestId('achievement-badge');
    expect(badge).toHaveAttribute('aria-label', 'Achievement locked: 10 Closed - 7/10 - Close 10 action items');
  });

  it('should render different icon types correctly', () => {
    const streakAchievement: Achievement = {
      type: 'STREAK_SEVEN',
      unlocked: false,
      label: '7-Week Streak',
      description: 'Maintain a 7-week 1:1 streak',
      current: 3,
      target: 7,
    };
    render(<AchievementBadge achievement={streakAchievement} />);
    const iconContainer = screen.getByTestId('achievement-icon');
    const svg = iconContainer.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(screen.getByTestId('achievement-progress')).toHaveTextContent('3/7');
  });
});
