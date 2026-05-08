import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import OneOnOneEntryForm from '@/components/one-on-one/OneOnOneEntryForm';
import { OneOnOneEntry } from '@/types/one-on-one';

// Mock crypto.randomUUID for AgendaItemList
Object.defineProperty(globalThis, 'crypto', {
  value: { randomUUID: () => 'test-uuid-' + Math.random().toString(36).substr(2, 9) },
});

describe('OneOnOneEntryForm', () => {
  const mockOnSubmit = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders all sub-components in create mode', () => {
    render(<OneOnOneEntryForm onSubmit={mockOnSubmit} />);

    expect(screen.getByTestId('one-on-one-entry-form')).toBeInTheDocument();
    expect(screen.getByTestId('meeting-date-input')).toBeInTheDocument();
    expect(screen.getByTestId('agenda-item-list')).toBeInTheDocument();
    expect(screen.getByTestId('sensitive-toggle')).toBeInTheDocument();
    // MarkdownEditor instances
    expect(screen.getAllByTestId('markdown-editor')).toHaveLength(2);
  });

  it('shows "Create Entry" button in create mode', () => {
    render(<OneOnOneEntryForm onSubmit={mockOnSubmit} />);

    expect(screen.getByTestId('entry-form-submit')).toHaveTextContent('Create Entry');
  });

  it('shows "Update Entry" button in edit mode', () => {
    const existingEntry: OneOnOneEntry = {
      id: 'entry-1',
      personId: 'person-1',
      meetingDate: '2025-05-08T14:00:00Z',
      agendaItems: [],
      notesMarkdown: 'Some notes',
      outcomesMarkdown: null,
      sensitive: false,
      createdAt: '2025-05-08T14:00:00Z',
      updatedAt: '2025-05-08T14:00:00Z',
    };

    render(<OneOnOneEntryForm entry={existingEntry} onSubmit={mockOnSubmit} />);

    expect(screen.getByTestId('entry-form-submit')).toHaveTextContent('Update Entry');
  });

  it('validates meeting date presence and shows error when empty', () => {
    render(<OneOnOneEntryForm onSubmit={mockOnSubmit} />);

    // Clear the date input
    const dateInput = screen.getByTestId('meeting-date-input');
    fireEvent.change(dateInput, { target: { value: '' } });

    // Submit the form
    fireEvent.click(screen.getByTestId('entry-form-submit'));

    expect(screen.getByTestId('meeting-date-error')).toBeInTheDocument();
    expect(screen.getByText('Meeting date is required')).toBeInTheDocument();
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('calls onSubmit with correct data when form is valid', () => {
    render(<OneOnOneEntryForm onSubmit={mockOnSubmit} />);

    // Set a meeting date
    const dateInput = screen.getByTestId('meeting-date-input');
    fireEvent.change(dateInput, { target: { value: '2025-05-08T14:00' } });

    // Submit the form
    fireEvent.click(screen.getByTestId('entry-form-submit'));

    expect(mockOnSubmit).toHaveBeenCalledTimes(1);
    const formData = mockOnSubmit.mock.calls[0][0];
    expect(formData.meetingDate).toBeDefined();
    expect(formData.agendaItems).toEqual([]);
    expect(formData.sensitive).toBe(false);
  });

  it('prefills notes from template in create mode', () => {
    const template = '## Agenda\n- Review items';
    render(<OneOnOneEntryForm onSubmit={mockOnSubmit} templateMarkdown={template} />);

    // The markdown editor textarea should have the template content
    const textareas = screen.getAllByTestId('markdown-editor-textarea');
    // First textarea is notes, second is outcomes
    expect((textareas[0] as HTMLTextAreaElement).value).toBe(template);
  });

  it('renders cancel button when onCancel is provided', () => {
    render(<OneOnOneEntryForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    expect(screen.getByTestId('entry-form-cancel')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('entry-form-cancel'));
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('disables submit button when isSubmitting is true', () => {
    render(<OneOnOneEntryForm onSubmit={mockOnSubmit} isSubmitting={true} />);

    const submitButton = screen.getByTestId('entry-form-submit');
    expect(submitButton).toBeDisabled();
    expect(submitButton).toHaveTextContent('Saving...');
  });
});
