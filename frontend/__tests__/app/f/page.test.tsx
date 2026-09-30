import React from 'react';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import PublicFeedbackPage from '@/app/f/[token]/page';
import { ApiException } from '@/types/api';

jest.mock('next/navigation', () => ({
  useParams: () => ({ token: 'link-token-abc' }),
}));

const mockGetForm = jest.fn();
const mockSubmit = jest.fn();

jest.mock('@/lib/api-client', () => ({
  getPublicFeedbackForm: (...args: unknown[]) => mockGetForm(...args),
  submitPublicFeedback: (...args: unknown[]) => mockSubmit(...args),
}));

const baseForm = {
  personName: 'Alice',
  title: 'Peer feedback',
  description: 'Quarterly',
  requestSubmitterInfo: true,
  questions: [
    { id: 'q1', type: 'RATING', text: 'How was collaboration?', required: true, lowLabel: 'Poor', highLabel: 'Great', showIf: null },
    { id: 'q2', type: 'TEXT', text: 'What could improve?', required: false, showIf: { questionId: 'q1', operator: 'LTE', value: 2 } },
    { id: 'q3', type: 'LIKERT', text: 'They communicate clearly', required: true, showIf: null },
  ],
};

describe('PublicFeedbackPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders questions from the fetched form', async () => {
    mockGetForm.mockResolvedValue(baseForm);
    await act(async () => { render(<PublicFeedbackPage />); });

    await waitFor(() => {
      expect(screen.getByTestId('public-feedback-form')).toBeInTheDocument();
    });
    expect(mockGetForm).toHaveBeenCalledWith('link-token-abc');
    expect(screen.getByTestId('public-feedback-title')).toHaveTextContent('Provide feedback for Alice');
    expect(screen.getByTestId('public-feedback-subtitle')).toHaveTextContent('Your input helps Alice reflect and improve');

    // RATING renders 1-5 buttons + labels
    expect(screen.getByTestId('public-feedback-rating-q1-1')).toBeInTheDocument();
    expect(screen.getByTestId('public-feedback-rating-q1-5')).toBeInTheDocument();
    expect(screen.getByTestId('public-feedback-low-label-q1')).toHaveTextContent('Poor');
    expect(screen.getByTestId('public-feedback-high-label-q1')).toHaveTextContent('Great');

    // LIKERT renders 5 options
    expect(screen.getByTestId('public-feedback-likert-q3-1')).toHaveTextContent('Strongly disagree');
    expect(screen.getByTestId('public-feedback-likert-q3-5')).toHaveTextContent('Strongly agree');
  });

  it('hides a showIf follow-up until the trigger rating satisfies the rule', async () => {
    mockGetForm.mockResolvedValue(baseForm);
    await act(async () => { render(<PublicFeedbackPage />); });
    await waitFor(() => expect(screen.getByTestId('public-feedback-form')).toBeInTheDocument());

    // q2 shows only when q1 <= 2
    expect(screen.queryByTestId('public-feedback-question-q2')).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId('public-feedback-rating-q1-5'));
    expect(screen.queryByTestId('public-feedback-question-q2')).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId('public-feedback-rating-q1-1'));
    expect(screen.getByTestId('public-feedback-question-q2')).toBeInTheDocument();
  });

  it('includes a honeypot field', async () => {
    mockGetForm.mockResolvedValue(baseForm);
    await act(async () => { render(<PublicFeedbackPage />); });
    await waitFor(() => expect(screen.getByTestId('public-feedback-form')).toBeInTheDocument());

    const honeypot = screen.getByTestId('public-feedback-honeypot') as HTMLInputElement;
    expect(honeypot).toBeInTheDocument();
    expect(honeypot).toHaveAttribute('name', 'website');
    expect(honeypot).toHaveAttribute('tabindex', '-1');
  });

  it('hides name/email when anonymous toggle is on (default) and shows them when off', async () => {
    mockGetForm.mockResolvedValue(baseForm);
    await act(async () => { render(<PublicFeedbackPage />); });
    await waitFor(() => expect(screen.getByTestId('public-feedback-form')).toBeInTheDocument());

    // Default anonymous ON -> no name/email
    expect(screen.queryByTestId('public-feedback-name')).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId('public-feedback-anonymous-toggle'));
    expect(screen.getByTestId('public-feedback-name')).toBeInTheDocument();
    expect(screen.getByTestId('public-feedback-email')).toBeInTheDocument();
  });

  it('submits successfully and shows the confirmation', async () => {
    mockGetForm.mockResolvedValue(baseForm);
    mockSubmit.mockResolvedValue(undefined);
    await act(async () => { render(<PublicFeedbackPage />); });
    await waitFor(() => expect(screen.getByTestId('public-feedback-form')).toBeInTheDocument());

    // Answer the required visible questions (q1 rating, q3 likert)
    fireEvent.click(screen.getByTestId('public-feedback-rating-q1-4'));
    fireEvent.click(screen.getByTestId('public-feedback-likert-q3-5'));

    await act(async () => {
      fireEvent.click(screen.getByTestId('public-feedback-submit'));
    });

    expect(mockSubmit).toHaveBeenCalledWith('link-token-abc', expect.objectContaining({
      anonymous: true,
      submitterName: null,
      submitterEmail: null,
      website: '',
    }));
    const submission = mockSubmit.mock.calls[0][1];
    // Hidden q2 must not be submitted
    expect(submission.answers.find((a: { questionId: string }) => a.questionId === 'q2')).toBeUndefined();
    expect(screen.getByTestId('public-feedback-confirmation')).toHaveTextContent('Thanks — your feedback was recorded.');
  });

  it('shows the expired message when the form load throws 410 expired', async () => {
    mockGetForm.mockRejectedValue(new ApiException(410, 'Gone', 'This link has expired', '2026-01-01'));
    await act(async () => { render(<PublicFeedbackPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('public-feedback-unavailable')).toHaveTextContent('This link has expired');
    });
  });

  it('shows the revoked message when 410 mentions revoked', async () => {
    mockGetForm.mockRejectedValue(new ApiException(410, 'Gone', 'This link has been revoked', '2026-01-01'));
    await act(async () => { render(<PublicFeedbackPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('public-feedback-unavailable')).toHaveTextContent('This link has been revoked');
    });
  });

  it('shows not-found on 404', async () => {
    mockGetForm.mockRejectedValue(new ApiException(404, 'Not Found', 'nope', '2026-01-01'));
    await act(async () => { render(<PublicFeedbackPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('public-feedback-unavailable')).toHaveTextContent('This feedback link was not found');
    });
  });

  it('shows the rate-limit message on 429 submit', async () => {
    mockGetForm.mockResolvedValue(baseForm);
    mockSubmit.mockRejectedValue(new ApiException(429, 'Too Many Requests', 'slow down', '2026-01-01'));
    await act(async () => { render(<PublicFeedbackPage />); });
    await waitFor(() => expect(screen.getByTestId('public-feedback-form')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('public-feedback-rating-q1-4'));
    fireEvent.click(screen.getByTestId('public-feedback-likert-q3-5'));

    await act(async () => {
      fireEvent.click(screen.getByTestId('public-feedback-submit'));
    });

    expect(screen.getByTestId('public-feedback-error')).toHaveTextContent('Too many submissions from this device');
  });
});
