import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import StreakCounter from '@/components/gamification/StreakCounter';

describe('StreakCounter', () => {
  it('should render with current streak value', () => {
    render(<StreakCounter currentStreak={5} longestStreak={10} totalOneOnOnesHeld={25} />);
    expect(screen.getByTestId('streak-counter')).toBeInTheDocument();
    expect(screen.getByTestId('streak-current-value')).toHaveTextContent('5');
  });

  it('should render longest streak value', () => {
    render(<StreakCounter currentStreak={3} longestStreak={12} totalOneOnOnesHeld={30} />);
    expect(screen.getByTestId('streak-longest-value')).toHaveTextContent('12');
  });

  it('should render total 1:1s value', () => {
    render(<StreakCounter currentStreak={3} longestStreak={12} totalOneOnOnesHeld={30} />);
    expect(screen.getByTestId('streak-total-value')).toHaveTextContent('30');
  });

  it('should show zero streak with muted color', () => {
    render(<StreakCounter currentStreak={0} longestStreak={5} totalOneOnOnesHeld={10} />);
    const currentValue = screen.getByTestId('streak-current-value');
    expect(currentValue).toHaveStyle({ color: 'var(--color-text-muted)' });
  });

  it('should show active streak with primary color', () => {
    render(<StreakCounter currentStreak={3} longestStreak={5} totalOneOnOnesHeld={10} />);
    const currentValue = screen.getByTestId('streak-current-value');
    expect(currentValue).toHaveStyle({ color: 'var(--color-primary)' });
  });

  it('should display current streak prominently', () => {
    render(<StreakCounter currentStreak={7} longestStreak={7} totalOneOnOnesHeld={20} />);
    const currentValue = screen.getByTestId('streak-current-value');
    expect(currentValue).toHaveStyle({ fontSize: '32px' });
  });

  it('should render with all zero values', () => {
    render(<StreakCounter currentStreak={0} longestStreak={0} totalOneOnOnesHeld={0} />);
    expect(screen.getByTestId('streak-current-value')).toHaveTextContent('0');
    expect(screen.getByTestId('streak-longest-value')).toHaveTextContent('0');
    expect(screen.getByTestId('streak-total-value')).toHaveTextContent('0');
  });

  it('should use monospace font for values', () => {
    render(<StreakCounter currentStreak={5} longestStreak={10} totalOneOnOnesHeld={25} />);
    const currentValue = screen.getByTestId('streak-current-value');
    expect(currentValue).toHaveStyle({ fontFamily: 'var(--font-mono)' });
  });
});
