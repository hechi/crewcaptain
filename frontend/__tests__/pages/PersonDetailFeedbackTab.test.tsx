import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PersonDetailPage from '@/app/people/[id]/page';
import { Person } from '@/types/person';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => ({ id: '123e4567-e89b-12d3-a456-426614174000' }),
  useSearchParams: () => ({ get: () => null }),
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/useStableToken', () => {
  const getToken = () => 'test-token';
  const value = { getToken, isAuthenticated: true, status: 'authenticated' };
  return { useStableToken: jest.fn(() => value) };
});

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
  listKudosByPerson: jest.fn(),
  createKudos: jest.fn(),
  deleteKudos: jest.fn(),
  exportPersonMarkdown: jest.fn(),
  generateReviewPacket: jest.fn(),
  getUserSettings: jest.fn(),
  generateAiNarrative: jest.fn(),
  // Feedback feature
  listFeedbackResponses: jest.fn(),
  updateFeedbackResponse: jest.fn(),
  deleteFeedbackResponse: jest.fn(),
  bulkUpdateFeedbackResponses: jest.fn(),
  convertFeedbackResponse: jest.fn(),
  listFeedbackLinks: jest.fn(),
  createFeedbackLink: jest.fn(),
  revokeFeedbackLink: jest.fn(),
  extendFeedbackLink: jest.fn(),
  getFeedbackAnalytics: jest.fn(),
  listFeedbackSummaries: jest.fn(),
  generateFeedbackSummary: jest.fn(),
  saveFeedbackSummary: jest.fn(),
  listFeedbackTemplates: jest.fn(),
  getFeedbackStarterTemplates: jest.fn(),
  generateFeedbackTemplate: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import {
  getPerson,
  getUserSettings,
  listFeedbackResponses,
  listFeedbackTemplates,
  getFeedbackStarterTemplates,
  listFeedbackLinks,
} from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockGetPerson = getPerson as jest.MockedFunction<typeof getPerson>;
const mockGetUserSettings = getUserSettings as jest.MockedFunction<typeof getUserSettings>;
const mockListResponses = listFeedbackResponses as jest.MockedFunction<typeof listFeedbackResponses>;
const mockListTemplates = listFeedbackTemplates as jest.MockedFunction<typeof listFeedbackTemplates>;
const mockStarters = getFeedbackStarterTemplates as jest.MockedFunction<typeof getFeedbackStarterTemplates>;

const mockPerson: Person = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  name: 'Jane Smith',
  preferredName: null,
  roleTitle: 'Senior Engineer',
  timezone: null,
  startDate: null,
  email: null,
  tags: [],
  moraleStatus: 'GREEN',
  moraleNote: null,
  pinnedRememberItems: [],
  atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-08T12:00:00Z',
};

describe('PersonDetailPage - Feedback Tab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);
    mockGetUserSettings.mockResolvedValue({ aiAvailable: false } as never);
    mockListResponses.mockResolvedValue([]);
    mockListTemplates.mockResolvedValue([]);
    mockStarters.mockResolvedValue([]);
    (listFeedbackLinks as jest.Mock).mockResolvedValue([]);
  });

  it('renders the Feedback tab button', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-feedback')).toBeInTheDocument();
    });
    expect(screen.getByTestId('tab-feedback')).toHaveTextContent('Feedback');
  });

  it('switches to the Feedback tab and renders the feedback panel', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-feedback')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-feedback'));

    await waitFor(() => {
      expect(screen.getByTestId('feedback-tab-panel')).toBeInTheDocument();
    });
    // Responses sub-panel is the default sub-tab and fetches responses
    await waitFor(() => {
      expect(screen.getByTestId('feedback-responses-panel')).toBeInTheDocument();
    });
    expect(mockListResponses).toHaveBeenCalledWith(
      'test-token',
      '123e4567-e89b-12d3-a456-426614174000',
      expect.any(Object)
    );
  });

  it('keeps other tabs working (details tab still renders)', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-feedback'));
    await waitFor(() => {
      expect(screen.getByTestId('feedback-tab-panel')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-details'));
    await waitFor(() => {
      expect(screen.getByTestId('edit-button')).toBeInTheDocument();
    });
  });

  it('does NOT show a create feedback link button in the top actions toolbar', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('person-actions-toolbar')).toBeInTheDocument();
    });
    // The create action lives inside the Feedback section, not the top toolbar.
    expect(screen.queryByTestId('create-feedback-link-button')).not.toBeInTheDocument();
  });

  it('opens the Create feedback link modal from the Feedback section header', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('tab-feedback')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('tab-feedback'));

    const createBtn = await screen.findByTestId('feedback-create-link-button');
    expect(createBtn).toHaveTextContent('Create feedback link');
    fireEvent.click(createBtn);

    await waitFor(() => {
      expect(screen.getByTestId('create-feedback-link-modal')).toBeInTheDocument();
    });
  });

  it('the Feedback create button is available regardless of active sub-tab', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => expect(screen.getByTestId('tab-feedback')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('tab-feedback'));

    // Switch to the Links sub-tab; the header create button remains visible.
    await waitFor(() => expect(screen.getByTestId('feedback-subtab-links')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('feedback-subtab-links'));
    expect(screen.getByTestId('feedback-create-link-button')).toBeInTheDocument();
  });
});
