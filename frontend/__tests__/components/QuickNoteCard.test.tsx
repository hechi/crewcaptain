import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import QuickNoteCard from '@/components/quick-notes/QuickNoteCard';
import { QuickNote } from '@/types/quick-note';

describe('QuickNoteCard', () => {
  const mockNote: QuickNote = {
    id: 'note-1',
    personId: null,
    text: 'Remember to follow up on project timeline',
    sensitive: false,
    status: 'INBOX',
    createdAt: '2026-05-10T10:00:00Z',
    updatedAt: '2026-05-10T10:00:00Z',
  };

  const mockOnArchive = jest.fn();
  const mockOnConvert = jest.fn();
  const mockOnAttach = jest.fn();
  const mockOnDelete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render quick note text', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    expect(screen.getByTestId('quick-note-text')).toHaveTextContent('Remember to follow up on project timeline');
  });

  it('should render formatted date', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    expect(screen.getByTestId('quick-note-date')).toBeInTheDocument();
  });

  it('should render status badge', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    expect(screen.getByTestId('quick-note-status')).toHaveTextContent('INBOX');
  });

  it('should not render sensitive badge when not sensitive', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    expect(screen.queryByTestId('quick-note-sensitive-badge')).not.toBeInTheDocument();
  });

  it('should render sensitive badge when sensitive', () => {
    const sensitiveNote: QuickNote = { ...mockNote, sensitive: true };
    render(
      <QuickNoteCard
        quickNote={sensitiveNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    expect(screen.getByTestId('quick-note-sensitive-badge')).toBeInTheDocument();
  });

  it('should show all action buttons for INBOX notes', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    expect(screen.getByTestId('quick-note-attach-btn')).toBeInTheDocument();
    expect(screen.getByTestId('quick-note-convert-btn')).toBeInTheDocument();
    expect(screen.getByTestId('quick-note-archive-btn')).toBeInTheDocument();
    expect(screen.getByTestId('quick-note-delete-btn')).toBeInTheDocument();
  });

  it('should only show delete button for non-INBOX notes', () => {
    const archivedNote: QuickNote = { ...mockNote, status: 'ARCHIVED' };
    render(
      <QuickNoteCard
        quickNote={archivedNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    expect(screen.queryByTestId('quick-note-attach-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('quick-note-convert-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('quick-note-archive-btn')).not.toBeInTheDocument();
    expect(screen.getByTestId('quick-note-delete-btn')).toBeInTheDocument();
  });

  it('should call onAttach when attach button is clicked', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    fireEvent.click(screen.getByTestId('quick-note-attach-btn'));
    expect(mockOnAttach).toHaveBeenCalledWith('note-1');
  });

  it('should call onConvert when convert button is clicked', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    fireEvent.click(screen.getByTestId('quick-note-convert-btn'));
    expect(mockOnConvert).toHaveBeenCalledWith('note-1');
  });

  it('should call onArchive when archive button is clicked', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    fireEvent.click(screen.getByTestId('quick-note-archive-btn'));
    expect(mockOnArchive).toHaveBeenCalledWith('note-1');
  });

  it('should call onDelete when delete button is clicked', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    fireEvent.click(screen.getByTestId('quick-note-delete-btn'));
    expect(mockOnDelete).toHaveBeenCalledWith('note-1');
  });

  it('should render with correct test id', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    expect(screen.getByTestId('quick-note-card-note-1')).toBeInTheDocument();
  });

  it('should have accessible labels on action buttons', () => {
    render(
      <QuickNoteCard
        quickNote={mockNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onDelete={mockOnDelete}
      />
    );
    expect(screen.getByLabelText('Attach to 1:1')).toBeInTheDocument();
    expect(screen.getByLabelText('Convert to action item')).toBeInTheDocument();
    expect(screen.getByLabelText('Archive')).toBeInTheDocument();
    expect(screen.getByLabelText('Delete quick note')).toBeInTheDocument();
  });
});
