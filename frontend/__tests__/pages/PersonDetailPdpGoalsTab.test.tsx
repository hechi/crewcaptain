import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PersonDetailPage from '@/app/people/[id]/page';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => ({ id: '123e4567-e89b-12d3-a456-426614174000' }),
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
  removeRememberItem: jest.fn(),
  reorderRememberItems: jest.fn(),
  listOneOnOneEntries: jest.fn(),
  getOneOnOneSeries: jest.fn(),
  upsertOneOnOneSeries: jest.fn(),
  listActionItemsByPerson: jest.fn(),
  createActionItem: jest.fn(),
  completeActionItem: jest.fn(),
  cancelActionItem: jest.fn(),
  deleteActionItem: jest.fn(),
  updateActionItem: jest.fn(),
  listPdpGoalsByPerson: jest.fn(),
  createPdpGoal: jest.fn(),
  updatePdpGoal: jest.fn(),
  achievePdpGoal: jest.fn(),
  pausePdpGoal: jest.fn(),
  dropPdpGoal: jest.fn(),
  resumePdpGoal: jest.fn(),
  deletePdpGoal: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { getPerson, listPdpGoalsByPerson, createPdpGoal, achievePdpGoal, deletePdpGoal } from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockGetPerson = getPerson as jest.MockedFunction<typeof getPerson>;
const mockListPdpGoals = listPdpGoalsByPerson as jest.MockedFunction<typeof listPdpGoalsByPerson>;
const mockCreatePdpGoal = createPdpGoal as jest.MockedFunction<typeof createPdpGoal>;
const mockAchievePdpGoal = achievePdpGoal as jest.MockedFunction<typeof achievePdpGoal>;
const mockDeletePdpGoal = deletePdpGoal as jest.MockedFunction<typeof deletePdpGoal>;

const mockPerson = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  name: 'Jane Smith',
  preferredName: null,
  roleTitle: 'Senior Engineer',
  timezone: null,
  startDate: null,
  email: null,
  tags: [],
  moraleStatus: 'GREEN' as const,
  moraleNote: null,
  pinnedRememberItems: [],
  atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-08T12:00:00Z',
};

const mockPdpGoals = {
  content: [
    {
      id: 'goal-1',
      personId: '123e4567-e89b-12d3-a456-426614174000',
      title: 'Improve public speaking',
      description: 'Practice presentations monthly',
      targetDate: '2026-12-31',
      status: 'ACTIVE' as const,
      createdAt: '2026-05-10T10:00:00Z',
      updatedAt: '2026-05-10T10:00:00Z',
    },
    {
      id: 'goal-2',
      personId: '123e4567-e89b-12d3-a456-426614174000',
      title: 'Learn Kotlin',
      description: null,
      targetDate: null,
      status: 'PAUSED' as const,
      createdAt: '2026-05-09T10:00:00Z',
      updatedAt: '2026-05-09T10:00:00Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
};

describe('PersonDetailPage - PDP Goals Tab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);
    mockListPdpGoals.mockResolvedValue(mockPdpGoals);
  });

  it('should render PDP Goals tab button', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });
    expect(screen.getByTestId('tab-pdp-goals')).toHaveTextContent('PDP Goals');
  });

  it('should show PDP goals when tab is clicked', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-pdp-goals'));

    await waitFor(() => {
      expect(screen.getByTestId('pdp-goal-list')).toBeInTheDocument();
    });
  });

  it('should fetch PDP goals when tab is activated', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-pdp-goals'));

    await waitFor(() => {
      expect(mockListPdpGoals).toHaveBeenCalledWith('test-token', '123e4567-e89b-12d3-a456-426614174000', {
        status: undefined,
      });
    });
  });

  it('should display PDP goal cards after loading', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-pdp-goals'));

    await waitFor(() => {
      expect(screen.getAllByTestId('pdp-goal-card')).toHaveLength(2);
    });

    expect(screen.getByText('Improve public speaking')).toBeInTheDocument();
    expect(screen.getByText('Learn Kotlin')).toBeInTheDocument();
  });

  it('should show empty state when no goals exist', async () => {
    mockListPdpGoals.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-pdp-goals'));

    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
  });

  it('should create a PDP goal via the form', async () => {
    mockCreatePdpGoal.mockResolvedValue({
      id: 'new-goal',
      personId: '123e4567-e89b-12d3-a456-426614174000',
      title: 'New Goal',
      description: null,
      targetDate: null,
      status: 'ACTIVE',
      createdAt: '2026-05-10T14:00:00Z',
      updatedAt: '2026-05-10T14:00:00Z',
    });

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-pdp-goals'));

    await waitFor(() => {
      expect(screen.getByTestId('pdp-goal-create-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('pdp-goal-create-btn'));
    fireEvent.change(screen.getByTestId('pdp-goal-title-input'), { target: { value: 'New Goal' } });
    fireEvent.submit(screen.getByTestId('pdp-goal-form'));

    await waitFor(() => {
      expect(mockCreatePdpGoal).toHaveBeenCalledWith('test-token', '123e4567-e89b-12d3-a456-426614174000', {
        title: 'New Goal',
        description: null,
        targetDate: null,
      });
    });

    // Should re-fetch person to update at-a-glance counters
    await waitFor(() => {
      expect(mockGetPerson).toHaveBeenCalledTimes(2); // initial + after create
    });
  });

  it('should achieve a PDP goal', async () => {
    mockAchievePdpGoal.mockResolvedValue({ ...mockPdpGoals.content[0], status: 'ACHIEVED' });

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-pdp-goals'));

    await waitFor(() => {
      expect(screen.getAllByTestId('pdp-goal-achieve-btn')).toHaveLength(1);
    });

    mockGetPerson.mockClear();
    fireEvent.click(screen.getByTestId('pdp-goal-achieve-btn'));

    await waitFor(() => {
      expect(mockAchievePdpGoal).toHaveBeenCalledWith('test-token', '123e4567-e89b-12d3-a456-426614174000', 'goal-1');
    });

    // Should re-fetch person to update at-a-glance counters
    await waitFor(() => {
      expect(mockGetPerson).toHaveBeenCalled();
    });
  });

  it('should delete a PDP goal', async () => {
    mockDeletePdpGoal.mockResolvedValue(undefined);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-pdp-goals'));

    await waitFor(() => {
      expect(screen.getAllByTestId('pdp-goal-delete-btn')).toHaveLength(2);
    });

    fireEvent.click(screen.getAllByTestId('pdp-goal-delete-btn')[0]);

    await waitFor(() => {
      expect(mockDeletePdpGoal).toHaveBeenCalledWith('test-token', '123e4567-e89b-12d3-a456-426614174000', 'goal-1');
    });
  });

  it('should filter goals by status', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-pdp-goals'));

    await waitFor(() => {
      expect(screen.getByTestId('pdp-goal-filter-active')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('pdp-goal-filter-active'));

    await waitFor(() => {
      expect(mockListPdpGoals).toHaveBeenCalledWith('test-token', '123e4567-e89b-12d3-a456-426614174000', {
        status: 'ACTIVE',
      });
    });
  });

  it('should show loading state while fetching goals', async () => {
    // Make the API call hang
    mockListPdpGoals.mockImplementation(() => new Promise(() => {}));

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-pdp-goals')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-pdp-goals'));

    await waitFor(() => {
      expect(screen.getByTestId('pdp-goals-loading')).toBeInTheDocument();
    });
  });
});
