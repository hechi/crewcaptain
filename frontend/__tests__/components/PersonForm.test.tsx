import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PersonForm from '@/components/PersonForm';
import { Person } from '@/types/person';

describe('PersonForm', () => {
  it('should render all form fields', () => {
    const onSubmit = jest.fn();
    render(<PersonForm mode="create" onSubmit={onSubmit} />);

    expect(screen.getByLabelText(/^Name/)).toBeInTheDocument();
    expect(screen.getByLabelText('Preferred Name')).toBeInTheDocument();
    expect(screen.getByLabelText('Role / Title')).toBeInTheDocument();
    expect(screen.getByLabelText('Timezone')).toBeInTheDocument();
    expect(screen.getByLabelText('Start Date')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Tags (comma-separated)')).toBeInTheDocument();
  });

  it('should show validation error when name is blank on submit', () => {
    const onSubmit = jest.fn();
    render(<PersonForm mode="create" onSubmit={onSubmit} />);

    fireEvent.click(screen.getByText('Create Person'));

    expect(screen.getByTestId('name-error')).toHaveTextContent('Name is required');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('should show validation error when name is only whitespace', () => {
    const onSubmit = jest.fn();
    render(<PersonForm mode="create" onSubmit={onSubmit} />);

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: '   ' } });
    fireEvent.click(screen.getByText('Create Person'));

    expect(screen.getByTestId('name-error')).toHaveTextContent('Name is required');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('should call onSubmit with form data when valid', () => {
    const onSubmit = jest.fn();
    render(<PersonForm mode="create" onSubmit={onSubmit} />);

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Jane Smith' } });
    fireEvent.change(screen.getByLabelText('Role / Title'), { target: { value: 'Engineer' } });
    fireEvent.change(screen.getByLabelText('Tags (comma-separated)'), { target: { value: 'eng, senior' } });
    fireEvent.click(screen.getByText('Create Person'));

    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Jane Smith',
      roleTitle: 'Engineer',
      tags: ['eng', 'senior'],
    });
  });

  it('should display "Save Changes" button in edit mode', () => {
    const onSubmit = jest.fn();
    render(<PersonForm mode="edit" onSubmit={onSubmit} />);

    expect(screen.getByText('Save Changes')).toBeInTheDocument();
  });

  it('should display "Create Person" button in create mode', () => {
    const onSubmit = jest.fn();
    render(<PersonForm mode="create" onSubmit={onSubmit} />);

    expect(screen.getByText('Create Person')).toBeInTheDocument();
  });

  it('should populate fields with initial data in edit mode', () => {
    const onSubmit = jest.fn();
    const initialData: Person = {
      id: '123',
      name: 'Jane Smith',
      preferredName: 'Jane',
      roleTitle: 'Senior Engineer',
      timezone: 'Europe/Berlin',
      startDate: '2024-03-15',
      email: 'jane@example.com',
      tags: ['engineering', 'senior'],
      moraleStatus: 'GREEN',
      moraleNote: null,
      pinnedRememberItems: [],
      atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
      createdAt: '2025-05-08T12:00:00Z',
      updatedAt: '2025-05-08T12:00:00Z',
    };

    render(<PersonForm mode="edit" initialData={initialData} onSubmit={onSubmit} />);

    expect((screen.getByLabelText(/^Name/) as HTMLInputElement).value).toBe('Jane Smith');
    expect((screen.getByLabelText('Preferred Name') as HTMLInputElement).value).toBe('Jane');
    expect((screen.getByLabelText('Role / Title') as HTMLInputElement).value).toBe('Senior Engineer');
    expect((screen.getByLabelText('Email') as HTMLInputElement).value).toBe('jane@example.com');
    expect((screen.getByLabelText('Tags (comma-separated)') as HTMLInputElement).value).toBe('engineering, senior');
  });

  it('should call onCancel when cancel button is clicked', () => {
    const onSubmit = jest.fn();
    const onCancel = jest.fn();
    render(<PersonForm mode="edit" onSubmit={onSubmit} onCancel={onCancel} />);

    fireEvent.click(screen.getByText('Cancel'));
    expect(onCancel).toHaveBeenCalled();
  });

  it('should clear name error when user starts typing', () => {
    const onSubmit = jest.fn();
    render(<PersonForm mode="create" onSubmit={onSubmit} />);

    fireEvent.click(screen.getByText('Create Person'));
    expect(screen.getByTestId('name-error')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'J' } });
    expect(screen.queryByTestId('name-error')).not.toBeInTheDocument();
  });
});
