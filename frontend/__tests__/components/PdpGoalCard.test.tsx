import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PdpGoalCard from '@/components/pdp-goals/PdpGoalCard';
import { PdpGoal } from '@/types/pdp-goal';

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(() => ({ status: 'unauthenticated', data: null })),
}));

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: jest.fn(() => jest.fn(() => null)),
}));

const mockGoal: PdpGoal = {
  id: 'goal-1',
  personId: 'person-1',
  title: 'Improve public speaking',
  description: 'Practice presentations monthly',
  targetDate: '2026-12-31',
  status: 'ACTIVE',
  createdAt: '2026-05-10T10:00:00Z',
  updatedAt: '2026-05-10T10:00:00Z',
};

describe('PdpGoalCard', () => {
  it('should render goal title', () => {
    render(<PdpGoalCard goal={mockGoal} />);
    expect(screen.getByTestId('pdp-goal-title')).toHaveTextContent('Improve public speaking');
  });

  it('should render goal description', () => {
    render(<PdpGoalCard goal={mockGoal} />);
    expect(screen.getByTestId('pdp-goal-description')).toHaveTextContent('Practice presentations monthly');
  });

  it('should not render description when null', () => {
    const goalWithoutDesc = { ...mockGoal, description: null };
    render(<PdpGoalCard goal={goalWithoutDesc} />);
    expect(screen.queryByTestId('pdp-goal-description')).not.toBeInTheDocument();
  });

  it('should render target date', () => {
    render(<PdpGoalCard goal={mockGoal} />);
    expect(screen.getByTestId('pdp-goal-target-date')).toBeInTheDocument();
  });

  it('should not render target date when null', () => {
    const goalWithoutDate = { ...mockGoal, targetDate: null };
    render(<PdpGoalCard goal={goalWithoutDate} />);
    expect(screen.queryByTestId('pdp-goal-target-date')).not.toBeInTheDocument();
  });

  it('should render status badge', () => {
    render(<PdpGoalCard goal={mockGoal} />);
    expect(screen.getByTestId('pdp-goal-status-badge')).toHaveTextContent('Active');
  });

  it('should show achieve button for active goals', () => {
    render(<PdpGoalCard goal={mockGoal} onAchieve={jest.fn()} />);
    expect(screen.getByTestId('pdp-goal-achieve-btn')).toBeInTheDocument();
  });

  it('should show pause button for active goals', () => {
    render(<PdpGoalCard goal={mockGoal} onPause={jest.fn()} />);
    expect(screen.getByTestId('pdp-goal-pause-btn')).toBeInTheDocument();
  });

  it('should show drop button for active goals', () => {
    render(<PdpGoalCard goal={mockGoal} onDrop={jest.fn()} />);
    expect(screen.getByTestId('pdp-goal-drop-btn')).toBeInTheDocument();
  });

  it('should show resume button for paused goals', () => {
    const pausedGoal = { ...mockGoal, status: 'PAUSED' as const };
    render(<PdpGoalCard goal={pausedGoal} onResume={jest.fn()} />);
    expect(screen.getByTestId('pdp-goal-resume-btn')).toBeInTheDocument();
  });

  it('should not show action buttons for achieved goals', () => {
    const achievedGoal = { ...mockGoal, status: 'ACHIEVED' as const };
    render(<PdpGoalCard goal={achievedGoal} onAchieve={jest.fn()} onPause={jest.fn()} />);
    expect(screen.queryByTestId('pdp-goal-achieve-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('pdp-goal-pause-btn')).not.toBeInTheDocument();
  });

  it('should not show action buttons for dropped goals', () => {
    const droppedGoal = { ...mockGoal, status: 'DROPPED' as const };
    render(<PdpGoalCard goal={droppedGoal} onAchieve={jest.fn()} onDrop={jest.fn()} />);
    expect(screen.queryByTestId('pdp-goal-achieve-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('pdp-goal-drop-btn')).not.toBeInTheDocument();
  });

  it('should call onAchieve when achieve button is clicked', () => {
    const onAchieve = jest.fn();
    render(<PdpGoalCard goal={mockGoal} onAchieve={onAchieve} />);
    fireEvent.click(screen.getByTestId('pdp-goal-achieve-btn'));
    expect(onAchieve).toHaveBeenCalledWith('goal-1');
  });

  it('should call onPause when pause button is clicked', () => {
    const onPause = jest.fn();
    render(<PdpGoalCard goal={mockGoal} onPause={onPause} />);
    fireEvent.click(screen.getByTestId('pdp-goal-pause-btn'));
    expect(onPause).toHaveBeenCalledWith('goal-1');
  });

  it('should call onDrop when drop button is clicked', () => {
    const onDrop = jest.fn();
    render(<PdpGoalCard goal={mockGoal} onDrop={onDrop} />);
    fireEvent.click(screen.getByTestId('pdp-goal-drop-btn'));
    expect(onDrop).toHaveBeenCalledWith('goal-1');
  });

  it('should call onResume when resume button is clicked', () => {
    const onResume = jest.fn();
    const pausedGoal = { ...mockGoal, status: 'PAUSED' as const };
    render(<PdpGoalCard goal={pausedGoal} onResume={onResume} />);
    fireEvent.click(screen.getByTestId('pdp-goal-resume-btn'));
    expect(onResume).toHaveBeenCalledWith('goal-1');
  });

  it('should call onDelete when delete button is clicked', () => {
    const onDelete = jest.fn();
    render(<PdpGoalCard goal={mockGoal} onDelete={onDelete} />);
    fireEvent.click(screen.getByTestId('pdp-goal-delete-btn'));
    expect(onDelete).toHaveBeenCalledWith('goal-1');
  });

  it('should call onEdit when edit button is clicked', () => {
    const onEdit = jest.fn();
    render(<PdpGoalCard goal={mockGoal} onEdit={onEdit} />);
    fireEvent.click(screen.getByTestId('pdp-goal-edit-btn'));
    expect(onEdit).toHaveBeenCalledWith('goal-1');
  });

  it('should apply line-through style for dropped goals', () => {
    const droppedGoal = { ...mockGoal, status: 'DROPPED' as const };
    render(<PdpGoalCard goal={droppedGoal} />);
    expect(screen.getByTestId('pdp-goal-title')).toHaveStyle({ textDecoration: 'line-through' });
  });

  it('should show view updates button when handler provided', () => {
    render(<PdpGoalCard goal={mockGoal} onViewUpdates={jest.fn()} />);
    expect(screen.getByTestId('pdp-goal-view-updates-btn')).toBeInTheDocument();
  });
});
