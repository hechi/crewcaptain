import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PersonDetailPage from '@/app/people/[id]/page';
import { Person } from '@/types/person';
import { OneOnOneEntry, OneOnOneSeries } from '@/types/one-on-one';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => ({ id: 'person-uuid-123' }),
  useSearchParams: () => ({
    get: () => null,
  }),
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  getPerson: jest.fn(),
  updatePerson: jest.fn(),
  deletePerson: jest.fn(),
  setMorale: jest.fn(),
  addRememberItem: jest.fn(),
  updateRememberItem: jest.fn(),
  removeRememberItem: jest.fn(),
  reorderRememberItems: jest.fn(),
  listOneOnOneEntries: jest.fn(),
  getOneOnOneSeries: jest.fn(),
  upsertOneOnOneSeries: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import {
  getPerson,
  listOneOnOneEntries,
  getOneOnOneSeries,
  upsertOneOnOneSeries,
} from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockGetPerson = getPerson as jest.MockedFunction<typeof getPerson>;
const mockListOneOnOneEntries = listOneOnOneEntries as jest.MockedFunction<typeof listOneOnOneEntries>;
const mockGetOneOnOneSeries = getOneOnOneSeries as jest.MockedFunction<typeof getOneOnOneSeries>;
const mockUpsertOneOnOneSeries = upsertOneOnOneSeries as jest.MockedFunction<typeof upsertOneOnOneSeries>;

const mockPerson: Person = {
  id: 'person-uuid-123',
  name: 'Alice Johnson',
  preferredName: 'Alice',
  roleTitle: 'Staff Engineer',
  timezone: 'America/New_York',
  startDate: '2023-01-10',
  email: 'alice@example.com',
  tags: ['engineering'],
  moraleStatus: 'GREEN',
  moraleNote: null,
  pinnedRememberItems: [],
  atAGlance: { last1on1Date: '2025-05-01T14:00:00Z', openActionItemsCount: 2, activePdpGoalsSummary: null },
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-05-01T00:00:00Z',
};

const mockEntry: OneOnOneEntry = {
  id: 'entry-uuid-1',
  personId: 'person-uuid-123',
  meetingDate: '2025-05-01T14:00:00Z',
  agendaItems: [
    { id: 'ai-1', text: 'Review Q2 goals', checked: true, displayOrder: 0, createdAt: '2025-05-01T14:00:00Z' },
    { id: 'ai-2', text: 'Discuss project timeline', checked: false, displayOrder: 1, createdAt: '2025-05-01T14:00:00Z' },
  ],
  notesMarkdown: '## Discussion\nTalked about Q2 goals and project timeline.',
  outcomesMarkdown: 'Agreed to extend deadline.',
  sensitive: false,
  createdAt: '2025-05-01T14:00:00Z',
  updatedAt: '2025-05-01T14:00:00Z',
};

const mockSeries: OneOnOneSeries = {
  id: 'series-uuid-1',
  personId: 'person-uuid-123',
  cadenceType: 'BIWEEKLY',
  customIntervalDays: null,
  templateMarkdown: '## Agenda\n- [ ] Check-in\n\n## Notes\n',
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
};

function setupAuthenticatedSession() {
  mockUseSession.mockReturnValue({
    data: { accessToken: 'test-token', user: {}, expires: '' },
    status: 'authenticated',
    update: jest.fn(),
  } as ReturnType<typeof useSession>);
}

describe('PersonDetailPage — 1:1s Tab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupAuthenticatedSession();
    mockGetPerson.mockResolvedValue(mockPerson);
  });

  it('should render the 1:1s tab button', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-one-on-ones')).toBeInTheDocument();
    });

    expect(screen.getByTestId('tab-one-on-ones')).toHaveTextContent('1:1s');
  });

  it('should show timeline with entries when 1:1s tab is clicked', async () => {
    mockListOneOnOneEntries.mockResolvedValue({
      content: [mockEntry],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    mockGetOneOnOneSeries.mockResolvedValue(mockSeries);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-one-on-ones')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-one-on-ones'));

    await waitFor(() => {
      expect(screen.getByTestId('one-on-one-timeline')).toBeInTheDocument();
    });
  });

  it('should show empty state when no entries exist', async () => {
    mockListOneOnOneEntries.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
    mockGetOneOnOneSeries.mockRejectedValue(new Error('Not found'));

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-one-on-ones')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-one-on-ones'));

    await waitFor(() => {
      expect(screen.getByTestId('one-on-one-timeline-empty')).toBeInTheDocument();
    });

    expect(screen.getByTestId('start-one-on-one-button')).toBeInTheDocument();
    expect(screen.getByText(/No 1:1s yet/)).toBeInTheDocument();
  });

  it('should show "Start 1:1" navigation button in toolbar', async () => {
    mockListOneOnOneEntries.mockResolvedValue({
      content: [mockEntry],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    mockGetOneOnOneSeries.mockResolvedValue(mockSeries);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-one-on-ones')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-one-on-ones'));

    await waitFor(() => {
      expect(screen.getByTestId('start-one-on-one-nav-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('start-one-on-one-nav-button'));

    expect(mockPush).toHaveBeenCalledWith('/people/person-uuid-123/one-on-ones/new');
  });

  it('should show series config panel when gear icon is clicked', async () => {
    mockListOneOnOneEntries.mockResolvedValue({
      content: [mockEntry],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    mockGetOneOnOneSeries.mockResolvedValue(mockSeries);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-one-on-ones')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-one-on-ones'));

    await waitFor(() => {
      expect(screen.getByTestId('series-config-toggle')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('series-config-toggle'));

    await waitFor(() => {
      expect(screen.getByTestId('series-config-panel')).toBeInTheDocument();
    });
  });

  it('should save series configuration via SeriesConfigPanel', async () => {
    mockListOneOnOneEntries.mockResolvedValue({
      content: [mockEntry],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    mockGetOneOnOneSeries.mockResolvedValue(mockSeries);
    mockUpsertOneOnOneSeries.mockResolvedValue({
      ...mockSeries,
      cadenceType: 'WEEKLY',
    });

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-one-on-ones')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-one-on-ones'));

    await waitFor(() => {
      expect(screen.getByTestId('series-config-toggle')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('series-config-toggle'));

    await waitFor(() => {
      expect(screen.getByTestId('series-config-panel')).toBeInTheDocument();
    });

    // Change cadence to Weekly
    fireEvent.click(screen.getByTestId('cadence-option-weekly'));

    // Save
    fireEvent.click(screen.getByTestId('series-config-save'));

    await waitFor(() => {
      expect(mockUpsertOneOnOneSeries).toHaveBeenCalledWith(
        'test-token',
        'person-uuid-123',
        expect.objectContaining({ cadenceType: 'WEEKLY' })
      );
    });
  });
});
