import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import CreateOneOnOneEntryPage from '@/app/people/[id]/one-on-ones/new/page';
import { Person } from '@/types/person';
import { OneOnOneSeries, OneOnOneEntry } from '@/types/one-on-one';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => ({ id: 'person-uuid-123' }),
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  getPerson: jest.fn(),
  getOneOnOneSeries: jest.fn(),
  createOneOnOneEntry: jest.fn(),
  listActionItemsByPerson: jest.fn(),
  createActionItem: jest.fn(),
  completeActionItem: jest.fn(),
  getUserSettings: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { getPerson, getOneOnOneSeries, createOneOnOneEntry, listActionItemsByPerson, getUserSettings } from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockGetPerson = getPerson as jest.MockedFunction<typeof getPerson>;
const mockGetOneOnOneSeries = getOneOnOneSeries as jest.MockedFunction<typeof getOneOnOneSeries>;
const mockCreateOneOnOneEntry = createOneOnOneEntry as jest.MockedFunction<typeof createOneOnOneEntry>;
const mockListActionItemsByPerson = listActionItemsByPerson as jest.MockedFunction<typeof listActionItemsByPerson>;
const mockGetUserSettings = getUserSettings as jest.MockedFunction<typeof getUserSettings>;

const mockPerson: Person = {
  id: 'person-uuid-123',
  name: 'Bob Williams',
  preferredName: 'Bob',
  roleTitle: 'Product Manager',
  timezone: 'Europe/London',
  startDate: '2024-06-01',
  email: 'bob@example.com',
  tags: ['product'],
  moraleStatus: 'GREEN',
  moraleNote: null,
  pinnedRememberItems: [],
  atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
  createdAt: '2024-06-01T00:00:00Z',
  updatedAt: '2024-06-01T00:00:00Z',
};

const mockSeries: OneOnOneSeries = {
  id: 'series-uuid-1',
  personId: 'person-uuid-123',
  cadenceType: 'WEEKLY',
  customIntervalDays: null,
  templateMarkdown: '## Agenda\n- [ ] Check-in\n\n## Notes\n',
  createdAt: '2024-06-01T00:00:00Z',
  updatedAt: '2024-06-01T00:00:00Z',
};

const mockCreatedEntry: OneOnOneEntry = {
  id: 'new-entry-uuid',
  personId: 'person-uuid-123',
  meetingDate: '2025-05-08T14:00:00Z',
  agendaItems: [],
  notesMarkdown: '## Agenda\n- [ ] Check-in\n\n## Notes\n',
  outcomesMarkdown: null,
  sensitive: false,
  createdAt: '2025-05-08T14:00:00Z',
  updatedAt: '2025-05-08T14:00:00Z',
};

function setupAuthenticatedSession() {
  mockUseSession.mockReturnValue({
    data: { accessToken: 'test-token', user: {}, expires: '' },
    status: 'authenticated',
    update: jest.fn(),
  } as ReturnType<typeof useSession>);
}

describe('CreateOneOnOneEntryPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupAuthenticatedSession();
    mockListActionItemsByPerson.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    mockGetUserSettings.mockResolvedValue({
      dueSoonDays: 3,
      staleOneOnOneDays: 14,
      anniversaryLookaheadDays: 30,
      theme: 'DARK',
      showAchievements: true,
      notifyActionItemOverdue: true,
      notifyActionItemDueSoon: true,
      notifyStaleOneOnOne: true,
      notifyUpcomingAnniversary: true,
      aiEnabled: false,
      aiApiBaseUrl: null,
      aiModelName: null,
      aiPrivacyMode: true,
      aiWritingStyle: 'NARRATIVE',
      kudosRefinementPrompt: null,
      pdpOptimizationPrompt: null,
      agendaPrepPrompt: null,
      narrativePrompt: null,
    });
  });

  it('should show loading state initially', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'loading', update: jest.fn() });
    render(<CreateOneOnOneEntryPage />);
    expect(screen.getByTestId('loading-screen')).toBeInTheDocument();
  });

  it('should show unauthenticated message when not signed in', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'unauthenticated', update: jest.fn() });
    render(<CreateOneOnOneEntryPage />);
    expect(screen.getByTestId('unauthenticated')).toBeInTheDocument();
  });

  it('should render the entry form when loaded', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneSeries.mockRejectedValue(new Error('Not found'));

    render(<CreateOneOnOneEntryPage />);

    await waitFor(() => {
      expect(screen.getByTestId('one-on-one-entry-form')).toBeInTheDocument();
    });

    expect(screen.getByText(/New 1:1 with Bob/)).toBeInTheDocument();
    expect(screen.getByTestId('meeting-date-input')).toBeInTheDocument();
    expect(screen.getByTestId('entry-form-submit')).toBeInTheDocument();
  });

  it('should prefill notes from series template', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneSeries.mockResolvedValue(mockSeries);

    render(<CreateOneOnOneEntryPage />);

    await waitFor(() => {
      expect(screen.getByTestId('one-on-one-entry-form')).toBeInTheDocument();
    });

    // The MarkdownEditor should contain the template text
    const notesTextarea = screen.getByLabelText('Notes');
    expect(notesTextarea).toHaveValue('## Agenda\n- [ ] Check-in\n\n## Notes\n');
  });

  it('should submit form and navigate to entry detail on success', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneSeries.mockRejectedValue(new Error('Not found'));
    mockCreateOneOnOneEntry.mockResolvedValue(mockCreatedEntry);

    render(<CreateOneOnOneEntryPage />);

    await waitFor(() => {
      expect(screen.getByTestId('one-on-one-entry-form')).toBeInTheDocument();
    });

    // Submit the form (meeting date is pre-filled with current date)
    fireEvent.click(screen.getByTestId('entry-form-submit'));

    await waitFor(() => {
      expect(mockCreateOneOnOneEntry).toHaveBeenCalledWith(
        'test-token',
        'person-uuid-123',
        expect.objectContaining({
          meetingDate: expect.any(String),
          sensitive: false,
        })
      );
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/people/person-uuid-123/one-on-ones/new-entry-uuid');
    });
  });

  it('should show error message on submission failure', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneSeries.mockRejectedValue(new Error('Not found'));
    mockCreateOneOnOneEntry.mockRejectedValue(new Error('Server error'));

    render(<CreateOneOnOneEntryPage />);

    await waitFor(() => {
      expect(screen.getByTestId('one-on-one-entry-form')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('entry-form-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toBeInTheDocument();
    });

    expect(screen.getByText('Server error')).toBeInTheDocument();
  });

  it('should navigate back when cancel is clicked', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneSeries.mockRejectedValue(new Error('Not found'));

    render(<CreateOneOnOneEntryPage />);

    await waitFor(() => {
      expect(screen.getByTestId('entry-form-cancel')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('entry-form-cancel'));

    expect(mockPush).toHaveBeenCalledWith('/people/person-uuid-123');
  });

  it('should show inline action items section on create page', async () => {
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetOneOnOneSeries.mockRejectedValue(new Error('Not found'));

    render(<CreateOneOnOneEntryPage />);

    await waitFor(() => {
      expect(screen.getByTestId('one-on-one-action-items')).toBeInTheDocument();
    });

    // Should show the quick-add form
    expect(screen.getByTestId('quick-add-action-item-form')).toBeInTheDocument();
    expect(screen.getByTestId('quick-action-title-input')).toBeInTheDocument();
  });
});
