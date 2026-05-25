import '@testing-library/jest-dom';
import { render, screen, fireEvent } from '@testing-library/react';
import SpiderWebVisualization, { LinkData } from '@/components/strategy/SpiderWebVisualization';
import { StrategyGoal } from '@/types/strategy-goal';

describe('SpiderWebVisualization', () => {
  const mockGoals: StrategyGoal[] = [
    {
      id: 'strategy-1',
      title: 'Improve Team Performance',
      description: 'Focus on delivery excellence',
      targetDate: '2026-12-31',
      status: 'ACTIVE',
      sensitive: false,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
    {
      id: 'strategy-2',
      title: 'Develop Leadership Pipeline',
      description: 'Build strong leaders',
      targetDate: '2026-12-31',
      status: 'ACTIVE',
      sensitive: false,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ];

  const mockLinks: LinkData[] = [
    {
      strategyGoalId: 'strategy-1',
      pdpGoal: {
        id: 'pdp-1',
        personId: 'person-1',
        personName: 'Alice Smith',
        title: 'Learn AWS',
        status: 'ACTIVE',
      },
    },
    {
      strategyGoalId: 'strategy-1',
      pdpGoal: {
        id: 'pdp-2',
        personId: 'person-2',
        personName: 'Bob Jones',
        title: 'Public Speaking',
        status: 'ACTIVE',
      },
    },
    {
      strategyGoalId: 'strategy-2',
      pdpGoal: {
        id: 'pdp-3',
        personId: 'person-1',
        personName: 'Alice Smith',
        title: 'Mentor Others',
        status: 'ACTIVE',
      },
    },
  ];

  it('renders empty state when no goals provided', () => {
    render(<SpiderWebVisualization goals={[]} links={[]} />);
    expect(screen.getByText('No strategy goals to visualize')).toBeInTheDocument();
  });

  it('renders spider web visualization with goals', () => {
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    expect(screen.getByTestId('spider-web-visualization')).toBeInTheDocument();
  });

  it('renders strategy goal nodes', () => {
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    expect(screen.getByTestId('node-strategy-strategy-1')).toBeInTheDocument();
    expect(screen.getByTestId('node-strategy-strategy-2')).toBeInTheDocument();
  });

  it('renders PDP goal nodes', () => {
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    expect(screen.getByTestId('node-pdp-pdp-1')).toBeInTheDocument();
    expect(screen.getByTestId('node-pdp-pdp-2')).toBeInTheDocument();
    expect(screen.getByTestId('node-pdp-pdp-3')).toBeInTheDocument();
  });

  it('renders strategy goal labels', () => {
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    expect(screen.getByText(/Improve Team/)).toBeInTheDocument();
    expect(screen.getByText(/Develop Leader/)).toBeInTheDocument();
  });

  it('renders PDP goal labels', () => {
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    expect(screen.getByText('Learn AWS')).toBeInTheDocument();
    expect(screen.getByText('Public Speaking')).toBeInTheDocument();
    expect(screen.getByText('Mentor Others')).toBeInTheDocument();
  });

  it('renders person names for PDP goals', () => {
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    const aliceElements = screen.getAllByText(/Alice/);
    expect(aliceElements.length).toBeGreaterThan(0);
    const bobElements = screen.getAllByText(/Bob/);
    expect(bobElements.length).toBeGreaterThan(0);
  });

  it('calls onNodeClick when strategy node is clicked', () => {
    const handleClick = jest.fn();
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} onNodeClick={handleClick} />);
    fireEvent.click(screen.getByTestId('node-strategy-strategy-1'));
    expect(handleClick).toHaveBeenCalledWith('strategy-1', 'strategy');
  });

  it('calls onNodeClick when PDP node is clicked', () => {
    const handleClick = jest.fn();
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} onNodeClick={handleClick} />);
    fireEvent.click(screen.getByTestId('node-pdp-pdp-1'));
    expect(handleClick).toHaveBeenCalledWith('pdp-1', 'pdp');
  });

  it('renders legend with all status types', () => {
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    expect(screen.getByText('Strategy Goal')).toBeInTheDocument();
    expect(screen.getByText('PDP Goal')).toBeInTheDocument();
    expect(screen.getByText('Achieved')).toBeInTheDocument();
    expect(screen.getByText('Sensitive')).toBeInTheDocument();
  });

  it('has proper ARIA attributes for accessibility', () => {
    render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    const svg = screen.getByRole('img');
    expect(svg).toHaveAttribute('aria-label', 'Spider web visualization showing strategy goals and linked PDP goals');
  });

  it('marks sensitive goal titles', () => {
    const sensitiveGoals: StrategyGoal[] = [
      {
        ...mockGoals[0],
        sensitive: true,
        title: 'Confidential Strategy',
      },
    ];
    render(<SpiderWebVisualization goals={sensitiveGoals} links={[]} />);
    const lockIcons = screen.getAllByText('🔒');
    expect(lockIcons.length).toBeGreaterThan(0);
  });

  it('hides sensitive content when hideSensitiveContent is true', () => {
    const sensitiveGoals: StrategyGoal[] = [
      {
        ...mockGoals[0],
        sensitive: true,
        title: 'Confidential Strategy',
      },
    ];
    render(<SpiderWebVisualization goals={sensitiveGoals} links={[]} hideSensitiveContent={true} />);
    expect(screen.getByText('•••')).toBeInTheDocument();
    expect(screen.queryByText('Confidential Strategy')).not.toBeInTheDocument();
  });

  it('truncates long titles', () => {
    const longTitleGoal: StrategyGoal = {
      ...mockGoals[0],
      title: 'This is a very long strategy goal title that should be truncated',
    };
    const { container } = render(<SpiderWebVisualization goals={[longTitleGoal]} links={[]} />);
    const textElement = container.querySelector('text');
    expect(textElement?.textContent).toContain('This is a very');
  });

  it('only renders ACTIVE strategy goals', () => {
    const goalsWithStatus: StrategyGoal[] = [
      { ...mockGoals[0], status: 'ACTIVE' },
      { ...mockGoals[1], status: 'ACHIEVED', title: 'Achieved Goal' },
    ];
    render(<SpiderWebVisualization goals={goalsWithStatus} links={[]} />);
    expect(screen.getByTestId('node-strategy-strategy-1')).toBeInTheDocument();
    expect(screen.queryByTestId('node-strategy-strategy-2')).not.toBeInTheDocument();
    expect(screen.queryByText('Achieved Goal')).not.toBeInTheDocument();
  });

  it('renders SVG with proper structure', () => {
    const { container } = render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    
    expect(container.querySelector('defs')).toBeInTheDocument();
    expect(container.querySelector('pattern')).toBeInTheDocument();
  });

  it('applies cyberpunk grid pattern to background', () => {
    const { container } = render(<SpiderWebVisualization goals={mockGoals} links={mockLinks} />);
    const rect = container.querySelector('rect[fill="url(#grid)"]');
    expect(rect).toBeInTheDocument();
  });
});
