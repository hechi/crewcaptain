import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import MyNoteCard from '@/components/quick-notes/MyNoteCard';
import { QuickNote } from '@/types/quick-note';

function note(partial: Partial<QuickNote> & { id: string; text: string }): QuickNote {
  return {
    personId: null,
    sensitive: false,
    selfAssigned: true,
    status: 'INBOX',
    attachedEntryId: null,
    tag: null,
    displayOrder: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...partial,
  };
}

describe('MyNoteCard (shared card)', () => {
  it('renders the note text, date, and status', () => {
    render(<MyNoteCard note={note({ id: 'a', text: 'Hello note' })} />);
    expect(screen.getByTestId('my-note-text')).toHaveTextContent('Hello note');
    expect(screen.getByTestId('my-note-date')).toBeInTheDocument();
    expect(screen.getByTestId('my-note-status')).toHaveTextContent('INBOX');
  });

  it('shows a tag chip when tagged', () => {
    render(<MyNoteCard note={note({ id: 'a', text: 'x', tag: 'work' })} />);
    expect(screen.getByTestId('my-note-tag')).toHaveTextContent('work');
  });

  it('shows the sensitive badge when sensitive', () => {
    render(<MyNoteCard note={note({ id: 'a', text: 'x', sensitive: true })} />);
    expect(screen.getByTestId('my-note-sensitive-badge')).toBeInTheDocument();
  });

  it('edits text and tag via onEdit', () => {
    const onEdit = jest.fn();
    render(<MyNoteCard note={note({ id: 'a', text: 'Old', tag: null })} onEdit={onEdit} />);
    fireEvent.click(screen.getByTestId('my-note-edit-btn'));
    fireEvent.change(screen.getByTestId('my-note-edit-text-input'), { target: { value: 'New' } });
    fireEvent.change(screen.getByTestId('my-note-edit-tag-input'), { target: { value: 'tagged' } });
    fireEvent.click(screen.getByTestId('my-note-edit-save-btn'));
    expect(onEdit).toHaveBeenCalledWith('a', 'New', 'tagged');
  });

  it('cancels edit without calling onEdit', () => {
    const onEdit = jest.fn();
    render(<MyNoteCard note={note({ id: 'a', text: 'Old' })} onEdit={onEdit} />);
    fireEvent.click(screen.getByTestId('my-note-edit-btn'));
    fireEvent.change(screen.getByTestId('my-note-edit-text-input'), { target: { value: 'Discard' } });
    fireEvent.click(screen.getByTestId('my-note-edit-cancel-btn'));
    expect(onEdit).not.toHaveBeenCalled();
    expect(screen.getByTestId('my-note-text')).toHaveTextContent('Old');
  });

  it('converts to a TODO with a due date', () => {
    const onConvert = jest.fn();
    render(<MyNoteCard note={note({ id: 'a', text: 'x' })} onConvert={onConvert} />);
    fireEvent.change(screen.getByTestId('my-note-due-date-input'), { target: { value: '2026-07-01' } });
    fireEvent.click(screen.getByTestId('my-note-convert-btn'));
    expect(onConvert).toHaveBeenCalledWith('a', '2026-07-01');
  });

  it('archives and deletes', () => {
    const onArchive = jest.fn();
    const onDelete = jest.fn();
    render(<MyNoteCard note={note({ id: 'a', text: 'x' })} onArchive={onArchive} onDelete={onDelete} />);
    fireEvent.click(screen.getByTestId('my-note-archive-btn'));
    fireEvent.click(screen.getByTestId('my-note-delete-btn'));
    expect(onArchive).toHaveBeenCalledWith('a');
    expect(onDelete).toHaveBeenCalledWith('a');
  });

  it('renders reorder arrows and calls onMove, disabling at the edges', () => {
    const onMove = jest.fn();
    const { rerender } = render(
      <MyNoteCard note={note({ id: 'a', text: 'x' })} onMove={onMove} isFirst isLast={false} />
    );
    expect(screen.getByTestId('my-note-move-up-btn')).toBeDisabled();
    fireEvent.click(screen.getByTestId('my-note-move-down-btn'));
    expect(onMove).toHaveBeenCalledWith('a', 'DOWN');

    rerender(<MyNoteCard note={note({ id: 'a', text: 'x' })} onMove={onMove} isFirst={false} isLast />);
    expect(screen.getByTestId('my-note-move-down-btn')).toBeDisabled();
  });

  it('is draggable when drag handlers are supplied', () => {
    render(
      <MyNoteCard
        note={note({ id: 'a', text: 'x' })}
        onDragStart={jest.fn()}
        onDragOverNote={jest.fn()}
        onDragEnd={jest.fn()}
        onDropOnNote={jest.fn()}
      />
    );
    expect(screen.getByTestId('my-note-card-a')).toHaveAttribute('draggable', 'true');
    expect(screen.getByTestId('my-note-drag-handle-a')).toBeInTheDocument();
  });

  it('is not draggable without drag handlers', () => {
    render(<MyNoteCard note={note({ id: 'a', text: 'x' })} />);
    expect(screen.getByTestId('my-note-card-a')).toHaveAttribute('draggable', 'false');
  });

  it('fires drag callbacks on drag/drop', () => {
    const onDragStart = jest.fn();
    const onDragOverNote = jest.fn();
    const onDropOnNote = jest.fn();
    render(
      <MyNoteCard
        note={note({ id: 'a', text: 'x' })}
        onDragStart={onDragStart}
        onDragOverNote={onDragOverNote}
        onDragEnd={jest.fn()}
        onDropOnNote={onDropOnNote}
      />
    );
    const card = screen.getByTestId('my-note-card-a');
    const dt = { setData: jest.fn(), getData: () => 'a', effectAllowed: 'move', dropEffect: 'move' };
    fireEvent.dragStart(card, { dataTransfer: dt });
    fireEvent.dragOver(card, { dataTransfer: dt });
    fireEvent.drop(card, { dataTransfer: dt });
    expect(onDragStart).toHaveBeenCalledWith('a');
    expect(onDragOverNote).toHaveBeenCalledWith('a');
    expect(onDropOnNote).toHaveBeenCalledWith('a');
  });

  it('only shows convert/archive/edit for INBOX notes', () => {
    const cbs = { onConvert: jest.fn(), onArchive: jest.fn(), onEdit: jest.fn(), onDelete: jest.fn() };
    render(<MyNoteCard note={note({ id: 'a', text: 'x', status: 'ARCHIVED' })} {...cbs} />);
    expect(screen.queryByTestId('my-note-convert-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('my-note-archive-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('my-note-edit-btn')).not.toBeInTheDocument();
    // Delete remains available regardless of status.
    expect(screen.getByTestId('my-note-delete-btn')).toBeInTheDocument();
  });
});
