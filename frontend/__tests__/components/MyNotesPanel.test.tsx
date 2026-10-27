import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import MyNotesPanel from '@/components/quick-notes/MyNotesPanel';
import {
  listQuickNotes,
  createQuickNote,
  updateQuickNote,
  deleteQuickNote,
  moveQuickNote,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import { QuickNote } from '@/types/quick-note';

jest.mock('@/lib/api-client', () => ({
  listQuickNotes: jest.fn(),
  createQuickNote: jest.fn(),
  updateQuickNote: jest.fn(),
  deleteQuickNote: jest.fn(),
  moveQuickNote: jest.fn(),
}));

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: jest.fn(),
}));

const mockList = listQuickNotes as jest.MockedFunction<typeof listQuickNotes>;
const mockCreate = createQuickNote as jest.MockedFunction<typeof createQuickNote>;
const mockUpdate = updateQuickNote as jest.MockedFunction<typeof updateQuickNote>;
const mockDelete = deleteQuickNote as jest.MockedFunction<typeof deleteQuickNote>;
const mockMove = moveQuickNote as jest.MockedFunction<typeof moveQuickNote>;
const mockUseStableToken = useStableToken as jest.MockedFunction<typeof useStableToken>;

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

function paginated(content: QuickNote[]) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 };
}

describe('MyNotesPanel', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseStableToken.mockReturnValue({
      getToken: () => 'test-token',
      isAuthenticated: true,
      status: 'authenticated',
    });
    mockList.mockResolvedValue(paginated([]));
  });

  it('renders the left-edge trigger button when authenticated', () => {
    render(<MyNotesPanel />);
    expect(screen.getByTestId('my-notes-panel-trigger')).toBeInTheDocument();
  });

  it('renders nothing when not authenticated', () => {
    mockUseStableToken.mockReturnValue({
      getToken: () => null,
      isAuthenticated: false,
      status: 'unauthenticated',
    });
    const { container } = render(<MyNotesPanel />);
    expect(container.innerHTML).toBe('');
  });

  it('opens the panel when the trigger is clicked', async () => {
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    expect(await screen.findByTestId('my-notes-panel')).toBeInTheDocument();
  });

  it('opens on Ctrl+Shift+N', async () => {
    render(<MyNotesPanel />);
    fireEvent.keyDown(document, { key: 'N', ctrlKey: true, shiftKey: true });
    expect(await screen.findByTestId('my-notes-panel')).toBeInTheDocument();
  });

  it('has accessible dialog attributes', async () => {
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    const panel = await screen.findByTestId('my-notes-panel');
    expect(panel).toHaveAttribute('role', 'dialog');
    expect(panel).toHaveAttribute('aria-modal', 'true');
    expect(panel).toHaveAttribute('aria-labelledby', 'my-notes-panel-title');
  });

  it('closes on Escape', async () => {
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    expect(await screen.findByTestId('my-notes-panel')).toBeInTheDocument();
    fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByTestId('my-notes-panel')).not.toBeInTheDocument());
  });

  it('closes when the backdrop is clicked', async () => {
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    expect(await screen.findByTestId('my-notes-panel')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('my-notes-panel-backdrop'));
    await waitFor(() => expect(screen.queryByTestId('my-notes-panel')).not.toBeInTheDocument());
  });

  it('loads and renders self-assigned notes when opened', async () => {
    mockList.mockResolvedValue(
      paginated([
        note({ id: 'a', text: 'First note', displayOrder: 0 }),
        note({ id: 'b', text: 'Second note', displayOrder: 1 }),
      ])
    );
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));

    expect(await screen.findByText('First note')).toBeInTheDocument();
    expect(screen.getByText('Second note')).toBeInTheDocument();
    // Loaded as self-assigned notes.
    expect(mockList).toHaveBeenCalledWith('test-token', expect.objectContaining({ selfAssigned: true }));
  });

  it('creates a note from the panel', async () => {
    mockCreate.mockResolvedValue(note({ id: 'new', text: 'Brand new' }));
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    await screen.findByTestId('my-notes-panel');

    fireEvent.change(screen.getByTestId('my-notes-panel-new-text'), {
      target: { value: 'Brand new' },
    });
    fireEvent.click(screen.getByTestId('my-notes-panel-new-submit'));

    await waitFor(() =>
      expect(mockCreate).toHaveBeenCalledWith(
        'test-token',
        expect.objectContaining({ text: 'Brand new', selfAssigned: true })
      )
    );
  });

  it('moves a note up by calling the API with UP', async () => {
    mockList.mockResolvedValue(
      paginated([
        note({ id: 'a', text: 'Top note', displayOrder: 0 }),
        note({ id: 'b', text: 'Bottom note', displayOrder: 1 }),
      ])
    );
    mockMove.mockResolvedValue(note({ id: 'b', text: 'Bottom note', displayOrder: 0 }));
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    await screen.findByText('Bottom note');

    const card = screen.getByTestId('my-notes-panel-card-b');
    fireEvent.click(within(card).getByTestId('my-notes-panel-move-up-b'));

    await waitFor(() => expect(mockMove).toHaveBeenCalledWith('test-token', 'b', 'UP'));
  });

  it('moves a note down by calling the API with DOWN', async () => {
    mockList.mockResolvedValue(
      paginated([
        note({ id: 'a', text: 'Top note', displayOrder: 0 }),
        note({ id: 'b', text: 'Bottom note', displayOrder: 1 }),
      ])
    );
    mockMove.mockResolvedValue(note({ id: 'a', text: 'Top note', displayOrder: 1 }));
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    await screen.findByText('Top note');

    const card = screen.getByTestId('my-notes-panel-card-a');
    fireEvent.click(within(card).getByTestId('my-notes-panel-move-down-a'));

    await waitFor(() => expect(mockMove).toHaveBeenCalledWith('test-token', 'a', 'DOWN'));
  });

  it('disables move-up on the first note and move-down on the last note', async () => {
    mockList.mockResolvedValue(
      paginated([
        note({ id: 'a', text: 'Top note', displayOrder: 0 }),
        note({ id: 'b', text: 'Bottom note', displayOrder: 1 }),
      ])
    );
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    await screen.findByText('Top note');

    const first = screen.getByTestId('my-notes-panel-card-a');
    const last = screen.getByTestId('my-notes-panel-card-b');
    expect(within(first).getByTestId('my-notes-panel-move-up-a')).toBeDisabled();
    expect(within(last).getByTestId('my-notes-panel-move-down-b')).toBeDisabled();
  });

  it('edits a note text and tag', async () => {
    mockList.mockResolvedValue(paginated([note({ id: 'a', text: 'Editable', tag: null })]));
    mockUpdate.mockResolvedValue(note({ id: 'a', text: 'Edited', tag: 'work' }));
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    await screen.findByText('Editable');

    const card = screen.getByTestId('my-notes-panel-card-a');
    fireEvent.click(within(card).getByTestId('my-notes-panel-edit-a'));
    fireEvent.change(within(card).getByTestId('my-notes-panel-edit-text-a'), {
      target: { value: 'Edited' },
    });
    fireEvent.change(within(card).getByTestId('my-notes-panel-edit-tag-a'), {
      target: { value: 'work' },
    });
    fireEvent.click(within(card).getByTestId('my-notes-panel-edit-save-a'));

    await waitFor(() =>
      expect(mockUpdate).toHaveBeenCalledWith(
        'test-token',
        'a',
        expect.objectContaining({ text: 'Edited', tag: 'work', updateTag: true })
      )
    );
  });

  it('deletes a note', async () => {
    mockList.mockResolvedValue(paginated([note({ id: 'a', text: 'Delete me' })]));
    mockDelete.mockResolvedValue(undefined);
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    await screen.findByText('Delete me');

    const card = screen.getByTestId('my-notes-panel-card-a');
    fireEvent.click(within(card).getByTestId('my-notes-panel-delete-a'));

    await waitFor(() => expect(mockDelete).toHaveBeenCalledWith('test-token', 'a'));
  });

  it('filters notes by tag', async () => {
    mockList.mockResolvedValue(
      paginated([
        note({ id: 'a', text: 'Work note', tag: 'work' }),
        note({ id: 'b', text: 'Home note', tag: 'home' }),
      ])
    );
    render(<MyNotesPanel />);
    fireEvent.click(screen.getByTestId('my-notes-panel-trigger'));
    await screen.findByText('Work note');

    fireEvent.click(screen.getByTestId('my-notes-panel-tag-filter-work'));

    // Re-fetches scoped to the selected tag.
    await waitFor(() =>
      expect(mockList).toHaveBeenCalledWith('test-token', expect.objectContaining({ tag: 'work' }))
    );
  });
});
