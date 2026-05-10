import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PdpGoalForm from '@/components/pdp-goals/PdpGoalForm';
import { PdpGoal } from '@/types/pdp-goal';

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

describe('PdpGoalForm', () => {
  it('should render create form with empty fields', () => {
    render(<PdpGoalForm onSubmit={jest.fn()} onCancel={jest.fn()} />);
    expect(screen.getByTestId('pdp-goal-title-input')).toHaveValue('');
    expect(screen.getByTestId('pdp-goal-description-input')).toHaveValue('');
    expect(screen.getByTestId('pdp-goal-submit-btn')).toHaveTextContent('Create Goal');
  });

  it('should render edit form with existing values', () => {
    render(<PdpGoalForm existingGoal={mockGoal} onSubmit={jest.fn()} onCancel={jest.fn()} />);
    expect(screen.getByTestId('pdp-goal-title-input')).toHaveValue('Improve public speaking');
    expect(screen.getByTestId('pdp-goal-description-input')).toHaveValue('Practice presentations monthly');
    expect(screen.getByTestId('pdp-goal-submit-btn')).toHaveTextContent('Update Goal');
  });

  it('should call onSubmit with form data on create', () => {
    const onSubmit = jest.fn();
    render(<PdpGoalForm onSubmit={onSubmit} onCancel={jest.fn()} />);

    fireEvent.change(screen.getByTestId('pdp-goal-title-input'), { target: { value: 'Learn Kotlin' } });
    fireEvent.change(screen.getByTestId('pdp-goal-description-input'), { target: { value: 'Complete course' } });
    fireEvent.submit(screen.getByTestId('pdp-goal-form'));

    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Learn Kotlin',
      description: 'Complete course',
      targetDate: null,
    });
  });

  it('should call onSubmit with updated fields on edit', () => {
    const onSubmit = jest.fn();
    render(<PdpGoalForm existingGoal={mockGoal} onSubmit={onSubmit} onCancel={jest.fn()} />);

    fireEvent.change(screen.getByTestId('pdp-goal-title-input'), { target: { value: 'Updated title' } });
    fireEvent.submit(screen.getByTestId('pdp-goal-form'));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Updated title',
    }));
  });

  it('should not submit when title is empty', () => {
    const onSubmit = jest.fn();
    render(<PdpGoalForm onSubmit={onSubmit} onCancel={jest.fn()} />);

    fireEvent.submit(screen.getByTestId('pdp-goal-form'));

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('should call onCancel when cancel button is clicked', () => {
    const onCancel = jest.fn();
    render(<PdpGoalForm onSubmit={jest.fn()} onCancel={onCancel} />);

    fireEvent.click(screen.getByTestId('pdp-goal-cancel-btn'));

    expect(onCancel).toHaveBeenCalled();
  });

  it('should render target date input', () => {
    render(<PdpGoalForm onSubmit={jest.fn()} onCancel={jest.fn()} />);
    expect(screen.getByTestId('pdp-goal-target-date-input')).toBeInTheDocument();
  });

  it('should include target date in submission', () => {
    const onSubmit = jest.fn();
    render(<PdpGoalForm onSubmit={onSubmit} onCancel={jest.fn()} />);

    fireEvent.change(screen.getByTestId('pdp-goal-title-input'), { target: { value: 'Goal' } });
    fireEvent.change(screen.getByTestId('pdp-goal-target-date-input'), { target: { value: '2026-12-31' } });
    fireEvent.submit(screen.getByTestId('pdp-goal-form'));

    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Goal',
      description: null,
      targetDate: '2026-12-31',
    });
  });
});
