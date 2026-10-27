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
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockListQuickNotes = listQuickNotes as jest.MockedFunction<typeof listQuickNotes>;
const mockCreateQuickNote = createQuickNote as jest.MockedFunction<typeof createQuickNote>;
const mockArchiveQuickNote = archiveQuickNote as jest.MockedFunction<typeof archiveQuickNote>;
const mockDeleteQuickNote = deleteQuickNote as jest.MockedFunction<typeof deleteQuickNote>;
const mockConvertQuickNote = convertQuickNote as jest.MockedFunction<typeof convertQuickNote>;
const mockUpdateQuickNote = updateQuickNote as jest.MockedFunction<typeof updateQuickNote>;
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
});
