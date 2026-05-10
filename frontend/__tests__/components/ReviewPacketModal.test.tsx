import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ReviewPacketModal from '@/components/ReviewPacketModal';

describe('ReviewPacketModal', () => {
  const mockOnGenerate = jest.fn();
  const mockOnClose = jest.fn();

  const defaultProps = {
    personName: 'Jane Smith',
    onGenerate: mockOnGenerate,
    onClose: mockOnClose,
    generating: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the modal with title and person name', () => {
    render(<ReviewPacketModal {...defaultProps} />);
    expect(screen.getByTestId('review-packet-modal')).toBeInTheDocument();
    expect(screen.getByText('Generate Review Packet')).toBeInTheDocument();
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
  });

  it('should render date inputs with default values', () => {
    render(<ReviewPacketModal {...defaultProps} />);
    const dateFrom = screen.getByTestId('review-date-from') as HTMLInputElement;
    const dateTo = screen.getByTestId('review-date-to') as HTMLInputElement;
    expect(dateFrom).toBeInTheDocument();
    expect(dateTo).toBeInTheDocument();
    expect(dateFrom.value).toBeTruthy();
    expect(dateTo.value).toBeTruthy();
  });

  it('should call onGenerate with date range when form is submitted', () => {
    render(<ReviewPacketModal {...defaultProps} />);

    fireEvent.change(screen.getByTestId('review-date-from'), {
      target: { value: '2024-01-01' },
    });
    fireEvent.change(screen.getByTestId('review-date-to'), {
      target: { value: '2024-06-30' },
    });

    fireEvent.click(screen.getByTestId('review-packet-generate'));

    expect(mockOnGenerate).toHaveBeenCalledWith('2024-01-01', '2024-06-30');
  });

  it('should call onClose when cancel button is clicked', () => {
    render(<ReviewPacketModal {...defaultProps} />);
    fireEvent.click(screen.getByTestId('review-packet-cancel'));
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should call onClose when backdrop is clicked', () => {
    render(<ReviewPacketModal {...defaultProps} />);
    fireEvent.click(screen.getByTestId('review-packet-backdrop'));
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should show validation error when dateTo is before dateFrom', () => {
    render(<ReviewPacketModal {...defaultProps} />);

    fireEvent.change(screen.getByTestId('review-date-from'), {
      target: { value: '2024-06-30' },
    });
    fireEvent.change(screen.getByTestId('review-date-to'), {
      target: { value: '2024-01-01' },
    });

    fireEvent.click(screen.getByTestId('review-packet-generate'));

    expect(screen.getByTestId('review-packet-validation-error')).toBeInTheDocument();
    expect(screen.getByText('End date must not be before start date')).toBeInTheDocument();
    expect(mockOnGenerate).not.toHaveBeenCalled();
  });

  it('should show validation error when dates are empty', () => {
    render(<ReviewPacketModal {...defaultProps} />);

    fireEvent.change(screen.getByTestId('review-date-from'), {
      target: { value: '' },
    });
    fireEvent.change(screen.getByTestId('review-date-to'), {
      target: { value: '' },
    });

    fireEvent.click(screen.getByTestId('review-packet-generate'));

    expect(screen.getByTestId('review-packet-validation-error')).toBeInTheDocument();
    expect(mockOnGenerate).not.toHaveBeenCalled();
  });

  it('should show generating state when generating is true', () => {
    render(<ReviewPacketModal {...defaultProps} generating={true} />);
    expect(screen.getByText('Generating...')).toBeInTheDocument();
  });

  it('should disable buttons when generating', () => {
    render(<ReviewPacketModal {...defaultProps} generating={true} />);
    expect(screen.getByTestId('review-packet-generate')).toBeDisabled();
    expect(screen.getByTestId('review-packet-cancel')).toBeDisabled();
  });

  it('should show Generate text when not generating', () => {
    render(<ReviewPacketModal {...defaultProps} generating={false} />);
    expect(screen.getByText('Generate')).toBeInTheDocument();
  });

  it('should have proper accessibility attributes', () => {
    render(<ReviewPacketModal {...defaultProps} />);
    const modal = screen.getByTestId('review-packet-modal');
    expect(modal).toHaveAttribute('role', 'dialog');
    expect(modal).toHaveAttribute('aria-modal', 'true');
    expect(modal).toHaveAttribute('aria-labelledby', 'review-packet-title');
  });

  it('should have labels for date inputs', () => {
    render(<ReviewPacketModal {...defaultProps} />);
    expect(screen.getByLabelText('From')).toBeInTheDocument();
    expect(screen.getByLabelText('To')).toBeInTheDocument();
  });
});
