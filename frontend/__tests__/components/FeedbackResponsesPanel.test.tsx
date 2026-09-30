import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';

jest.mock('@/lib/useStableToken', () => {
  const getToken = () => 'test-token';
  const value = { getToken, isAuthenticated: true, status: 'authenticated' };
  return { useStableToken: jest.fn(() => value) };
});

jest.mock('@/lib/api-client', () => ({
  listFeedbackResponses: jest.fn(),
  updateFeedbackResponse: jest.fn(),
  deleteFeedbackResponse: jest.fn(),
  bulkUpdateFeedbackResponses: jest.fn(),
  convertFeedbackResponse: jest.fn(),
}));

import {
  listFeedbackResponses,
  updateFeedbackResponse,
  deleteFeedbackResponse,
  bulkUpdateFeedbackResponses,
  convertFeedbackResponse,
} from '@/lib/api-client';
import FeedbackResponsesPanel from '@/components/feedback/FeedbackResponsesPanel';

const mockList = listFeedbackResponses as jest.MockedFunction<typeof listFeedbackResponses>;
const mockUpdate = updateFeedbackResponse as jest.MockedFunction<typeof updateFeedbackResponse>;
const mockDelete = deleteFeedbackResponse as jest.MockedFunction<typeof deleteFeedbackResponse>;
const mockBulk = bulkUpdateFeedbackResponses as jest.MockedFunction<typeof bulkUpdateFeedbackResponses>;
const mockConvert = convertFeedbackResponse as jest.MockedFunction<typeof convertFeedbackResponse>;

const PERSON_ID = 'person-1';

const response1 = {
  id: 'resp-1',
  personId: PERSON_ID,
  linkId: 'link-1',
  submitterName: 'Alex Doe',
  submitterEmail: null,
  anonymous: false,
  answers: [
    { questionId: 'q1', ratingValue: 4, textValue: null },
    { questionId: 'q2', ratingValue: null, textValue: 'Great collaborator' },
  ],
  additionalComments: 'Keep it up',
  status: 'PENDING' as const,
  flagged: false,
  pinned: false,
  convertedToType: null,
  convertedToId: null,
  createdAt: '2026-01-10T10:00:00Z',
};

const response2 = {
  ...response1,
  id: 'resp-2',
  submitterName: null,
  anonymous: true,
  status: 'APPROVED' as const,
  additionalComments: null,
  answers: [{ questionId: 'q1', ratingValue: 5, textValue: null }],
};

describe('FeedbackResponsesPanel', () => {
  let confirmSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    mockList.mockResolvedValue([response1, response2]);
    mockUpdate.mockResolvedValue({ ...response1, status: 'APPROVED' });
    mockDelete.mockResolvedValue(undefined);
    mockBulk.mockResolvedValue(undefined);
    mockConvert.mockResolvedValue({ ...response1, convertedToType: 'KUDO' });
    confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    confirmSpy.mockRestore();
  });

  it('renders responses with submitter, rating, and anonymous', async () => {
    render(<FeedbackResponsesPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-response-resp-1')).toBeInTheDocument();
    });
    expect(screen.getByText('Alex Doe')).toBeInTheDocument();
    expect(screen.getByText('Anonymous')).toBeInTheDocument();
    // rating rendered as N/5
    expect(screen.getByText('4/5')).toBeInTheDocument();
    // pending count
    expect(screen.getByTestId('feedback-responses-pending-count')).toHaveTextContent('1 new');
  });

  it('approve calls updateFeedbackResponse with approve:true', async () => {
    render(<FeedbackResponsesPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-response-resp-1-approve')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-response-resp-1-approve'));

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledWith('test-token', 'resp-1', { approve: true });
    });
    // refetch after mutation
    await waitFor(() => {
      expect(mockList).toHaveBeenCalledTimes(2);
    });
  });

  it('delete calls deleteFeedbackResponse', async () => {
    render(<FeedbackResponsesPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-response-resp-1-delete')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-response-resp-1-delete'));

    await waitFor(() => {
      expect(mockDelete).toHaveBeenCalledWith('test-token', 'resp-1');
    });
  });

  it('convert calls convertFeedbackResponse with the chosen type', async () => {
    render(<FeedbackResponsesPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-response-resp-1-convert-select')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('feedback-response-resp-1-convert-select'), {
      target: { value: 'ACTION_ITEM' },
    });
    fireEvent.click(screen.getByTestId('feedback-response-resp-1-convert'));

    await waitFor(() => {
      expect(mockConvert).toHaveBeenCalledWith('test-token', 'resp-1', { type: 'ACTION_ITEM' });
    });
  });

  it('bulk approve calls bulkUpdateFeedbackResponses', async () => {
    render(<FeedbackResponsesPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-response-resp-1-select')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-response-resp-1-select'));
    fireEvent.click(screen.getByTestId('feedback-responses-bulk-approve'));

    await waitFor(() => {
      expect(mockBulk).toHaveBeenCalledWith('test-token', { responseIds: ['resp-1'], action: 'APPROVE' });
    });
  });
});
