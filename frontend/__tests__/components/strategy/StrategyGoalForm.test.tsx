import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import StrategyGoalForm from '@/components/strategy/StrategyGoalForm';

jest.mock('@/lib/api-client', () => ({
  optimizeStrategyGoal: jest.fn(),
}));

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: () => ({ getToken: () => 'test-token' }),
}));

describe('StrategyGoalForm', () => {
  const mockSubmit = jest.fn();
  const mockCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders create form with empty fields', () => {
    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" />);
    
    expect(screen.getByLabelText(/title/i)).toHaveValue('');
    expect(screen.getByLabelText(/description/i)).toHaveValue('');
    expect(screen.getByLabelText(/target date/i)).toHaveValue('');
  });

  it('renders edit form with initial data', () => {
    const initialData = {
      title: 'Test Goal',
      description: 'Test Description',
      targetDate: '2026-12-31',
      sensitive: true,
    };

    render(
      <StrategyGoalForm
        onSubmit={mockSubmit}
        onCancel={mockCancel}
        submitLabel="Save"
        initialData={initialData}
      />
    );

    expect(screen.getByLabelText(/title/i)).toHaveValue('Test Goal');
    expect(screen.getByLabelText(/description/i)).toHaveValue('Test Description');
    expect(screen.getByLabelText(/target date/i)).toHaveValue('2026-12-31');
    expect(screen.getByLabelText(/sensitive/i)).toBeChecked();
  });

  it('submits form with entered data', () => {
    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" />);

    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'New Goal' } });
    fireEvent.change(screen.getByLabelText(/description/i), { target: { value: 'New Description' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(mockSubmit).toHaveBeenCalledWith({
      title: 'New Goal',
      description: 'New Description',
      targetDate: null,
      sensitive: false,
    });
  });

  it('calls onCancel when cancel button is clicked', () => {
    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" />);
    
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
    
    expect(mockCancel).toHaveBeenCalled();
  });

  it('toggles sensitive flag', () => {
    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" />);

    const sensitiveCheckbox = screen.getByLabelText(/sensitive/i);
    expect(sensitiveCheckbox).not.toBeChecked();

    fireEvent.click(sensitiveCheckbox);
    expect(sensitiveCheckbox).toBeChecked();
  });

  it('validates required title', () => {
    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" />);

    const titleInput = screen.getByLabelText(/title/i);
    fireEvent.change(titleInput, { target: { value: '' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('should show SMART Check button when aiEnabled is true', () => {
    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" aiEnabled={true} />);
    expect(screen.getByTestId('strategy-goal-smart-check-btn')).toBeInTheDocument();
  });

  it('should not show SMART Check button when aiEnabled is false', () => {
    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" aiEnabled={false} />);
    expect(screen.queryByTestId('strategy-goal-smart-check-btn')).not.toBeInTheDocument();
  });

  it('should disable SMART Check button when title is empty', () => {
    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" aiEnabled={true} />);
    expect(screen.getByTestId('strategy-goal-smart-check-btn')).toBeDisabled();
  });

  it('should apply AI suggestion with structured format', async () => {
    const { optimizeStrategyGoal } = require('@/lib/api-client');
    optimizeStrategyGoal.mockResolvedValue({
      result: 'Title: Modernize Tech Stack by Q4\nDescription: Migrate legacy systems to cloud infrastructure\nExplanation: More specific and time-bound',
      error: null,
    });

    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" aiEnabled={true} />);

    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Improve tech' } });
    fireEvent.click(screen.getByTestId('strategy-goal-smart-check-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('strategy-goal-ai-comparison')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('strategy-goal-ai-apply-btn'));

    expect(screen.getByLabelText(/title/i)).toHaveValue('Modernize Tech Stack by Q4');
    expect(screen.getByLabelText(/description/i)).toHaveValue('Migrate legacy systems to cloud infrastructure');
  });

  it('should apply AI suggestion as description when no structured format', async () => {
    const { optimizeStrategyGoal } = require('@/lib/api-client');
    optimizeStrategyGoal.mockResolvedValue({
      result: 'This goal could be improved by adding measurable outcomes and a deadline.',
      error: null,
    });

    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" aiEnabled={true} />);

    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Improve tech' } });
    fireEvent.click(screen.getByTestId('strategy-goal-smart-check-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('strategy-goal-ai-comparison')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('strategy-goal-ai-apply-btn'));

    expect(screen.getByLabelText(/description/i)).toHaveValue(
      'This goal could be improved by adding measurable outcomes and a deadline.'
    );
  });

  it('should dismiss AI suggestion when Keep Original is clicked', async () => {
    const { optimizeStrategyGoal } = require('@/lib/api-client');
    optimizeStrategyGoal.mockResolvedValue({
      result: 'Title: Improved Goal\nDescription: Better description',
      error: null,
    });

    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" aiEnabled={true} />);

    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Original Goal' } });
    fireEvent.click(screen.getByTestId('strategy-goal-smart-check-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('strategy-goal-ai-comparison')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('strategy-goal-ai-dismiss-btn'));

    // Verify the comparison is dismissed and fields remain unchanged
    expect(screen.queryByTestId('strategy-goal-ai-comparison')).not.toBeInTheDocument();
    expect(screen.getByLabelText(/title/i)).toHaveValue('Original Goal');
  });

  it('should show error message when AI optimization fails', async () => {
    const { optimizeStrategyGoal } = require('@/lib/api-client');
    optimizeStrategyGoal.mockResolvedValue({
      result: null,
      error: 'AI service unavailable',
    });

    render(<StrategyGoalForm onSubmit={mockSubmit} onCancel={mockCancel} submitLabel="Create" aiEnabled={true} />);

    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Test Goal' } });
    fireEvent.click(screen.getByTestId('strategy-goal-smart-check-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('strategy-goal-ai-error')).toBeInTheDocument();
    });

    expect(screen.getByTestId('strategy-goal-ai-error')).toHaveTextContent('AI service unavailable');
  });
});
