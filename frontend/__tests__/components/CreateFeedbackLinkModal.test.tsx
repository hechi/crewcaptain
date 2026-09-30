import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';

jest.mock('@/lib/useStableToken', () => {
  const getToken = () => 'test-token';
  const value = { getToken, isAuthenticated: true, status: 'authenticated' };
  return { useStableToken: jest.fn(() => value) };
});

jest.mock('@/lib/api-client', () => ({
  listFeedbackTemplates: jest.fn(),
  getFeedbackStarterTemplates: jest.fn(),
  generateFeedbackTemplate: jest.fn(),
  createFeedbackLink: jest.fn(),
}));

import {
  listFeedbackTemplates,
  getFeedbackStarterTemplates,
  createFeedbackLink,
} from '@/lib/api-client';
import CreateFeedbackLinkModal from '@/components/feedback/CreateFeedbackLinkModal';

const mockListTemplates = listFeedbackTemplates as jest.MockedFunction<typeof listFeedbackTemplates>;
const mockStarters = getFeedbackStarterTemplates as jest.MockedFunction<typeof getFeedbackStarterTemplates>;
const mockCreate = createFeedbackLink as jest.MockedFunction<typeof createFeedbackLink>;

const PERSON_ID = 'person-1';

const template = {
  id: 'tpl-1',
  title: 'Quarterly peer feedback',
  description: null,
  questions: [
    { id: 'q1', type: 'RATING' as const, text: 'How is their collaboration?', required: true, lowLabel: null, highLabel: null, showIf: null },
  ],
};

describe('CreateFeedbackLinkModal', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockListTemplates.mockResolvedValue([template]);
    mockStarters.mockResolvedValue([]);
    mockCreate.mockResolvedValue({
      id: 'link-1',
      personId: PERSON_ID,
      token: 'tok999',
      title: 'Quarterly peer feedback',
      description: null,
      label: null,
      requestSubmitterInfo: true,
      status: 'ACTIVE',
      expiresAt: '2026-03-01T00:00:00Z',
      revokedAt: null,
      submissionCount: 0,
      lastSubmissionAt: null,
      createdAt: '2026-01-01T00:00:00Z',
    });
    Object.assign(navigator, { clipboard: { writeText: jest.fn().mockResolvedValue(undefined) } });
  });

  it('choosing a saved template + create calls createFeedbackLink with {templateId} and shows the /f/ URL', async () => {
    render(
      <CreateFeedbackLinkModal isOpen personId={PERSON_ID} personName="Alex" onClose={jest.fn()} />
    );

    await waitFor(() => {
      expect(screen.getByTestId('create-feedback-link-template-select')).toBeInTheDocument();
    });

    // The saved template option should be present
    await waitFor(() => {
      expect(screen.getByRole('option', { name: 'Quarterly peer feedback' })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('create-feedback-link-template-select'), {
      target: { value: 'tpl-1' },
    });

    fireEvent.click(screen.getByTestId('create-feedback-link-submit'));

    await waitFor(() => {
      expect(mockCreate).toHaveBeenCalledWith(
        'test-token',
        PERSON_ID,
        expect.objectContaining({ templateId: 'tpl-1', expiresInDays: 14, requestSubmitterInfo: true })
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('create-feedback-link-url')).toHaveTextContent(
        `${window.location.origin}/f/tok999`
      );
    });
  });

  it('renders the warning banner', async () => {
    render(<CreateFeedbackLinkModal isOpen personId={PERSON_ID} onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('create-feedback-link-warning')).toHaveTextContent(
        'Anyone with this link can submit feedback.'
      );
    });
  });

  it('disables create until a template is chosen', async () => {
    render(<CreateFeedbackLinkModal isOpen personId={PERSON_ID} onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('create-feedback-link-submit')).toBeInTheDocument();
    });
    expect(screen.getByTestId('create-feedback-link-submit')).toBeDisabled();
  });
});
