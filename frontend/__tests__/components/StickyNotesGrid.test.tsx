import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import StickyNotesGrid from '@/components/StickyNotesGrid';
import { PinnedRememberItem } from '@/types/person';

const mockItems: PinnedRememberItem[] = [
  { id: '1', text: 'Has 2 kids — picks up early Fridays', color: 'cyan', tag: 'Family', sensitive: false, displayOrder: 0, createdAt: '2024-01-01T00:00:00Z' },
  { id: '2', text: 'Building house — closing Q4', color: 'amber', tag: 'Life event', sensitive: false, displayOrder: 1, createdAt: '2024-01-02T00:00:00Z' },
  { id: '3', text: 'Sensitive personal info', color: 'pink', tag: null, sensitive: true, displayOrder: 2, createdAt: '2024-01-03T00:00:00Z' },
];

describe('StickyNotesGrid', () => {
  const defaultProps = {
    items: mockItems,
    onAdd: jest.fn(),
    onUpdate: jest.fn(),
    onRemove: jest.fn(),
    onReorder: jest.fn(),
  };

  beforeEach(() => jest.clearAllMocks());

  it('renders sticky notes as cards', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    const cards = screen.getAllByTestId('sticky-note-card');
    expect(cards).toHaveLength(3);
  });

  it('displays tag labels on notes', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    expect(screen.getByText('Family')).toBeInTheDocument();
    expect(screen.getByText('Life event')).toBeInTheDocument();
  });

  it('hides sensitive note content and shows placeholder', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    expect(screen.getByText('Sensitive note — view to reveal')).toBeInTheDocument();
    expect(screen.queryByText('Sensitive personal info')).not.toBeInTheDocument();
  });

  it('reveals sensitive note content when eye button is clicked', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    const revealButton = screen.getByLabelText('Reveal sensitive content');
    fireEvent.click(revealButton);
    expect(screen.getByText('Sensitive personal info')).toBeInTheDocument();
  });

  it('shows empty state when no items', () => {
    render(<StickyNotesGrid {...defaultProps} items={[]} />);
    expect(screen.getByText(/No sticky notes yet/)).toBeInTheDocument();
  });

  it('shows Add sticky note button', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    expect(screen.getByTestId('add-sticky-note-button')).toBeInTheDocument();
  });

  it('opens composer when Add button is clicked', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    fireEvent.click(screen.getByTestId('add-sticky-note-button'));
    expect(screen.getByTestId('sticky-note-composer')).toBeInTheDocument();
  });

  it('calls onAdd with text, color, tag, and sensitive when saving a new note', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    fireEvent.click(screen.getByTestId('add-sticky-note-button'));

    const textarea = screen.getByLabelText('Sticky note text');
    fireEvent.change(textarea, { target: { value: 'New note content' } });

    const tagInput = screen.getByLabelText('Sticky note tag');
    fireEvent.change(tagInput, { target: { value: 'Docs' } });

    fireEvent.click(screen.getByTestId('save-sticky-note'));

    expect(defaultProps.onAdd).toHaveBeenCalledWith({
      text: 'New note content',
      color: 'cyan',
      tag: 'Docs',
      sensitive: false,
    });
  });

  it('calls onRemove and shows undo toast on delete', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    const deleteButtons = screen.getAllByTestId('delete-sticky-note');
    fireEvent.click(deleteButtons[0]);

    expect(defaultProps.onRemove).toHaveBeenCalledWith('1');
    expect(screen.getByTestId('undo-toast')).toBeInTheDocument();
    expect(screen.getByText('Sticky note deleted')).toBeInTheDocument();
  });

  it('calls onAdd (undo) when Undo button is clicked after delete', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    const deleteButtons = screen.getAllByTestId('delete-sticky-note');
    fireEvent.click(deleteButtons[0]);

    fireEvent.click(screen.getByTestId('undo-button'));
    expect(defaultProps.onAdd).toHaveBeenCalledWith({
      text: 'Has 2 kids — picks up early Fridays',
      color: 'cyan',
      tag: 'Family',
      sensitive: false,
    });
  });

  it('opens editor when edit button is clicked and calls onUpdate on save', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    const editButtons = screen.getAllByTestId('edit-sticky-note');
    fireEvent.click(editButtons[0]);

    expect(screen.getByTestId('sticky-note-editor')).toBeInTheDocument();

    const textarea = screen.getByLabelText('Edit sticky note text');
    fireEvent.change(textarea, { target: { value: 'Updated text' } });

    fireEvent.click(screen.getByTestId('save-edit-sticky-note'));
    expect(defaultProps.onUpdate).toHaveBeenCalledWith('1', expect.objectContaining({ text: 'Updated text' }));
  });

  it('supports drag-and-drop reordering', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    const cards = screen.getAllByTestId('sticky-note-card');

    // Simulate drag from index 0 to index 1
    fireEvent.dragStart(cards[0]);
    fireEvent.dragOver(cards[1], { preventDefault: jest.fn() });
    fireEvent.drop(cards[1], { preventDefault: jest.fn() });

    expect(defaultProps.onReorder).toHaveBeenCalledWith(['2', '1', '3']);
  });

  it('supports keyboard reorder with arrow keys', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    const dragHandles = screen.getAllByLabelText(/reorder sticky note/i);
    fireEvent.keyDown(dragHandles[0], { key: 'ArrowDown' });
    expect(defaultProps.onReorder).toHaveBeenCalledWith(['2', '1', '3']);
  });

  it('shows soft limit warning when approaching 10 notes', () => {
    const manyItems: PinnedRememberItem[] = Array.from({ length: 9 }, (_, i) => ({
      id: String(i),
      text: `Note ${i}`,
      color: 'cyan' as const,
      tag: null,
      sensitive: false,
      displayOrder: i,
      createdAt: '2024-01-01T00:00:00Z',
    }));
    render(<StickyNotesGrid {...defaultProps} items={manyItems} />);
    expect(screen.getByText(/1 sticky notes remaining/)).toBeInTheDocument();
  });

  it('truncates long text to 100 characters with ellipsis', () => {
    const longItem: PinnedRememberItem = {
      id: 'long',
      text: 'A'.repeat(150),
      color: 'cyan',
      tag: null,
      sensitive: false,
      displayOrder: 0,
      createdAt: '2024-01-01T00:00:00Z',
    };
    render(<StickyNotesGrid {...defaultProps} items={[longItem]} />);
    expect(screen.getByText('A'.repeat(100) + '…')).toBeInTheDocument();
  });

  it('shows starter templates in composer', () => {
    render(<StickyNotesGrid {...defaultProps} items={[]} />);
    fireEvent.click(screen.getByTestId('add-sticky-note-button'));
    expect(screen.getByText('Family')).toBeInTheDocument();
    expect(screen.getByText('Link')).toBeInTheDocument();
    expect(screen.getByText('Docs')).toBeInTheDocument();
  });

  it('does not save when text is empty', () => {
    render(<StickyNotesGrid {...defaultProps} />);
    fireEvent.click(screen.getByTestId('add-sticky-note-button'));
    const saveButton = screen.getByTestId('save-sticky-note');
    expect(saveButton).toBeDisabled();
    fireEvent.click(saveButton);
    expect(defaultProps.onAdd).not.toHaveBeenCalled();
  });
});
