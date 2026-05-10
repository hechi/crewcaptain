import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ActivityHeatmap from '@/components/gamification/ActivityHeatmap';
import { ActivityDay } from '@/types/gamification';

describe('ActivityHeatmap', () => {
  it('should render empty state when no days provided', () => {
    render(<ActivityHeatmap days={[]} />);
    expect(screen.getByTestId('activity-heatmap-empty')).toBeInTheDocument();
    expect(screen.getByTestId('activity-heatmap-empty')).toHaveTextContent('No activity data available');
  });

  it('should render heatmap with activity data', () => {
    const days: ActivityDay[] = [
      { date: '2026-05-01', count: 1 },
      { date: '2026-05-02', count: 0 },
      { date: '2026-05-03', count: 3 },
    ];
    render(<ActivityHeatmap days={days} />);
    expect(screen.getByTestId('activity-heatmap')).toBeInTheDocument();
  });

  it('should render cells for each day', () => {
    const days: ActivityDay[] = [
      { date: '2026-05-01', count: 1 },
      { date: '2026-05-02', count: 0 },
      { date: '2026-05-03', count: 2 },
    ];
    render(<ActivityHeatmap days={days} />);
    const cells = screen.getAllByTestId('heatmap-cell');
    expect(cells.length).toBe(3);
  });

  it('should render legend', () => {
    const days: ActivityDay[] = [{ date: '2026-05-01', count: 1 }];
    render(<ActivityHeatmap days={days} />);
    expect(screen.getByTestId('heatmap-legend')).toBeInTheDocument();
  });

  it('should show title with activity count on cells', () => {
    const days: ActivityDay[] = [{ date: '2026-05-01', count: 3 }];
    render(<ActivityHeatmap days={days} />);
    const cells = screen.getAllByTestId('heatmap-cell');
    expect(cells[0]).toHaveAttribute('title', '2026-05-01: 3 activities');
  });

  it('should show singular activity text for count of 1', () => {
    const days: ActivityDay[] = [{ date: '2026-05-01', count: 1 }];
    render(<ActivityHeatmap days={days} />);
    const cells = screen.getAllByTestId('heatmap-cell');
    expect(cells[0]).toHaveAttribute('title', '2026-05-01: 1 activity');
  });

  it('should have accessible aria-label on cells', () => {
    const days: ActivityDay[] = [{ date: '2026-05-01', count: 2 }];
    render(<ActivityHeatmap days={days} />);
    const cells = screen.getAllByTestId('heatmap-cell');
    expect(cells[0]).toHaveAttribute('aria-label', '2026-05-01: 2 activities');
  });

  it('should render legend with Less and More labels', () => {
    const days: ActivityDay[] = [{ date: '2026-05-01', count: 1 }];
    render(<ActivityHeatmap days={days} />);
    expect(screen.getByText('Less')).toBeInTheDocument();
    expect(screen.getByText('More')).toBeInTheDocument();
  });

  it('should handle large datasets', () => {
    const days: ActivityDay[] = Array.from({ length: 90 }, (_, i) => ({
      date: `2026-02-${String(i + 1).padStart(2, '0')}`,
      count: i % 3,
    }));
    render(<ActivityHeatmap days={days} />);
    expect(screen.getByTestId('activity-heatmap')).toBeInTheDocument();
  });
});
