import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import AiNarrativeModal from '@/components/AiNarrativeModal';

describe('AiNarrativeModal', () => {
  const mockOnGenerate = jest.fn();
  const mockOnClose = jest.fn();

  const defaultProps = {
    personName: 'Alice Smith',
    onGenerate: mockOnGenerate,
    onClose: mockOnClose,
    generating: false,
    narrative: null,
    error: null,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the modal with title and person name', () => {
    render(<AiNarrativeModal {...defaultProps} />);
    expect(screen.getByTestId('ai-narrative-modal')).toBeInTheDocument();
    expect(screen.getByText('Generate AI Narrative')).toBeInTheDocument();
    expect(screen.getByText('Alice Smith')).toBeInTheDocument();
  });

  it('should render date inputs with default values', () => {
    render(<AiNarrativeModal {...defaultProps} />);
    const dateFrom = screen.getByTestId('narrative-date-from') as HTMLInputElement;
    const dateTo = screen.getByTestId('narrative-date-to') as HTMLInputElement;
    expect(dateFrom.value).toBeTruthy();
    expect(dateTo.value).toBeTruthy();
  });

  it('should call onGenerate with date range when form is submitted', () => {
    render(<AiNarrativeModal {...defaultProps} />);

    fireEvent.change(screen.getByTestId('narrative-date-from'), {
      target: { value: '2026-01-01' },
    });
    fireEvent.change(screen.getByTestId('narrative-date-to'), {
      target: { value: '2026-06-30' },
    });
    fireEvent.click(screen.getByTestId('ai-narrative-generate'));

    expect(mockOnGenerate).toHaveBeenCalledWith('2026-01-01', '2026-06-30');
  });

  it('should call onClose when cancel button is clicked', () => {
    render(<AiNarrativeModal {...defaultProps} />);
    fireEvent.click(screen.getByTestId('ai-narrative-cancel'));
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should call onClose when backdrop is clicked', () => {
    render(<AiNarrativeModal {...defaultProps} />);
    fireEvent.click(screen.getByTestId('ai-narrative-backdrop'));
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should show validation error when dateTo is before dateFrom', () => {
    render(<AiNarrativeModal {...defaultProps} />);

    fireEvent.change(screen.getByTestId('narrative-date-from'), {
      target: { value: '2026-06-01' },
    });
    fireEvent.change(screen.getByTestId('narrative-date-to'), {
      target: { value: '2026-01-01' },
    });
    fireEvent.click(screen.getByTestId('ai-narrative-generate'));

    expect(screen.getByTestId('ai-narrative-validation-error')).toHaveTextContent(
      'End date must not be before start date'
    );
    expect(mockOnGenerate).not.toHaveBeenCalled();
  });

  it('should show validation error when dates are empty', () => {
    render(<AiNarrativeModal {...defaultProps} />);

    fireEvent.change(screen.getByTestId('narrative-date-from'), {
      target: { value: '' },
    });
    fireEvent.click(screen.getByTestId('ai-narrative-generate'));

    expect(screen.getByTestId('ai-narrative-validation-error')).toHaveTextContent(
      'Both dates are required'
    );
  });

  it('should show generating state with pulse animation', () => {
    render(<AiNarrativeModal {...defaultProps} generating={true} />);
    expect(screen.getByText('Generating...')).toBeInTheDocument();
    expect(screen.getByTestId('ai-narrative-pulse')).toBeInTheDocument();
  });

  it('should disable buttons when generating', () => {
    render(<AiNarrativeModal {...defaultProps} generating={true} />);
    expect(screen.getByTestId('ai-narrative-generate')).toBeDisabled();
    expect(screen.getByTestId('ai-narrative-cancel')).toBeDisabled();
  });

  it('should display error message from API', () => {
    render(<AiNarrativeModal {...defaultProps} error="Cannot connect to AI API." />);
    expect(screen.getByTestId('ai-narrative-error')).toHaveTextContent('Cannot connect to AI API.');
  });

  it('should display narrative result in textarea when available', () => {
    render(
      <AiNarrativeModal
        {...defaultProps}
        narrative="Alice demonstrated exceptional leadership..."
      />
    );
    expect(screen.getByTestId('ai-narrative-result')).toBeInTheDocument();
    const textarea = screen.getByTestId('ai-narrative-textarea') as HTMLTextAreaElement;
    expect(textarea.value).toBe('Alice demonstrated exceptional leadership...');
  });

  it('should show copy and done buttons when narrative is displayed', () => {
    render(
      <AiNarrativeModal
        {...defaultProps}
        narrative="Some narrative text"
      />
    );
    expect(screen.getByTestId('ai-narrative-copy')).toBeInTheDocument();
    expect(screen.getByTestId('ai-narrative-done')).toBeInTheDocument();
  });

  it('should call onClose when done button is clicked', () => {
    render(
      <AiNarrativeModal
        {...defaultProps}
        narrative="Some narrative text"
      />
    );
    fireEvent.click(screen.getByTestId('ai-narrative-done'));
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should not show the form when narrative is displayed', () => {
    render(
      <AiNarrativeModal
        {...defaultProps}
        narrative="Some narrative text"
      />
    );
    expect(screen.queryByTestId('narrative-date-from')).not.toBeInTheDocument();
    expect(screen.queryByTestId('ai-narrative-generate')).not.toBeInTheDocument();
  });

  it('should have proper accessibility attributes', () => {
    render(<AiNarrativeModal {...defaultProps} />);
    const modal = screen.getByTestId('ai-narrative-modal');
    expect(modal).toHaveAttribute('role', 'dialog');
    expect(modal).toHaveAttribute('aria-modal', 'true');
    expect(modal).toHaveAttribute('aria-labelledby', 'ai-narrative-title');
  });

  it('should have labels for date inputs', () => {
    render(<AiNarrativeModal {...defaultProps} />);
    expect(screen.getByLabelText('From')).toBeInTheDocument();
    expect(screen.getByLabelText('To')).toBeInTheDocument();
  });

  it('should copy narrative to clipboard when copy button is clicked', () => {
    Object.assign(navigator, {
      clipboard: { writeText: jest.fn().mockResolvedValue(undefined) },
    });

    render(
      <AiNarrativeModal
        {...defaultProps}
        narrative="Narrative to copy"
      />
    );
    fireEvent.click(screen.getByTestId('ai-narrative-copy'));
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('Narrative to copy');
  });
});
