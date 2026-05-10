import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import AchievementBadge from '@/components/gamification/AchievementBadge';
import { Achievement } from '@/types/gamification';

describe('AchievementBadge', () => {
  const mockAchievement: Achievement = {
    type: 'FIRST_ONE_ON_ONE',
    unlockedAt: '2026-01-15',
    label: 'First 1:1',
    description: 'Held your first 1:1 meeting',
  };

  it('should render achievement badge', () => {
    render(<AchievementBadge achievement={mockAchievement} />);
    expect(screen.getByTestId('achievement-badge')).toBeInTheDocument();
  });

  it('should display achievement label', () => {
    render(<AchievementBadge achievement={mockAchievement} />);
    expect(screen.getByTestId('achievement-label')).toHaveTextContent('First 1:1');
  });

  it('should display achievement description', () => {
    render(<AchievementBadge achievement={mockAchievement} />);
    expect(screen.getByTestId('achievement-description')).toHaveTextContent('Held your first 1:1 meeting');
  });

  it('should render icon container', () => {
    render(<AchievementBadge achievement={mockAchievement} />);
    const iconContainer = screen.getByTestId('achievement-icon');
    expect(iconContainer).toBeInTheDocument();
    expect(iconContainer).toHaveAttribute('aria-hidden', 'true');
  });

  it('should render SVG icon inside icon container', () => {
    render(<AchievementBadge achievement={mockAchievement} />);
    const iconContainer = screen.getByTestId('achievement-icon');
    const svg = iconContainer.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('should have accessible aria-label', () => {
    render(<AchievementBadge achievement={mockAchievement} />);
    const badge = screen.getByTestId('achievement-badge');
    expect(badge).toHaveAttribute('aria-label', 'Achievement: First 1:1 - Held your first 1:1 meeting');
  });

  it('should render streak achievement with SVG icon', () => {
    const streakAchievement: Achievement = {
      type: 'STREAK_SEVEN',
      unlockedAt: '2026-03-01',
      label: '7-Week Streak',
      description: 'Maintained a 7-week 1:1 streak',
    };
    render(<AchievementBadge achievement={streakAchievement} />);
    const iconContainer = screen.getByTestId('achievement-icon');
    const svg = iconContainer.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('should render action item achievement with SVG icon', () => {
    const actionAchievement: Achievement = {
      type: 'FIRST_ACTION_ITEM_CLOSED',
      unlockedAt: '2026-02-01',
      label: 'First Close',
      description: 'Closed your first action item',
    };
    render(<AchievementBadge achievement={actionAchievement} />);
    const iconContainer = screen.getByTestId('achievement-icon');
    const svg = iconContainer.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('should render kudos achievement with SVG icon', () => {
    const kudosAchievement: Achievement = {
      type: 'TEN_KUDOS_GIVEN',
      unlockedAt: '2026-04-01',
      label: '10 Kudos',
      description: 'Gave 10 kudos to your team',
    };
    render(<AchievementBadge achievement={kudosAchievement} />);
    expect(screen.getByTestId('achievement-label')).toHaveTextContent('10 Kudos');
    const iconContainer = screen.getByTestId('achievement-icon');
    const svg = iconContainer.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('should render PDP achievement with SVG icon', () => {
    const pdpAchievement: Achievement = {
      type: 'FIVE_PDP_GOALS_ACHIEVED',
      unlockedAt: '2026-04-15',
      label: '5 Goals',
      description: 'Achieved 5 PDP goals',
    };
    render(<AchievementBadge achievement={pdpAchievement} />);
    expect(screen.getByTestId('achievement-label')).toHaveTextContent('5 Goals');
    const iconContainer = screen.getByTestId('achievement-icon');
    const svg = iconContainer.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });
});
