import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import MyNotesPage from '@/app/my-notes/page';

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  listQuickNotes: jest.fn(),
  createQuickNote: jest.fn(),
  archiveQuickNote: jest.fn(),
  deleteQuickNote: jest.fn(),
  convertQuickNote: jest.fn(),
  updateQuickNote: jest.fn(),
  moveQuickNote: jest.fn(),
  reorderQuickNote: jest.fn(),
  completeQuickNote: jest.fn(),
  listAllActionItems: jest.fn(),
  completeActionItemById: jest.fn(),
}));

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import {
  listQuickNotes,
  createQuickNote,
  archiveQuickNote,
  deleteQuickNote,
  convertQuickNote,
  updateQuickNote,
  moveQuickNote,
  reorderQuickNote,
  completeQuickNote,
  listAllActionItems,
  completeActionItemById,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockListQuickNotes = listQuickNotes as jest.MockedFunction<typeof listQuickNotes>;
const mockCreateQuickNote = createQuickNote as jest.MockedFunction<typeof createQuickNote>;
const mockArchiveQuickNote = archiveQuickNote as jest.MockedFunction<typeof archiveQuickNote>;
const mockDeleteQuickNote = deleteQuickNote as jest.MockedFunction<typeof deleteQuickNote>;
const mockConvertQuickNote = convertQuickNote as jest.MockedFunction<typeof convertQuickNote>;
const mockUpdateQuickNote = updateQuickNote as jest.MockedFunction<typeof updateQuickNote>;
const mockMoveQuickNote = moveQuickNote as jest.MockedFunction<typeof moveQuickNote>;
const mockReorderQuickNote = reorderQuickNote as jest.MockedFunction<typeof reorderQuickNote>;
const mockCompleteQuickNote = completeQuickNote as jest.MockedFunction<typeof completeQuickNote>;
const mockListAllActionItems = listAllActionItems as jest.MockedFunction<typeof listAllActionItems>;
const mockCompleteActionItem = completeActionItemById as jest.MockedFunction<typeof completeActionItemById>;
const mockUseStableToken = useStableToken as jest.MockedFunction<typeof useStableToken>;

const mockSelfAssignedNotes = {
  content: [
    {
      id: 'note-1',
      personId: null,
      text: 'My personal reminder',
      sensitive: false,
      selfAssigned: true,
      status: 'INBOX' as const,
      attachedEntryId: null,
      tag: 'hiring',
      displayOrder: 1,
      createdAt: '2026-05-15T10:00:00Z',
      updatedAt: '2026-05-15T10:00:00Z',
    },
    {
      id: 'note-2',
      personId: null,
      text: 'Sensitive personal note',
      sensitive: true,
      selfAssigned: true,
      status: 'INBOX' as const,
      attachedEntryId: null,
      tag: null,
      displayOrder: 0,
      createdAt: '2026-05-14T09:00:00Z',
      updatedAt: '2026-05-14T09:00:00Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
};

describe('MyNotesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseStableToken.mockReturnValue({
      getToken: () => 'test-token',
      isAuthenticated: true,
      status: 'authenticated',
    });
    mockListQuickNotes.mockResolvedValue(mockSelfAssignedNotes);
    mockListAllActionItems.mockResolvedValue({
      content: [
        {
          id: 'ai-1', personId: null, title: 'Prepare budget deck', description: null,
          ownerType: 'MANAGER', dueDate: null, status: 'OPEN', originatingEntryId: null,
          originatingQuickNoteId: 'note-9', createdAt: '2026-05-15T10:00:00Z', updatedAt: '2026-05-15T10:00:00Z',
        },
        {
          id: 'ai-2', personId: 'p1', title: 'Alice owns this', description: null,
          ownerType: 'PERSON', dueDate: null, status: 'OPEN', originatingEntryId: null,
          originatingQuickNoteId: null, createdAt: '2026-05-15T10:00:00Z', updatedAt: '2026-05-15T10:00:00Z',
        },
      ],
      page: 0, size: 100, totalElements: 2, totalPages: 1,
    });
  });

  it('shows separate Notes and My action items sections', async () => {
    render(<MyNotesPage />);
    expect(await screen.findByTestId('my-notes-section-notes')).toBeInTheDocument();
    expect(screen.getByTestId('my-notes-section-action-items')).toBeInTheDocument();
  });

  it('lists only my own open action items in the action items section', async () => {
    render(<MyNotesPage />);
    const section = await screen.findByTestId('my-notes-section-action-items');
    await waitFor(() => expect(section).toHaveTextContent('Prepare budget deck'));
    expect(section).not.toHaveTextContent('Alice owns this');
    expect(mockListAllActionItems).toHaveBeenCalledWith('test-token', { status: 'OPEN', page: 0, size: 100 });
  });

  it('completes a note via the Complete button', async () => {
    mockCompleteQuickNote.mockResolvedValue({ ...mockSelfAssignedNotes.content[0], status: 'DONE' as const });
    render(<MyNotesPage />);
    const buttons = await screen.findAllByTestId('my-note-complete-btn');
    fireEvent.click(buttons[0]);
    await waitFor(() => expect(mockCompleteQuickNote).toHaveBeenCalledWith('test-token', 'note-1'));
  });

  it('completes one of my action items from the action items section', async () => {
    mockCompleteActionItem.mockResolvedValue({} as never);
    render(<MyNotesPage />);
    const btn = await screen.findByTestId('action-item-complete-btn');
    fireEvent.click(btn);
    await waitFor(() => expect(mockCompleteActionItem).toHaveBeenCalledWith('test-token', 'ai-1'));
  });

  it('offers a Done filter for notes', async () => {
    render(<MyNotesPage />);
    fireEvent.click(await screen.findByTestId('filter-DONE'));
    await waitFor(() =>
      expect(mockListQuickNotes).toHaveBeenCalledWith(
        'test-token',
        expect.objectContaining({ status: 'DONE', selfAssigned: true })
      )
    );
  });

  it('refreshes my action items after making a note an action item', async () => {
    mockConvertQuickNote.mockResolvedValue({ ...mockSelfAssignedNotes.content[0], status: 'CONVERTED' as const });
    render(<MyNotesPage />);
    const btns = await screen.findAllByTestId('my-note-convert-btn');
    mockListAllActionItems.mockClear();
    fireEvent.click(btns[0]);
    await waitFor(() => expect(mockListAllActionItems).toHaveBeenCalled());
  });

  it('should show loading screen when session is loading', () => {
    mockUseStableToken.mockReturnValue({
      getToken: () => null,
      isAuthenticated: false,
      status: 'loading',
    });

    render(<MyNotesPage />);
    expect(screen.getByText('Loading my notes')).toBeInTheDocument();
  });

  it('should show unauthenticated message when not signed in', () => {
    mockUseStableToken.mockReturnValue({
      getToken: () => null,
      isAuthenticated: false,
      status: 'unauthenticated',
    });

    render(<MyNotesPage />);
    expect(screen.getByTestId('unauthenticated')).toBeInTheDocument();
  });

  it('should render page title', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByText('My Notes')).toBeInTheDocument();
    });
  });

  it('should fetch self-assigned notes on load', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(mockListQuickNotes).toHaveBeenCalledWith('test-token', {
        selfAssigned: true,
        status: undefined,
        tag: undefined,
        page: 0,
        size: 20,
      });
    });
  });

  it('should display self-assigned notes', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByText('My personal reminder')).toBeInTheDocument();
      expect(screen.getByText('Sensitive personal note')).toBeInTheDocument();
    });
  });

  it('should show sensitive badge on sensitive notes', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-2')).toBeInTheDocument();
    });
    const sensitiveCard = screen.getByTestId('my-note-card-note-2');
    expect(sensitiveCard.querySelector('[data-testid="my-note-sensitive-badge"]')).toBeInTheDocument();
  });

  it('should show a tag chip on tagged notes', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });
    const taggedCard = screen.getByTestId('my-note-card-note-1');
    const chip = taggedCard.querySelector('[data-testid="my-note-tag"]');
    expect(chip).toBeInTheDocument();
    expect(chip).toHaveTextContent('hiring');
  });

  it('should show empty state when no notes exist', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByText('No personal notes yet — capture something above!')).toBeInTheDocument();
    });
  });

  it('should create note with selfAssigned=true', async () => {
    mockCreateQuickNote.mockResolvedValue({
      id: 'new-note',
      personId: null,
      text: 'New personal note',
      sensitive: false,
      selfAssigned: true,
      status: 'INBOX',
      attachedEntryId: null,
      tag: null,
      displayOrder: 0,
      createdAt: '2026-05-15T12:00:00Z',
      updatedAt: '2026-05-15T12:00:00Z',
    });

    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('quick-note-form')).toBeInTheDocument();
    });

    const textInput = screen.getByTestId('quick-note-text-input');
    fireEvent.change(textInput, { target: { value: 'New personal note' } });
    fireEvent.click(screen.getByTestId('quick-note-submit-btn'));

    await waitFor(() => {
      expect(mockCreateQuickNote).toHaveBeenCalledWith('test-token', {
        text: 'New personal note',
        sensitive: undefined,
        tag: undefined,
        selfAssigned: true,
      });
    });
  });

  it('should archive a note', async () => {
    mockArchiveQuickNote.mockResolvedValue({
      id: 'note-1',
      personId: null,
      text: 'My personal reminder',
      sensitive: false,
      selfAssigned: true,
      status: 'ARCHIVED',
      attachedEntryId: null,
      tag: 'hiring',
      displayOrder: 0,
      createdAt: '2026-05-15T10:00:00Z',
      updatedAt: '2026-05-15T11:00:00Z',
    });

    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });

    const archiveBtn = screen.getAllByTestId('my-note-archive-btn')[0];
    fireEvent.click(archiveBtn);

    await waitFor(() => {
      expect(mockArchiveQuickNote).toHaveBeenCalledWith('test-token', 'note-1');
    });
  });

  it('should delete a note', async () => {
    mockDeleteQuickNote.mockResolvedValue(undefined);

    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });

    const deleteBtn = screen.getAllByTestId('my-note-delete-btn')[0];
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(mockDeleteQuickNote).toHaveBeenCalledWith('test-token', 'note-1');
    });
  });

  it('should convert a note to a personal TODO', async () => {
    mockConvertQuickNote.mockResolvedValue({
      id: 'note-1',
      personId: null,
      text: 'My personal reminder',
      sensitive: false,
      selfAssigned: true,
      status: 'CONVERTED',
      attachedEntryId: null,
      tag: 'hiring',
      displayOrder: 0,
      createdAt: '2026-05-15T10:00:00Z',
      updatedAt: '2026-05-15T11:00:00Z',
    });

    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });

    const convertBtn = screen.getAllByTestId('my-note-convert-btn')[0];
    fireEvent.click(convertBtn);

    await waitFor(() => {
      expect(mockConvertQuickNote).toHaveBeenCalledWith('test-token', 'note-1', { dueDate: undefined });
    });
  });

  it('should convert a note to a personal TODO with a due date', async () => {
    mockConvertQuickNote.mockResolvedValue({
      id: 'note-1',
      personId: null,
      text: 'My personal reminder',
      sensitive: false,
      selfAssigned: true,
      status: 'CONVERTED',
      attachedEntryId: null,
      tag: 'hiring',
      displayOrder: 0,
      createdAt: '2026-05-15T10:00:00Z',
      updatedAt: '2026-05-15T11:00:00Z',
    });

    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });

    const card = screen.getByTestId('my-note-card-note-1');
    const dueDateInput = card.querySelector('[data-testid="my-note-due-date-input"]') as HTMLInputElement;
    fireEvent.change(dueDateInput, { target: { value: '2026-07-01' } });

    const convertBtn = screen.getAllByTestId('my-note-convert-btn')[0];
    fireEvent.click(convertBtn);

    await waitFor(() => {
      expect(mockConvertQuickNote).toHaveBeenCalledWith('test-token', 'note-1', { dueDate: '2026-07-01' });
    });
  });

  it('should show error message on fetch failure', async () => {
    mockListQuickNotes.mockRejectedValue(new Error('Network error'));

    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toHaveTextContent('Network error');
    });
  });

  it('should render status filter buttons', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('filter-all')).toBeInTheDocument();
      expect(screen.getByTestId('filter-INBOX')).toBeInTheDocument();
      expect(screen.getByTestId('filter-ARCHIVED')).toBeInTheDocument();
    });
  });

  it('should filter by status when filter button is clicked', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('filter-INBOX')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('filter-INBOX'));

    await waitFor(() => {
      expect(mockListQuickNotes).toHaveBeenCalledWith('test-token', {
        selfAssigned: true,
        status: 'INBOX',
        tag: undefined,
        page: 0,
        size: 20,
      });
    });
  });

  it('should filter by tag when a tag filter chip is clicked', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-notes-tag-filter')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tag-filter-hiring'));

    await waitFor(() => {
      expect(mockListQuickNotes).toHaveBeenCalledWith('test-token', {
        selfAssigned: true,
        status: undefined,
        tag: 'hiring',
        page: 0,
        size: 20,
      });
    });
  });

  it('should enter edit mode when the edit button is clicked', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });

    const card = screen.getByTestId('my-note-card-note-1');
    fireEvent.click(card.querySelector('[data-testid="my-note-edit-btn"]')!);

    const textInput = card.querySelector('[data-testid="my-note-edit-text-input"]') as HTMLTextAreaElement;
    const tagInput = card.querySelector('[data-testid="my-note-edit-tag-input"]') as HTMLInputElement;
    expect(textInput).toBeInTheDocument();
    expect(tagInput).toBeInTheDocument();
    // Pre-filled with current values
    expect(textInput.value).toBe('My personal reminder');
    expect(tagInput.value).toBe('hiring');
  });

  it('should save edited text and tag', async () => {
    mockUpdateQuickNote.mockResolvedValue({
      id: 'note-1',
      personId: null,
      text: 'Edited reminder',
      sensitive: false,
      selfAssigned: true,
      status: 'INBOX',
      attachedEntryId: null,
      tag: 'urgent',
      displayOrder: 0,
      createdAt: '2026-05-15T10:00:00Z',
      updatedAt: '2026-05-15T12:00:00Z',
    });

    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });

    const card = screen.getByTestId('my-note-card-note-1');
    fireEvent.click(card.querySelector('[data-testid="my-note-edit-btn"]')!);

    const textInput = card.querySelector('[data-testid="my-note-edit-text-input"]') as HTMLTextAreaElement;
    const tagInput = card.querySelector('[data-testid="my-note-edit-tag-input"]') as HTMLInputElement;
    fireEvent.change(textInput, { target: { value: 'Edited reminder' } });
    fireEvent.change(tagInput, { target: { value: 'urgent' } });
    fireEvent.click(card.querySelector('[data-testid="my-note-edit-save-btn"]')!);

    await waitFor(() => {
      expect(mockUpdateQuickNote).toHaveBeenCalledWith('test-token', 'note-1', {
        text: 'Edited reminder',
        tag: 'urgent',
        updateTag: true,
      });
    });
  });

  it('should clear the tag when the edit tag field is emptied', async () => {
    mockUpdateQuickNote.mockResolvedValue({
      id: 'note-1',
      personId: null,
      text: 'My personal reminder',
      sensitive: false,
      selfAssigned: true,
      status: 'INBOX',
      attachedEntryId: null,
      tag: null,
      displayOrder: 0,
      createdAt: '2026-05-15T10:00:00Z',
      updatedAt: '2026-05-15T12:00:00Z',
    });

    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });

    const card = screen.getByTestId('my-note-card-note-1');
    fireEvent.click(card.querySelector('[data-testid="my-note-edit-btn"]')!);

    const tagInput = card.querySelector('[data-testid="my-note-edit-tag-input"]') as HTMLInputElement;
    fireEvent.change(tagInput, { target: { value: '' } });
    fireEvent.click(card.querySelector('[data-testid="my-note-edit-save-btn"]')!);

    await waitFor(() => {
      expect(mockUpdateQuickNote).toHaveBeenCalledWith('test-token', 'note-1', {
        text: 'My personal reminder',
        tag: null,
        updateTag: true,
      });
    });
  });

  it('should exit edit mode without saving on cancel', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });

    const card = screen.getByTestId('my-note-card-note-1');
    fireEvent.click(card.querySelector('[data-testid="my-note-edit-btn"]')!);
    const textInput = card.querySelector('[data-testid="my-note-edit-text-input"]') as HTMLTextAreaElement;
    fireEvent.change(textInput, { target: { value: 'Discarded change' } });
    fireEvent.click(card.querySelector('[data-testid="my-note-edit-cancel-btn"]')!);

    expect(mockUpdateQuickNote).not.toHaveBeenCalled();
    expect(card.querySelector('[data-testid="my-note-edit-text-input"]')).not.toBeInTheDocument();
    expect(card.querySelector('[data-testid="my-note-text"]')).toHaveTextContent('My personal reminder');
  });

  it('renders draggable, reorderable note cards (consistent with the panel)', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });
    // The page cards are now the shared card: draggable and with reorder arrows.
    expect(screen.getByTestId('my-note-card-note-1')).toHaveAttribute('draggable', 'true');
    const card = screen.getByTestId('my-note-card-note-1');
    expect(card.querySelector('[data-testid="my-note-move-up-btn"]')).toBeInTheDocument();
    expect(card.querySelector('[data-testid="my-note-move-down-btn"]')).toBeInTheDocument();
  });

  it('moves a note via the up arrow', async () => {
    mockMoveQuickNote.mockResolvedValue(mockSelfAssignedNotes.content[1] as never);
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-2')).toBeInTheDocument();
    });
    // note-2 is the second card -> its move-up is enabled.
    const card = screen.getByTestId('my-note-card-note-2');
    fireEvent.click(card.querySelector('[data-testid="my-note-move-up-btn"]')!);

    await waitFor(() => expect(mockMoveQuickNote).toHaveBeenCalledWith('test-token', 'note-2', 'UP'));
  });

  it('reorders via drag-and-drop by persisting the drop index', async () => {
    mockReorderQuickNote.mockResolvedValue(mockSelfAssignedNotes.content[0] as never);
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });

    const dt = {
      setData: jest.fn(),
      getData: () => 'note-1',
      effectAllowed: 'move',
      dropEffect: 'move',
    };
    const source = screen.getByTestId('my-note-card-note-1');
    const target = screen.getByTestId('my-note-card-note-2');
    fireEvent.dragStart(source, { dataTransfer: dt });
    fireEvent.dragOver(target, { dataTransfer: dt });
    fireEvent.drop(target, { dataTransfer: dt });

    // note-1 (index 0) dropped onto note-2 (index 1) -> reorder to index 1.
    await waitFor(() => expect(mockReorderQuickNote).toHaveBeenCalledWith('test-token', 'note-1', 1));
  });

  it('disables reordering when viewing the archived filter', async () => {
    render(<MyNotesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('filter-ARCHIVED')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('filter-ARCHIVED'));

    await waitFor(() => {
      expect(screen.getByTestId('my-note-card-note-1')).toBeInTheDocument();
    });
    // Archived view is not reorderable: no drag + no move arrows.
    expect(screen.getByTestId('my-note-card-note-1')).toHaveAttribute('draggable', 'false');
    expect(
      screen.getByTestId('my-note-card-note-1').querySelector('[data-testid="my-note-move-up-btn"]')
    ).not.toBeInTheDocument();
  });
});
