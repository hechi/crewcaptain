import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import FilterBar from '@/components/FilterBar';

describe('FilterBar', () => {
  it('should render tag input and morale dropdown', () => {
    const onFilterChange = jest.fn();
    render(<FilterBar onFilterChange={onFilterChange} />);

    expect(screen.getByLabelText('Filter by tag')).toBeInTheDocument();
    expect(screen.getByLabelText('Filter by morale status')).toBeInTheDocument();
  });

  it('should emit filter change when tag input changes', () => {
    const onFilterChange = jest.fn();
    render(<FilterBar onFilterChange={onFilterChange} />);

    const tagInput = screen.getByLabelText('Filter by tag');
    fireEvent.change(tagInput, { target: { value: 'engineering' } });

    expect(onFilterChange).toHaveBeenCalledWith({ tag: 'engineering', morale: '' });
  });

  it('should emit filter change when morale dropdown changes', () => {
    const onFilterChange = jest.fn();
    render(<FilterBar onFilterChange={onFilterChange} />);

    const moraleSelect = screen.getByLabelText('Filter by morale status');
    fireEvent.change(moraleSelect, { target: { value: 'GREEN' } });

    expect(onFilterChange).toHaveBeenCalledWith({ tag: '', morale: 'GREEN' });
  });

  it('should emit both tag and morale when both are set', () => {
    const onFilterChange = jest.fn();
    render(<FilterBar onFilterChange={onFilterChange} />);

    const tagInput = screen.getByLabelText('Filter by tag');
    const moraleSelect = screen.getByLabelText('Filter by morale status');

    fireEvent.change(tagInput, { target: { value: 'senior' } });
    fireEvent.change(moraleSelect, { target: { value: 'RED' } });

    expect(onFilterChange).toHaveBeenLastCalledWith({ tag: 'senior', morale: 'RED' });
  });

  it('should use initial values when provided', () => {
    const onFilterChange = jest.fn();
    render(<FilterBar onFilterChange={onFilterChange} initialTag="team-a" initialMorale="YELLOW" />);

    const tagInput = screen.getByLabelText('Filter by tag') as HTMLInputElement;
    const moraleSelect = screen.getByLabelText('Filter by morale status') as HTMLSelectElement;

    expect(tagInput.value).toBe('team-a');
    expect(moraleSelect.value).toBe('YELLOW');
  });
});
