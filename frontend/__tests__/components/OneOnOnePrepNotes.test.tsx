import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import OneOnOnePrepNotes from '@/components/one-on-one/OneOnOnePrepNotes';
import { QuickNote } from '@/types/quick-note';

jest.mock('@/lib/api-client', () => ({
  listQuickNotes: jest.fn(),
  attachQuickNote: jest.fn(),
  archiveQuickNote: jest.fn(),
}));

import {
  listQuickNotes,
  attachQuickNote,
  archiveQuickNote,
} from '@/lib/api-client';

const mockListQuickNotes = listQuickNotes as jest.MockedFunction<typeof listQuickNotes>;
const mockAttachQuickNote = attachQuickNote as jest.MockedFunction<typeof attachQuickNote>;
const mockArchiveQuickNote = archiveQuickNote as jest.MockedFunction<typeof archiveQuickNote>;

const prepNote1: QuickNote = {
  id: 'note-1',
  personId: 'person-1',
  text: 'Discuss promotion timeline',
  sensitive: false,
  selfAssigned: false,
  status: 'INBOX',
  attachedEntryId: null,
  createdAt: '2026-06-25T09:00:00Z',
  updatedAt: '2026-06-25T09:00:00Z',
};

const prepNote2: QuickNote = {
  id: 'note-2',
  personId: 'person-1',
  text: 'Follow up on conference talk proposal',
  sensitive: false,
  selfAssigned: false,
  status: 'INBOX',
  attachedEntryId: null,
  createdAt: '2026-06-26T14:00:00Z',
  updatedAt: '2026-06-26T14:00:00Z',
};

const sensitiveNote: QuickNote = {
  id: 'note-3',
  personId: 'person-1',
  text: 'Discuss salary review',
  sensitive: true,
  selfAssigned: false,
  status: 'INBOX',
  attachedEntryId: null,
  createdAt: '2026-06-27T10:00:00Z',
  updatedAt: '2026-06-27T10:00:00Z',
};

const defaultProps = {
  token: 'test-token',
  personId: 'person-1',
  entryId: 'entry-1',
};

describe('OneOnOnePrepNotes', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state initially', () => {
    mockListQuickNotes.mockReturnValue(new Promise(() => {}));
    render(<OneOnOnePrepNotes {...defaultProps} />);
    expect(screen.getByTestId('prep-notes-loading')).toBeInTheDocument();
  });

  it('should not render anything when there are no prep notes', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [],
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0,
    });
    const { container } = render(<OneOnOnePrepNotes {...defaultProps} />);
    await waitFor(() => {
      expect(screen.queryByTestId('prep-notes-loading')).not.toBeInTheDocument();
    });
    // Should render nothing when empty
    expect(container.querySelector('[data-testid="prep-notes-panel"]')).not.toBeInTheDocument();
  });

  it('should render prep notes when they exist', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [prepNote1, prepNote2],
      page: 0,
      size: 50,
      totalElements: 2,
      totalPages: 1,
    });
    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('prep-notes-panel')).toBeInTheDocument();
    });
    expect(screen.getByText('Discuss promotion timeline')).toBeInTheDocument();
    expect(screen.getByText('Follow up on conference talk proposal')).toBeInTheDocument();
  });

  it('should display the count of prep notes in the header', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [prepNote1, prepNote2],
      page: 0,
      size: 50,
      totalElements: 2,
      totalPages: 1,
    });
    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('prep-notes-count')).toHaveTextContent('2');
    });
  });

  it('should call listQuickNotes with correct params (personId + INBOX status)', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [],
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0,
    });
    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(mockListQuickNotes).toHaveBeenCalledWith('test-token', {
        personId: 'person-1',
        status: 'INBOX',
        size: 50,
      });
    });
  });

  it('should attach note to entry when "Add to Agenda" is clicked', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [prepNote1],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });
    mockAttachQuickNote.mockResolvedValue({ ...prepNote1, status: 'ATTACHED', attachedEntryId: 'entry-1' });

    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Discuss promotion timeline')).toBeInTheDocument();
    });

    const addButton = screen.getByTestId('prep-note-add-note-1');
    fireEvent.click(addButton);

    await waitFor(() => {
      expect(mockAttachQuickNote).toHaveBeenCalledWith('test-token', 'note-1', { entryId: 'entry-1' });
    });
  });

  it('should archive a note when dismiss is clicked', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [prepNote1],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });
    mockArchiveQuickNote.mockResolvedValue({ ...prepNote1, status: 'ARCHIVED' });

    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Discuss promotion timeline')).toBeInTheDocument();
    });

    const dismissButton = screen.getByTestId('prep-note-dismiss-note-1');
    fireEvent.click(dismissButton);

    await waitFor(() => {
      expect(mockArchiveQuickNote).toHaveBeenCalledWith('test-token', 'note-1');
    });
  });

  it('should remove note from list after successful attach', async () => {
    mockListQuickNotes
      .mockResolvedValueOnce({
        content: [prepNote1, prepNote2],
        page: 0,
        size: 50,
        totalElements: 2,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        content: [prepNote2],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1,
      });
    mockAttachQuickNote.mockResolvedValue({ ...prepNote1, status: 'ATTACHED', attachedEntryId: 'entry-1' });

    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Discuss promotion timeline')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('prep-note-add-note-1'));

    await waitFor(() => {
      expect(screen.queryByText('Discuss promotion timeline')).not.toBeInTheDocument();
    });
  });

  it('should remove note from list after successful dismiss', async () => {
    mockListQuickNotes
      .mockResolvedValueOnce({
        content: [prepNote1, prepNote2],
        page: 0,
        size: 50,
        totalElements: 2,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        content: [prepNote2],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1,
      });
    mockArchiveQuickNote.mockResolvedValue({ ...prepNote1, status: 'ARCHIVED' });

    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Discuss promotion timeline')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('prep-note-dismiss-note-1'));

    await waitFor(() => {
      expect(screen.queryByText('Discuss promotion timeline')).not.toBeInTheDocument();
    });
  });

  it('should show sensitive badge for sensitive notes', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [sensitiveNote],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });
    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('prep-note-sensitive-note-3')).toBeInTheDocument();
    });
  });

  it('should show error state when fetch fails', async () => {
    mockListQuickNotes.mockRejectedValue(new Error('Network error'));
    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('prep-notes-error')).toBeInTheDocument();
    });
  });

  it('should not render Add to Agenda button when entryId is not provided', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [prepNote1],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });
    render(<OneOnOnePrepNotes token="test-token" personId="person-1" />);

    await waitFor(() => {
      expect(screen.getByText('Discuss promotion timeline')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('prep-note-add-note-1')).not.toBeInTheDocument();
  });

  it('should show relative date for notes', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [prepNote1],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });
    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('prep-note-date-note-1')).toBeInTheDocument();
    });
  });

  it('should be collapsible', async () => {
    mockListQuickNotes.mockResolvedValue({
      content: [prepNote1, prepNote2],
      page: 0,
      size: 50,
      totalElements: 2,
      totalPages: 1,
    });
    render(<OneOnOnePrepNotes {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Discuss promotion timeline')).toBeInTheDocument();
    });

    // Click the header to collapse
    const toggle = screen.getByTestId('prep-notes-toggle');
    fireEvent.click(toggle);

    // Notes should be hidden
    expect(screen.queryByText('Discuss promotion timeline')).not.toBeVisible();
  });
});
