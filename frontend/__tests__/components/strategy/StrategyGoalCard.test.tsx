import { render, screen, fireEvent } from '@testing-library/react';
import StrategyGoalCard from '@/components/strategy/StrategyGoalCard';
import { StrategyGoal } from '@/types/strategy-goal';

describe('StrategyGoalCard', () => {
  const mockGoal: StrategyGoal = {
    id: 'test-id-123',
    title: 'Test Strategy Goal',
    description: 'This is a test description',
    targetDate: '2026-12-31',
    status: 'ACTIVE',
    sensitive: false,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };

  const mockHandlers = {
    onAchieve: jest.fn(),
    onDrop: jest.fn(),
    onEdit: jest.fn(),
    onDelete: jest.fn(),
    onManageLinks: jest.fn(),
  };

  it('renders strategy goal title', () => {
    render(<StrategyGoalCard goal={mockGoal} {...mockHandlers} />);
    expect(screen.getByTestId('strategy-goal-title')).toHaveTextContent('Test Strategy Goal');
  });

  it('renders strategy goal description', () => {
    render(<StrategyGoalCard goal={mockGoal} {...mockHandlers} />);
    expect(screen.getByTestId('strategy-goal-description')).toHaveTextContent('This is a test description');
  });

  it('renders target date when provided', () => {
    render(<StrategyGoalCard goal={mockGoal} {...mockHandlers} />);
    expect(screen.getByTestId('strategy-goal-target-date')).toBeInTheDocument();
  });

  it('shows sensitive indicator when sensitive=true', () => {
    const sensitiveGoal = { ...mockGoal, sensitive: true };
    render(<StrategyGoalCard goal={sensitiveGoal} {...mockHandlers} />);
    expect(screen.getByText('🔒')).toBeInTheDocument();
  });

  it('masks sensitive title when hideSensitiveContent is true', () => {
    const sensitiveGoal = { ...mockGoal, sensitive: true };
    render(<StrategyGoalCard goal={sensitiveGoal} hideSensitiveContent={true} {...mockHandlers} />);
    expect(screen.getByTestId('strategy-goal-title')).toHaveTextContent('Sensitive title hidden');
  });

  it('masks sensitive description when hideSensitiveContent is true', () => {
    const sensitiveGoal = { ...mockGoal, sensitive: true };
    render(<StrategyGoalCard goal={sensitiveGoal} hideSensitiveContent={true} {...mockHandlers} />);
    expect(screen.getByTestId('strategy-goal-description')).toHaveTextContent('Sensitive description hidden');
  });

  it('shows lock icon for sensitive goals', () => {
    const sensitiveGoal = { ...mockGoal, sensitive: true };
    render(<StrategyGoalCard goal={sensitiveGoal} {...mockHandlers} />);
    // lock icon rendered as emoji next to title
    expect(screen.getByText('🔒')).toBeInTheDocument();
  });

  it('applies warning styling to sensitive goals', () => {
    const sensitiveGoal = { ...mockGoal, sensitive: true };
    const { getByTestId } = render(<StrategyGoalCard goal={sensitiveGoal} {...mockHandlers} />);
    const card = getByTestId('strategy-goal-card');
    // border should include the warning rgba color and background should reference warning muted variable
    expect(card.style.border).toContain('rgba(255, 214, 0, 0.3)');
    expect(card.style.backgroundColor).toContain('--color-warning-muted');
  });

  it('does not show action buttons when goal is ACHIEVED', () => {
    const achievedGoal = { ...mockGoal, status: 'ACHIEVED' };
    render(<StrategyGoalCard goal={achievedGoal} {...mockHandlers} />);
    expect(screen.queryByTestId('strategy-goal-achieve-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('strategy-goal-drop-btn')).not.toBeInTheDocument();
  });

  it('does not show action buttons when goal is DROPPED', () => {
    const droppedGoal = { ...mockGoal, status: 'DROPPED' };
    render(<StrategyGoalCard goal={droppedGoal} {...mockHandlers} />);
    expect(screen.queryByTestId('strategy-goal-achieve-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('strategy-goal-drop-btn')).not.toBeInTheDocument();
  });

  it('calls onAchieve when achieve button is clicked', () => {
    render(<StrategyGoalCard goal={mockGoal} {...mockHandlers} />);
    fireEvent.click(screen.getByTestId('strategy-goal-achieve-btn'));
    expect(mockHandlers.onAchieve).toHaveBeenCalledWith('test-id-123');
  });

  it('calls onDrop when drop button is clicked', () => {
    render(<StrategyGoalCard goal={mockGoal} {...mockHandlers} />);
    fireEvent.click(screen.getByTestId('strategy-goal-drop-btn'));
    expect(mockHandlers.onDrop).toHaveBeenCalledWith('test-id-123');
  });

  it('calls onEdit when edit button is clicked', () => {
    render(<StrategyGoalCard goal={mockGoal} {...mockHandlers} />);
    fireEvent.click(screen.getByTestId('strategy-goal-edit-btn'));
    expect(mockHandlers.onEdit).toHaveBeenCalledWith('test-id-123');
  });

  it('calls onDelete when delete button is clicked', () => {
    render(<StrategyGoalCard goal={mockGoal} {...mockHandlers} />);
    fireEvent.click(screen.getByTestId('strategy-goal-delete-btn'));
    expect(mockHandlers.onDelete).toHaveBeenCalledWith('test-id-123');
  });

  it('calls onManageLinks when manage links button is clicked', () => {
    render(<StrategyGoalCard goal={mockGoal} {...mockHandlers} />);
    fireEvent.click(screen.getByTestId('strategy-goal-manage-links-btn'));
    expect(mockHandlers.onManageLinks).toHaveBeenCalledWith('test-id-123');
  });

  it('shows contributors badge when linkedPdpGoalCount is provided', () => {
    const goalWithContributors = { ...mockGoal, linkedPdpGoalCount: 5 };
    render(<StrategyGoalCard goal={goalWithContributors} {...mockHandlers} />);
    expect(screen.getByTestId('strategy-goal-contributors')).toHaveTextContent('5 contributors');
  });

  it('does not show contributors badge when linkedPdpGoalCount is 0', () => {
    const goalWithNoContributors = { ...mockGoal, linkedPdpGoalCount: 0 };
    render(<StrategyGoalCard goal={goalWithNoContributors} {...mockHandlers} />);
    expect(screen.queryByTestId('strategy-goal-contributors')).not.toBeInTheDocument();
  });

  it('shows strikethrough style for dropped goals', () => {
    const droppedGoal = { ...mockGoal, status: 'DROPPED' };
    render(<StrategyGoalCard goal={droppedGoal} {...mockHandlers} />);
    const title = screen.getByTestId('strategy-goal-title');
    expect(title).toHaveStyle({ textDecoration: 'line-through' });
  });
});
