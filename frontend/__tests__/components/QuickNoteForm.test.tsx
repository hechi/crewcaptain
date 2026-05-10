import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import QuickNoteForm from '@/components/quick-notes/QuickNoteForm';

describe('QuickNoteForm', () => {
  const mockOnSubmit = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the form', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    expect(screen.getByTestId('quick-note-form')).toBeInTheDocument();
  });

  it('should render text input with placeholder', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    expect(screen.getByPlaceholderText("Quick capture — what's on your mind?")).toBeInTheDocument();
  });

  it('should render sensitive checkbox', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    expect(screen.getByTestId('quick-note-sensitive-checkbox')).toBeInTheDocument();
  });

  it('should render submit button', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    expect(screen.getByTestId('quick-note-submit-btn')).toHaveTextContent('Capture');
  });

  it('should disable submit button when text is empty', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    expect(screen.getByTestId('quick-note-submit-btn')).toBeDisabled();
  });

  it('should enable submit button when text is entered', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    fireEvent.change(screen.getByTestId('quick-note-text-input'), { target: { value: 'Test note' } });
    expect(screen.getByTestId('quick-note-submit-btn')).not.toBeDisabled();
  });

  it('should call onSubmit with text when form is submitted', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    fireEvent.change(screen.getByTestId('quick-note-text-input'), { target: { value: 'My quick note' } });
    fireEvent.click(screen.getByTestId('quick-note-submit-btn'));

    expect(mockOnSubmit).toHaveBeenCalledWith({
      text: 'My quick note',
      sensitive: undefined,
    });
  });

  it('should call onSubmit with sensitive flag when checked', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    fireEvent.change(screen.getByTestId('quick-note-text-input'), { target: { value: 'Sensitive note' } });
    fireEvent.click(screen.getByTestId('quick-note-sensitive-checkbox'));
    fireEvent.click(screen.getByTestId('quick-note-submit-btn'));

    expect(mockOnSubmit).toHaveBeenCalledWith({
      text: 'Sensitive note',
      sensitive: true,
    });
  });

  it('should clear form after submission', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    const input = screen.getByTestId('quick-note-text-input') as HTMLTextAreaElement;
    fireEvent.change(input, { target: { value: 'Test' } });
    fireEvent.click(screen.getByTestId('quick-note-submit-btn'));

    expect(input.value).toBe('');
  });

  it('should show Saving text when isSubmitting is true', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} isSubmitting={true} />);
    expect(screen.getByTestId('quick-note-submit-btn')).toHaveTextContent('Saving...');
  });

  it('should not submit when text is only whitespace', () => {
    render(<QuickNoteForm onSubmit={mockOnSubmit} />);
    fireEvent.change(screen.getByTestId('quick-note-text-input'), { target: { value: '   ' } });
    fireEvent.click(screen.getByTestId('quick-note-submit-btn'));

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });
});
