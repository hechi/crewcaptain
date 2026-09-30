import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';

jest.mock('@/lib/useStableToken', () => {
  const getToken = () => 'test-token';
  const value = { getToken, isAuthenticated: true, status: 'authenticated' };
  return { useStableToken: jest.fn(() => value) };
});

jest.mock('@/lib/api-client', () => ({
  listFeedbackSummaries: jest.fn(),
  generateFeedbackSummary: jest.fn(),
  saveFeedbackSummary: jest.fn(),
  getFeedbackAnalytics: jest.fn(),
}));

import {
  listFeedbackSummaries,
  generateFeedbackSummary,
  saveFeedbackSummary,
  getFeedbackAnalytics,
} from '@/lib/api-client';
import FeedbackSummaryPanel from '@/components/feedback/FeedbackSummaryPanel';

const mockList = listFeedbackSummaries as jest.MockedFunction<typeof listFeedbackSummaries>;
const mockGenerate = generateFeedbackSummary as jest.MockedFunction<typeof generateFeedbackSummary>;
const mockSave = saveFeedbackSummary as jest.MockedFunction<typeof saveFeedbackSummary>;
const mockAnalytics = getFeedbackAnalytics as jest.MockedFunction<typeof getFeedbackAnalytics>;

const PERSON_ID = 'person-1';

describe('FeedbackSummaryPanel', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockList.mockResolvedValue([]);
    mockAnalytics.mockResolvedValue({
      totalResponses: 8,
      overallAverageRating: 4.1,
      periods: [],
      topThemes: [],
    });
    mockGenerate.mockResolvedValue({ content: 'Alex has shown strong ownership this period.' });
    mockSave.mockResolvedValue({
      id: 'sum-1',
      personId: PERSON_ID,
      periodFrom: '2025-07-01',
      periodTo: '2026-01-01',
      content: 'Alex has shown strong ownership this period.',
      responseCount: 8,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });
  });

  it('generate shows the returned content in an editable textarea', async () => {
    render(<FeedbackSummaryPanel personId={PERSON_ID} aiAvailable />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-summary-generate')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-summary-generate'));

    await waitFor(() => {
      expect(screen.getByTestId('feedback-summary-textarea')).toBeInTheDocument();
    });
    expect(screen.getByTestId('feedback-summary-textarea')).toHaveValue(
      'Alex has shown strong ownership this period.'
    );
  });

  it('save calls saveFeedbackSummary with the draft and computed period', async () => {
    render(<FeedbackSummaryPanel personId={PERSON_ID} aiAvailable />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-summary-generate')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-summary-generate'));

    await waitFor(() => {
      expect(screen.getByTestId('feedback-summary-save')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-summary-save'));

    await waitFor(() => {
      expect(mockSave).toHaveBeenCalledWith(
        'test-token',
        PERSON_ID,
        expect.objectContaining({
          content: 'Alex has shown strong ownership this period.',
          responseCount: 8,
        })
      );
    });
    const args = mockSave.mock.calls[0][2];
    expect(typeof args.periodFrom).toBe('string');
    expect(typeof args.periodTo).toBe('string');
  });

  it('hides the AI generate button when aiAvailable is false', async () => {
    render(<FeedbackSummaryPanel personId={PERSON_ID} aiAvailable={false} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-summary-panel')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('feedback-summary-generate')).not.toBeInTheDocument();
  });

  it('shows an inline error when generation returns an error', async () => {
    mockGenerate.mockResolvedValue({ error: 'AI is not configured.' });

    render(<FeedbackSummaryPanel personId={PERSON_ID} aiAvailable />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-summary-generate')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-summary-generate'));

    await waitFor(() => {
      expect(screen.getByTestId('feedback-summary-gen-error')).toHaveTextContent('AI is not configured.');
    });
  });
});
