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
}));

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { listQuickNotes, createQuickNote, archiveQuickNote, deleteQuickNote } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockListQuickNotes = listQuickNotes as jest.MockedFunction<typeof listQuickNotes>;
const mockCreateQuickNote = createQuickNote as jest.MockedFunction<typeof createQuickNote>;
const mockArchiveQuickNote = archiveQuickNote as jest.MockedFunction<typeof archiveQuickNote>;
const mockDeleteQuickNote = deleteQuickNote as jest.MockedFunction<typeof deleteQuickNote>;
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
        page: 0,
        size: 20,
      });
    });
  });
});
