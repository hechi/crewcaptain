import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PdpGoalForm from '@/components/pdp-goals/PdpGoalForm';
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

  it('should show SMART Check button when aiEnabled is true', () => {
    render(<PdpGoalForm onSubmit={jest.fn()} onCancel={jest.fn()} aiEnabled={true} />);
    expect(screen.getByTestId('pdp-goal-smart-check-btn')).toBeInTheDocument();
  });

  it('should not show SMART Check button when aiEnabled is false', () => {
    render(<PdpGoalForm onSubmit={jest.fn()} onCancel={jest.fn()} aiEnabled={false} />);
    expect(screen.queryByTestId('pdp-goal-smart-check-btn')).not.toBeInTheDocument();
  });

  it('should disable SMART Check button when title is empty', () => {
    render(<PdpGoalForm onSubmit={jest.fn()} onCancel={jest.fn()} aiEnabled={true} />);
    expect(screen.getByTestId('pdp-goal-smart-check-btn')).toBeDisabled();
  });

  it('should apply AI suggestion with structured format in edit mode', async () => {
    const { optimizePdpGoal } = require('@/lib/api-client');
    optimizePdpGoal.mockResolvedValue({
      result: 'Title: Deliver 3 presentations by Q3\nDescription: Present at team all-hands monthly\nExplanation: More specific and measurable',
      error: null,
    });

    const onSubmit = jest.fn();
    render(<PdpGoalForm existingGoal={mockGoal} onSubmit={onSubmit} onCancel={jest.fn()} aiEnabled={true} />);

    // Click SMART Check
    fireEvent.click(screen.getByTestId('pdp-goal-smart-check-btn'));

    // Wait for the AI comparison to appear
    const { waitFor } = require('@testing-library/react');
    await waitFor(() => {
      expect(screen.getByTestId('pdp-goal-ai-comparison')).toBeInTheDocument();
    });

    // Click Apply
    fireEvent.click(screen.getByTestId('pdp-goal-ai-apply-btn'));

    // Verify the form fields were updated
    expect(screen.getByTestId('pdp-goal-title-input')).toHaveValue('Deliver 3 presentations by Q3');
    expect(screen.getByTestId('pdp-goal-description-input')).toHaveValue('Present at team all-hands monthly');

    // Submit and verify the updated values are sent
    fireEvent.submit(screen.getByTestId('pdp-goal-form'));
    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Deliver 3 presentations by Q3',
      description: 'Present at team all-hands monthly',
    }));
  });

  it('should apply AI suggestion as description when no structured format', async () => {
    const { optimizePdpGoal } = require('@/lib/api-client');
    optimizePdpGoal.mockResolvedValue({
      result: 'This goal could be improved by adding measurable outcomes and a deadline.',
      error: null,
    });

    render(<PdpGoalForm existingGoal={mockGoal} onSubmit={jest.fn()} onCancel={jest.fn()} aiEnabled={true} />);

    fireEvent.click(screen.getByTestId('pdp-goal-smart-check-btn'));

    const { waitFor } = require('@testing-library/react');
    await waitFor(() => {
      expect(screen.getByTestId('pdp-goal-ai-comparison')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('pdp-goal-ai-apply-btn'));

    // When no structured format, the whole response becomes the description
    expect(screen.getByTestId('pdp-goal-description-input')).toHaveValue(
      'This goal could be improved by adding measurable outcomes and a deadline.'
    );
  });
});
