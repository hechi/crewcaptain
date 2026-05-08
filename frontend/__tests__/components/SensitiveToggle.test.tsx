import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import SensitiveToggle from '@/components/one-on-one/SensitiveToggle';

describe('SensitiveToggle', () => {
  const mockOnChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the switch element', () => {
    render(<SensitiveToggle checked={false} onChange={mockOnChange} />);

    expect(screen.getByTestId('sensitive-toggle')).toBeInTheDocument();
    expect(screen.getByTestId('sensitive-toggle-switch')).toBeInTheDocument();
  });

  it('shows aria-checked as false when unchecked', () => {
    render(<SensitiveToggle checked={false} onChange={mockOnChange} />);

    const switchEl = screen.getByTestId('sensitive-toggle-switch');
    expect(switchEl).toHaveAttribute('aria-checked', 'false');
  });

  it('shows aria-checked as true when checked', () => {
    render(<SensitiveToggle checked={true} onChange={mockOnChange} />);

    const switchEl = screen.getByTestId('sensitive-toggle-switch');
    expect(switchEl).toHaveAttribute('aria-checked', 'true');
  });

  it('calls onChange with true when clicked while unchecked', () => {
    render(<SensitiveToggle checked={false} onChange={mockOnChange} />);

    fireEvent.click(screen.getByTestId('sensitive-toggle-switch'));
    expect(mockOnChange).toHaveBeenCalledWith(true);
  });

  it('calls onChange with false when clicked while checked', () => {
    render(<SensitiveToggle checked={true} onChange={mockOnChange} />);

    fireEvent.click(screen.getByTestId('sensitive-toggle-switch'));
    expect(mockOnChange).toHaveBeenCalledWith(false);
  });

  it('renders default label text', () => {
    render(<SensitiveToggle checked={false} onChange={mockOnChange} />);

    expect(screen.getByText('Mark as sensitive')).toBeInTheDocument();
  });

  it('renders custom label text', () => {
    render(<SensitiveToggle checked={false} onChange={mockOnChange} label="Custom label" />);

    expect(screen.getByText('Custom label')).toBeInTheDocument();
  });

  it('has role="switch" on the toggle button', () => {
    render(<SensitiveToggle checked={false} onChange={mockOnChange} />);

    const switchEl = screen.getByRole('switch');
    expect(switchEl).toBeInTheDocument();
  });
});
