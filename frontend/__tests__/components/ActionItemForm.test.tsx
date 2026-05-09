import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ActionItemForm from '@/components/action-items/ActionItemForm';

describe('ActionItemForm', () => {
  it('renders all form fields', () => {
    render(<ActionItemForm mode="create" onSubmit={jest.fn()} />);
    expect(screen.getByTestId('action-item-title-input')).toBeInTheDocument();
    expect(screen.getByTestId('action-item-description-input')).toBeInTheDocument();
    expect(screen.getByTestId('action-item-owner-select')).toBeInTheDocument();
    expect(screen.getByTestId('action-item-due-date-input')).toBeInTheDocument();
  });

  it('shows create button text in create mode', () => {
    render(<ActionItemForm mode="create" onSubmit={jest.fn()} />);
    expect(screen.getByTestId('action-item-submit-btn')).toHaveTextContent('Create Action Item');
  });

  it('shows save button text in edit mode', () => {
    render(<ActionItemForm mode="edit" onSubmit={jest.fn()} />);
    expect(screen.getByTestId('action-item-submit-btn')).toHaveTextContent('Save Changes');
  });

  it('pre-fills form with initial data', () => {
    render(
      <ActionItemForm
        mode="edit"
        initialData={{
          title: 'Existing task',
          description: 'Some description',
          ownerType: 'PERSON',
          dueDate: '2026-06-01',
        }}
        onSubmit={jest.fn()}
      />
    );
    expect(screen.getByTestId('action-item-title-input')).toHaveValue('Existing task');
    expect(screen.getByTestId('action-item-description-input')).toHaveValue('Some description');
    expect(screen.getByTestId('action-item-owner-select')).toHaveValue('PERSON');
    expect(screen.getByTestId('action-item-due-date-input')).toHaveValue('2026-06-01');
  });

  it('shows error when title is empty on submit', () => {
    const onSubmit = jest.fn();
    render(<ActionItemForm mode="create" onSubmit={onSubmit} />);
    fireEvent.click(screen.getByTestId('action-item-submit-btn'));
    expect(screen.getByTestId('form-error')).toHaveTextContent('Title is required');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('calls onSubmit with form data when valid', () => {
    const onSubmit = jest.fn();
    render(<ActionItemForm mode="create" onSubmit={onSubmit} />);

    fireEvent.change(screen.getByTestId('action-item-title-input'), { target: { value: 'New task' } });
    fireEvent.change(screen.getByTestId('action-item-description-input'), { target: { value: 'Details' } });
    fireEvent.change(screen.getByTestId('action-item-owner-select'), { target: { value: 'PERSON' } });
    fireEvent.change(screen.getByTestId('action-item-due-date-input'), { target: { value: '2026-06-15' } });
    fireEvent.click(screen.getByTestId('action-item-submit-btn'));

    expect(onSubmit).toHaveBeenCalledWith({
      title: 'New task',
      description: 'Details',
      ownerType: 'PERSON',
      dueDate: '2026-06-15',
    });
  });

  it('sends null description when empty', () => {
    const onSubmit = jest.fn();
    render(<ActionItemForm mode="create" onSubmit={onSubmit} />);

    fireEvent.change(screen.getByTestId('action-item-title-input'), { target: { value: 'Task' } });
    fireEvent.click(screen.getByTestId('action-item-submit-btn'));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      description: null,
      dueDate: null,
    }));
  });

  it('calls onCancel when cancel button is clicked', () => {
    const onCancel = jest.fn();
    render(<ActionItemForm mode="create" onSubmit={jest.fn()} onCancel={onCancel} />);
    fireEvent.click(screen.getByTestId('action-item-cancel-btn'));
    expect(onCancel).toHaveBeenCalled();
  });

  it('disables submit button when isSubmitting is true', () => {
    render(<ActionItemForm mode="create" onSubmit={jest.fn()} isSubmitting={true} />);
    expect(screen.getByTestId('action-item-submit-btn')).toBeDisabled();
    expect(screen.getByTestId('action-item-submit-btn')).toHaveTextContent('Saving...');
  });
});
