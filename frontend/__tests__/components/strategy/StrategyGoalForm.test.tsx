import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import StrategyGoalForm from '@/components/strategy/StrategyGoalForm';

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

    // Form should not submit with empty title
    expect(mockSubmit).not.toHaveBeenCalled();
  });
});
