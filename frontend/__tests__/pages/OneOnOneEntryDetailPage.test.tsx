import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import OneOnOneEntryDetailPage from '@/app/people/[id]/one-on-ones/[entryId]/page';
import { Person } from '@/types/person';
import { OneOnOneEntry } from '@/types/one-on-one';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => ({ id: 'person-uuid-123', entryId: 'entry-uuid-456' }),
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  getPerson: jest.fn(),
  getOneOnOneEntry: jest.fn(),
  updateOneOnOneEntry: jest.fn(),
  deleteOneOnOneEntry: jest.fn(),
  listActionItemsByPerson: jest.fn(),
  createActionItem: jest.fn(),
  completeActionItem: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import {
  getPerson,
  getOneOnOneEntry,
  updateOneOnOneEntry,
  deleteOneOnOneEntry,
  listActionItemsByPerson,
} from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockGetPerson = getPerson as jest.MockedFunction<typeof getPerson>;
const mockGetOneOnOneEntry = getOneOnOneEntry as jest.MockedFunction<typeof getOneOnOneEntry>;
const mockUpdateOneOnOneEntry = updateOneOnOneEntry as jest.MockedFunction<typeof updateOneOnOneEntry>;
const mockDeleteOneOnOneEntry = deleteOneOnOneEntry as jest.MockedFunction<typeof deleteOneOnOneEntry>;
const mockListActionItemsByPerson = listActionItemsByPerson as jest.MockedFunction<typeof listActionItemsByPerson>;

const mockPerson: Person = {
  id: 'person-uuid-123',
  name: 'Carol Davis',
  preferredName: 'Carol',
  roleTitle: 'Designer',
  timezone: 'US/Pacific',
  startDate: '2024-02-01',
  email: 'carol@example.com',
  tags: ['design'],
  moraleStatus: 'YELLOW',
  moraleNote: 'Feeling stretched',
  pinnedRememberItems: [],
  atAGlance: { last1on1Date: '2025-05-05T10:00:00Z', openActionItemsCount: 1, activePdpGoalsSummary: null },
  createdAt: '2024-02-01T00:00:00Z',
  updatedAt: '2025-05-05T00:00:00Z',
};

const mockEntry: OneOnOneEntry = {
  id: 'entry-uuid-456',
  personId: 'person-uuid-123',
  meetingDate: '2025-05-05T10:00:00Z',
  agendaItems: [
    { id: 'ai-1', text: 'Design review', checked: true, displayOrder: 0, createdAt: '2025-05-05T10:00:00Z' },
    { id: 'ai-2', text: 'Sprint planning', checked: false, displayOrder: 1, createdAt: '2025-05-05T10:00:00Z' },
  ],
  notesMarkdown: '## Notes\nDiscussed design review feedback.',
  outcomesMarkdown: 'Will revise mockups by Friday.',
  sensitive: false,
  createdAt: '2025-05-05T10:00:00Z',
  updatedAt: '2025-05-05T10:00:00Z',
};

function setupAuthenticatedSession() {
  mockUseSession.mockReturnValue({
    data: { accessToken: 'test-token', user: {}, expires: '' },
    status: 'authenticated',
    update: jest.fn(),
  } as ReturnType<typeof useSession>);
}

describe('OneOnOneEntryDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupAuthenticatedSession();
    mockListActionItemsByPerson.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
  });

  it('should show loading state initially', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'loading', update: jest.fn() });
    render(<OneOnOneEntryDetailPage />);
    expect(screen.getByTestId('loading')).toBeInTheDocument();
  });

  it('should show unauthenticated message when not signed in', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'unauthenticated', update: jest.fn() });
    render(<OneOnOneEntryDetailPage />);
    expect(screen.getByTestId('unauthenticated')).toBeInTheDocument();
  });

  it('should render entry in edit mode with all fields', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneEntry.mockResolvedValue(mockEntry);

    render(<OneOnOneEntryDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('one-on-one-entry-form')).toBeInTheDocument();
    });

    // Header shows formatted date
    expect(screen.getByText(/May 5, 2025/)).toBeInTheDocument();
    expect(screen.getByText(/with Carol/)).toBeInTheDocument();

    // Form is in edit mode (shows "Update Entry" button)
    expect(screen.getByTestId('entry-form-submit')).toHaveTextContent('Update Entry');

    // Meeting date input is populated (value is in datetime-local format)
    const dateInput = screen.getByTestId('meeting-date-input') as HTMLInputElement;
    expect(dateInput.value).toContain('2025-05-05');

    // Notes editor has content
    const notesTextarea = screen.getByLabelText('Notes');
    expect(notesTextarea).toHaveValue('## Notes\nDiscussed design review feedback.');

    // Outcomes editor has content
    const outcomesTextarea = screen.getByLabelText('Outcomes');
    expect(outcomesTextarea).toHaveValue('Will revise mockups by Friday.');
  });

  it('should show delete button', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneEntry.mockResolvedValue(mockEntry);

    render(<OneOnOneEntryDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('delete-entry-button')).toBeInTheDocument();
    });

    expect(screen.getByTestId('delete-entry-button')).toHaveTextContent('Delete');
  });

  it('should show delete confirmation when delete button is clicked', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneEntry.mockResolvedValue(mockEntry);

    render(<OneOnOneEntryDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('delete-entry-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('delete-entry-button'));

    expect(screen.getByTestId('delete-entry-confirmation')).toBeInTheDocument();
    expect(screen.getByText(/Are you sure you want to delete this 1:1 entry/)).toBeInTheDocument();
    expect(screen.getByTestId('confirm-delete-entry-button')).toBeInTheDocument();
    expect(screen.getByTestId('cancel-delete-entry-button')).toBeInTheDocument();
  });

  it('should delete entry and navigate back on confirmation', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneEntry.mockResolvedValue(mockEntry);
    mockDeleteOneOnOneEntry.mockResolvedValue(undefined);

    render(<OneOnOneEntryDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('delete-entry-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('delete-entry-button'));
    fireEvent.click(screen.getByTestId('confirm-delete-entry-button'));

    await waitFor(() => {
      expect(mockDeleteOneOnOneEntry).toHaveBeenCalledWith('test-token', 'person-uuid-123', 'entry-uuid-456');
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/people/person-uuid-123');
    });
  });

  it('should cancel delete when cancel button is clicked', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneEntry.mockResolvedValue(mockEntry);

    render(<OneOnOneEntryDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('delete-entry-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('delete-entry-button'));
    expect(screen.getByTestId('delete-entry-confirmation')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('cancel-delete-entry-button'));
    expect(screen.queryByTestId('delete-entry-confirmation')).not.toBeInTheDocument();
  });

  it('should show error when entry fetch fails', async () => {
    mockGetPerson.mockRejectedValue(new Error('Entry not found'));
    mockGetOneOnOneEntry.mockRejectedValue(new Error('Entry not found'));

    render(<OneOnOneEntryDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toBeInTheDocument();
    });

    expect(screen.getByText('Entry not found')).toBeInTheDocument();
  });

  it('should submit update and refresh entry data', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneEntry.mockResolvedValue(mockEntry);
    const updatedEntry = { ...mockEntry, notesMarkdown: '## Updated Notes' };
    mockUpdateOneOnOneEntry.mockResolvedValue(updatedEntry);

    render(<OneOnOneEntryDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('one-on-one-entry-form')).toBeInTheDocument();
    });

    // Submit the form
    fireEvent.click(screen.getByTestId('entry-form-submit'));

    await waitFor(() => {
      expect(mockUpdateOneOnOneEntry).toHaveBeenCalledWith(
        'test-token',
        'person-uuid-123',
        'entry-uuid-456',
        expect.objectContaining({
          meetingDate: expect.any(String),
          sensitive: false,
        })
      );
    });
  });
});
