import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PdpGoalList from '@/components/pdp-goals/PdpGoalList';
import { PdpGoal } from '@/types/pdp-goal';

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: () => ({
    getToken: () => 'test-token',
    isAuthenticated: true,
    status: 'authenticated',
  }),
}));

jest.mock('@/lib/api-client', () => ({
  optimizePdpGoal: jest.fn(),
}));

const mockGoals: PdpGoal[] = [
  {
    id: 'goal-1',
    personId: 'person-1',
    title: 'Improve public speaking',
    description: 'Practice presentations monthly',
    targetDate: '2026-12-31',
    status: 'ACTIVE',
    createdAt: '2026-05-10T10:00:00Z',
    updatedAt: '2026-05-10T10:00:00Z',
  },
  {
    id: 'goal-2',
    personId: 'person-1',
    title: 'Learn Kotlin',
    description: null,
    targetDate: null,
    status: 'PAUSED',
    createdAt: '2026-05-09T10:00:00Z',
    updatedAt: '2026-05-09T10:00:00Z',
  },
];

const defaultProps = {
  goals: mockGoals,
  onCreateGoal: jest.fn(),
  onUpdateGoal: jest.fn(),
  onAchieveGoal: jest.fn(),
  onPauseGoal: jest.fn(),
  onDropGoal: jest.fn(),
  onResumeGoal: jest.fn(),
  onDeleteGoal: jest.fn(),
  statusFilter: null,
  onStatusFilterChange: jest.fn(),
};

describe('PdpGoalList', () => {
  it('should render all goals', () => {
    render(<PdpGoalList {...defaultProps} />);
    expect(screen.getAllByTestId('pdp-goal-card')).toHaveLength(2);
  });

  it('should show empty state when no goals', () => {
    render(<PdpGoalList {...defaultProps} goals={[]} />);
    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
  });

  it('should render status filter buttons', () => {
    render(<PdpGoalList {...defaultProps} />);
    expect(screen.getByTestId('pdp-goal-filter-all')).toBeInTheDocument();
    expect(screen.getByTestId('pdp-goal-filter-active')).toBeInTheDocument();
    expect(screen.getByTestId('pdp-goal-filter-achieved')).toBeInTheDocument();
    expect(screen.getByTestId('pdp-goal-filter-paused')).toBeInTheDocument();
    expect(screen.getByTestId('pdp-goal-filter-dropped')).toBeInTheDocument();
  });

  it('should call onStatusFilterChange when filter is clicked', () => {
    const onStatusFilterChange = jest.fn();
    render(<PdpGoalList {...defaultProps} onStatusFilterChange={onStatusFilterChange} />);
    fireEvent.click(screen.getByTestId('pdp-goal-filter-active'));
    expect(onStatusFilterChange).toHaveBeenCalledWith('ACTIVE');
  });

  it('should show create button', () => {
    render(<PdpGoalList {...defaultProps} />);
    expect(screen.getByTestId('pdp-goal-create-btn')).toBeInTheDocument();
  });

  it('should show create form when create button is clicked', () => {
    render(<PdpGoalList {...defaultProps} />);
    fireEvent.click(screen.getByTestId('pdp-goal-create-btn'));
    expect(screen.getByTestId('pdp-goal-form')).toBeInTheDocument();
  });

  it('should hide create button when form is shown', () => {
    render(<PdpGoalList {...defaultProps} />);
    fireEvent.click(screen.getByTestId('pdp-goal-create-btn'));
    expect(screen.queryByTestId('pdp-goal-create-btn')).not.toBeInTheDocument();
  });

  it('should call onCreateGoal when form is submitted', () => {
    const onCreateGoal = jest.fn();
    render(<PdpGoalList {...defaultProps} onCreateGoal={onCreateGoal} />);
    fireEvent.click(screen.getByTestId('pdp-goal-create-btn'));
    fireEvent.change(screen.getByTestId('pdp-goal-title-input'), { target: { value: 'New Goal' } });
    fireEvent.submit(screen.getByTestId('pdp-goal-form'));
    expect(onCreateGoal).toHaveBeenCalled();
  });

  it('should show edit form when edit button is clicked', () => {
    render(<PdpGoalList {...defaultProps} />);
    const editButtons = screen.getAllByTestId('pdp-goal-edit-btn');
    fireEvent.click(editButtons[0]);
    expect(screen.getByTestId('pdp-goal-form')).toBeInTheDocument();
    expect(screen.getByTestId('pdp-goal-title-input')).toHaveValue('Improve public speaking');
  });
});
