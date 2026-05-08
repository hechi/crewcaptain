import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import SeriesConfigPanel from '@/components/one-on-one/SeriesConfigPanel';

describe('SeriesConfigPanel', () => {
  const mockOnSave = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders cadence options', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} />);

    expect(screen.getByTestId('series-config-panel')).toBeInTheDocument();
    expect(screen.getByTestId('cadence-option-weekly')).toBeInTheDocument();
    expect(screen.getByTestId('cadence-option-biweekly')).toBeInTheDocument();
    expect(screen.getByTestId('cadence-option-monthly')).toBeInTheDocument();
    expect(screen.getByTestId('cadence-option-custom')).toBeInTheDocument();
  });

  it('does not show custom interval input for WEEKLY cadence', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} />);

    // WEEKLY is default
    expect(screen.queryByTestId('custom-interval-section')).not.toBeInTheDocument();
  });

  it('does not show custom interval input for BIWEEKLY cadence', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} />);

    fireEvent.click(screen.getByTestId('cadence-option-biweekly'));
    expect(screen.queryByTestId('custom-interval-section')).not.toBeInTheDocument();
  });

  it('does not show custom interval input for MONTHLY cadence', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} />);

    fireEvent.click(screen.getByTestId('cadence-option-monthly'));
    expect(screen.queryByTestId('custom-interval-section')).not.toBeInTheDocument();
  });

  it('shows custom interval input only when CUSTOM is selected', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} />);

    fireEvent.click(screen.getByTestId('cadence-option-custom'));
    expect(screen.getByTestId('custom-interval-section')).toBeInTheDocument();
    expect(screen.getByTestId('custom-interval-input')).toBeInTheDocument();
  });

  it('hides custom interval input when switching from CUSTOM to another cadence', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} />);

    // Select CUSTOM
    fireEvent.click(screen.getByTestId('cadence-option-custom'));
    expect(screen.getByTestId('custom-interval-section')).toBeInTheDocument();

    // Switch to WEEKLY
    fireEvent.click(screen.getByTestId('cadence-option-weekly'));
    expect(screen.queryByTestId('custom-interval-section')).not.toBeInTheDocument();
  });

  it('calls onSave with correct data for non-custom cadence', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} />);

    // Select BIWEEKLY
    fireEvent.click(screen.getByTestId('cadence-option-biweekly'));

    // Click save
    fireEvent.click(screen.getByTestId('series-config-save'));

    expect(mockOnSave).toHaveBeenCalledTimes(1);
    expect(mockOnSave).toHaveBeenCalledWith({
      cadenceType: 'BIWEEKLY',
      customIntervalDays: null,
      templateMarkdown: null,
    });
  });

  it('calls onSave with correct data for CUSTOM cadence', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} />);

    // Select CUSTOM
    fireEvent.click(screen.getByTestId('cadence-option-custom'));

    // Enter interval
    const intervalInput = screen.getByTestId('custom-interval-input');
    fireEvent.change(intervalInput, { target: { value: '10' } });

    // Click save
    fireEvent.click(screen.getByTestId('series-config-save'));

    expect(mockOnSave).toHaveBeenCalledTimes(1);
    expect(mockOnSave).toHaveBeenCalledWith({
      cadenceType: 'CUSTOM',
      customIntervalDays: 10,
      templateMarkdown: null,
    });
  });

  it('shows error when CUSTOM cadence has no interval', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} />);

    // Select CUSTOM without entering interval
    fireEvent.click(screen.getByTestId('cadence-option-custom'));
    fireEvent.click(screen.getByTestId('series-config-save'));

    expect(screen.getByTestId('custom-interval-error')).toBeInTheDocument();
    expect(mockOnSave).not.toHaveBeenCalled();
  });

  it('disables save button when isSaving is true', () => {
    render(<SeriesConfigPanel onSave={mockOnSave} isSaving={true} />);

    const saveButton = screen.getByTestId('series-config-save');
    expect(saveButton).toBeDisabled();
    expect(saveButton).toHaveTextContent('Saving...');
  });
});
