import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';

jest.mock('@/lib/useStableToken', () => {
  const getToken = () => 'test-token';
  const value = { getToken, isAuthenticated: true, status: 'authenticated' };
  return { useStableToken: jest.fn(() => value) };
});

jest.mock('@/lib/api-client', () => ({
  listFeedbackLinks: jest.fn(),
  revokeFeedbackLink: jest.fn(),
  extendFeedbackLink: jest.fn(),
}));

import { listFeedbackLinks, revokeFeedbackLink, extendFeedbackLink } from '@/lib/api-client';
import FeedbackLinksPanel from '@/components/feedback/FeedbackLinksPanel';

const mockList = listFeedbackLinks as jest.MockedFunction<typeof listFeedbackLinks>;
const mockRevoke = revokeFeedbackLink as jest.MockedFunction<typeof revokeFeedbackLink>;
const mockExtend = extendFeedbackLink as jest.MockedFunction<typeof extendFeedbackLink>;

const PERSON_ID = 'person-1';

const activeLink = {
  id: 'link-1',
  personId: PERSON_ID,
  token: 'abc123',
  title: 'Quarterly peer feedback',
  description: null,
  label: 'Q1 review',
  requestSubmitterInfo: true,
  status: 'ACTIVE' as const,
  expiresAt: '2026-03-01T00:00:00Z',
  revokedAt: null,
  submissionCount: 3,
  lastSubmissionAt: '2026-01-20T12:00:00Z',
  createdAt: '2026-01-01T00:00:00Z',
};

describe('FeedbackLinksPanel', () => {
  let writeText: jest.Mock;
  let confirmSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    mockList.mockResolvedValue([activeLink]);
    mockRevoke.mockResolvedValue({ ...activeLink, status: 'REVOKED' });
    mockExtend.mockResolvedValue({ ...activeLink, expiresAt: '2026-04-01T00:00:00Z' });

    writeText = jest.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });

    confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    confirmSpy.mockRestore();
  });

  it('renders a link with status, expiry, and submission count', async () => {
    render(<FeedbackLinksPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-link-link-1')).toBeInTheDocument();
    });
    expect(screen.getByText('Quarterly peer feedback')).toBeInTheDocument();
    expect(screen.getByTestId('feedback-link-link-1-status')).toHaveTextContent('ACTIVE');
    // The prominent note is present
    expect(screen.getByTestId('feedback-links-warning')).toHaveTextContent(
      'Anyone with this link can submit feedback.'
    );
  });

  it('copy writes the full public /f/{token} URL to the clipboard', async () => {
    render(<FeedbackLinksPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-link-link-1-copy')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-link-link-1-copy'));

    await waitFor(() => {
      expect(writeText).toHaveBeenCalledWith(`${window.location.origin}/f/abc123`);
    });
  });

  it('revoke calls revokeFeedbackLink', async () => {
    render(<FeedbackLinksPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-link-link-1-revoke')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-link-link-1-revoke'));

    await waitFor(() => {
      expect(mockRevoke).toHaveBeenCalledWith('test-token', 'link-1');
    });
  });

  it('extend calls extendFeedbackLink with the entered days', async () => {
    render(<FeedbackLinksPanel personId={PERSON_ID} />);

    await waitFor(() => {
      expect(screen.getByTestId('feedback-link-link-1-extend')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('feedback-link-link-1-extend-days'), { target: { value: '30' } });
    fireEvent.click(screen.getByTestId('feedback-link-link-1-extend'));

    await waitFor(() => {
      expect(mockExtend).toHaveBeenCalledWith('test-token', 'link-1', 30);
    });
  });

  it('empty state guides the user to create a link', async () => {
    mockList.mockResolvedValue([]);
    render(<FeedbackLinksPanel personId={PERSON_ID} />);

    const empty = await screen.findByTestId('feedback-links-empty');
    expect(empty).toHaveTextContent('Create feedback link');
  });
});
