import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';

jest.mock('@/lib/useStableToken', () => {
  const getToken = () => 'test-token';
  const value = { getToken, isAuthenticated: true, status: 'authenticated' };
  return { useStableToken: jest.fn(() => value) };
});

jest.mock('@/lib/api-client', () => ({
  getFeedbackAnalytics: jest.fn(),
}));

import { getFeedbackAnalytics } from '@/lib/api-client';
import FeedbackAnalyticsPanel from '@/components/feedback/FeedbackAnalyticsPanel';

const mockAnalytics = getFeedbackAnalytics as jest.MockedFunction<typeof getFeedbackAnalytics>;

const PERSON_ID = 'person-1';

describe('FeedbackAnalyticsPanel', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders overall average, total, top themes, and an <svg> for periods', async () => {
    mockAnalytics.mockResolvedValue({
      totalResponses: 12,
      overallAverageRating: 4.25,
      periods: [
        { label: 'Jan', averageRating: 4.0, responseCount: 5 },
        { label: 'Feb', averageRating: 4.5, responseCount: 7 },
      ],
      topThemes: [
        { theme: 'communication', count: 6 },
        { theme: 'ownership', count: 4 },
      ],
    });

    render(<FeedbackAnalyticsPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-analytics-average')).toBeInTheDocument();
    });

    expect(screen.getByTestId('feedback-analytics-average')).toHaveTextContent('4.3/5');
    expect(screen.getByTestId('feedback-analytics-total')).toHaveTextContent('12');
    expect(screen.getByTestId('feedback-analytics-theme-communication')).toHaveTextContent('communication');
    expect(screen.getByTestId('feedback-analytics-theme-ownership')).toBeInTheDocument();

    // Inline SVG chart is rendered from periods
    const chart = screen.getByTestId('feedback-analytics-chart');
    expect(chart.tagName.toLowerCase()).toBe('svg');
    expect(screen.getByTestId('feedback-analytics-point-0')).toBeInTheDocument();
    expect(screen.getByTestId('feedback-analytics-point-1')).toBeInTheDocument();
  });

  it('shows empty state when there are no responses', async () => {
    mockAnalytics.mockResolvedValue({
      totalResponses: 0,
      overallAverageRating: null,
      periods: [],
      topThemes: [],
    });

    render(<FeedbackAnalyticsPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-analytics-empty')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('feedback-analytics-chart')).not.toBeInTheDocument();
  });
});
