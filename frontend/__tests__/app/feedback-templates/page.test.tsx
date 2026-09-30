import React from 'react';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import FeedbackTemplatesPage from '@/app/feedback-templates/page';

jest.mock('next-auth/react', () => ({
  useSession: () => ({
    data: { accessToken: 'test-token', user: { name: 'Test User' } },
    status: 'authenticated',
  }),
  signOut: jest.fn(),
}));

const stableGetToken = () => 'test-token';
jest.mock('@/lib/useStableToken', () => ({
  useStableToken: () => ({
    getToken: stableGetToken,
    isAuthenticated: true,
    status: 'authenticated',
  }),
}));

const mockList = jest.fn();
const mockGet = jest.fn();
const mockStarters = jest.fn();
const mockCreate = jest.fn();
const mockUpdate = jest.fn();
const mockDelete = jest.fn();
const mockGenerate = jest.fn();
const mockGetSettings = jest.fn();

jest.mock('@/lib/api-client', () => ({
  listFeedbackTemplates: (...args: unknown[]) => mockList(...args),
  getFeedbackTemplate: (...args: unknown[]) => mockGet(...args),
  getFeedbackStarterTemplates: (...args: unknown[]) => mockStarters(...args),
  createFeedbackTemplate: (...args: unknown[]) => mockCreate(...args),
  updateFeedbackTemplate: (...args: unknown[]) => mockUpdate(...args),
  deleteFeedbackTemplate: (...args: unknown[]) => mockDelete(...args),
  generateFeedbackTemplate: (...args: unknown[]) => mockGenerate(...args),
  getUserSettings: (...args: unknown[]) => mockGetSettings(...args),
}));

const templates = [
  { id: 't1', title: 'Peer feedback', description: 'For peers', questions: [{ id: 'q1', type: 'RATING', text: 'A', required: true }] },
  { id: 't2', title: 'Manager check-in', description: null, questions: [] },
];

describe('FeedbackTemplatesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockList.mockResolvedValue(templates);
    mockStarters.mockResolvedValue([]);
    // AI available by default so the AI-generate tests exercise the visible control.
    mockGetSettings.mockResolvedValue({ aiAvailable: true });
  });

  it('lists the manager templates with question counts', async () => {
    await act(async () => { render(<FeedbackTemplatesPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('feedback-template-row-t1')).toBeInTheDocument();
    });
    expect(screen.getByTestId('feedback-template-row-t1')).toHaveTextContent('Peer feedback');
    expect(screen.getByTestId('feedback-template-row-t1')).toHaveTextContent('1 question');
    expect(screen.getByTestId('feedback-template-row-t2')).toHaveTextContent('0 questions');
  });

  it('deletes a template via the api', async () => {
    mockDelete.mockResolvedValue(undefined);
    await act(async () => { render(<FeedbackTemplatesPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('feedback-template-delete-t1')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('feedback-template-delete-t1'));
    });

    expect(mockDelete).toHaveBeenCalledWith('test-token', 't1');
    await waitFor(() => {
      expect(screen.queryByTestId('feedback-template-row-t1')).not.toBeInTheDocument();
    });
  });

  it('shows an inline AI error and does not open the builder on {error}', async () => {
    mockGenerate.mockResolvedValue({ error: 'AI is unavailable' });
    await act(async () => { render(<FeedbackTemplatesPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('feedback-templates-ai-brief')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('feedback-templates-ai-brief'), { target: { value: 'peer review' } });
    await act(async () => {
      fireEvent.click(screen.getByTestId('feedback-templates-ai-generate-btn'));
    });

    expect(mockGenerate).toHaveBeenCalledWith('test-token', 'peer review');
    expect(screen.getByTestId('feedback-templates-ai-error')).toHaveTextContent('AI is unavailable');
    expect(screen.queryByTestId('feedback-template-builder')).not.toBeInTheDocument();
  });

  it('shows a spinner while the AI request is in flight', async () => {
    let resolveGen: (v: { draft?: unknown; error?: string }) => void = () => {};
    mockGenerate.mockReturnValue(new Promise((res) => { resolveGen = res; }));
    await act(async () => { render(<FeedbackTemplatesPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('feedback-templates-ai-brief')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('feedback-templates-ai-brief'), { target: { value: 'brief' } });
    fireEvent.click(screen.getByTestId('feedback-templates-ai-generate-btn'));

    // While the promise is pending, the spinner is visible.
    expect(await screen.findByTestId('ai-spinner')).toBeInTheDocument();

    // Resolve to finish and clear the spinner.
    await act(async () => {
      resolveGen({ error: 'done' });
    });
    await waitFor(() => {
      expect(screen.queryByTestId('ai-spinner')).not.toBeInTheDocument();
    });
  });

  it('opens the builder pre-filled on a successful AI {draft}', async () => {
    mockGenerate.mockResolvedValue({
      draft: { title: 'Generated', description: null, questions: [{ id: 'q1', type: 'TEXT', text: 'Thoughts?', required: false }] },
    });
    await act(async () => { render(<FeedbackTemplatesPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('feedback-templates-ai-brief')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('feedback-templates-ai-brief'), { target: { value: 'brief' } });
    await act(async () => {
      fireEvent.click(screen.getByTestId('feedback-templates-ai-generate-btn'));
    });

    expect(screen.getByTestId('feedback-template-builder')).toBeInTheDocument();
    expect(screen.getByTestId('ftb-title-input')).toHaveValue('Generated');
  });

  it('hides the AI generate section when AI is not available', async () => {
    mockGetSettings.mockResolvedValue({ aiAvailable: false });
    await act(async () => { render(<FeedbackTemplatesPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('feedback-templates-new-btn')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('feedback-templates-ai')).not.toBeInTheDocument();
    expect(screen.queryByTestId('feedback-templates-ai-generate-btn')).not.toBeInTheDocument();
  });

  it('shows the AI generate section when AI is available', async () => {
    mockGetSettings.mockResolvedValue({ aiAvailable: true });
    await act(async () => { render(<FeedbackTemplatesPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('feedback-templates-ai')).toBeInTheDocument();
    });
  });

  it('still renders (AI hidden) when the settings fetch fails', async () => {
    mockGetSettings.mockRejectedValue(new Error('boom'));
    await act(async () => { render(<FeedbackTemplatesPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('feedback-templates-new-btn')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('feedback-templates-ai')).not.toBeInTheDocument();
  });

  it('creates a new template via the builder', async () => {
    mockCreate.mockResolvedValue({ id: 't3' });
    await act(async () => { render(<FeedbackTemplatesPage />); });
    await waitFor(() => {
      expect(screen.getByTestId('feedback-templates-new-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('feedback-templates-new-btn'));
    fireEvent.change(screen.getByTestId('ftb-title-input'), { target: { value: 'Brand new' } });
    fireEvent.change(screen.getByTestId('ftb-question-0-text'), { target: { value: 'Rate it' } });

    await act(async () => {
      fireEvent.click(screen.getByTestId('ftb-save'));
    });

    expect(mockCreate).toHaveBeenCalledWith('test-token', expect.objectContaining({ title: 'Brand new' }));
  });
});
