import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import AiPrepAssistant from '@/components/one-on-one/AiPrepAssistant';
import * as apiClient from '@/lib/api-client';

jest.mock('@/lib/api-client');

const mockGenerateAiAgendaSuggestions = apiClient.generateAiAgendaSuggestions as jest.MockedFunction<typeof apiClient.generateAiAgendaSuggestions>;

describe('AiPrepAssistant', () => {
  const defaultProps = {
    token: 'test-token',
    personId: 'person-123',
    onAddSuggestion: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the generate button', () => {
    render(<AiPrepAssistant {...defaultProps} />);
    expect(screen.getByTestId('ai-generate-btn')).toBeInTheDocument();
    expect(screen.getByText('Generate Agenda')).toBeInTheDocument();
  });

  it('should render the AI Prep Assistant heading', () => {
    render(<AiPrepAssistant {...defaultProps} />);
    expect(screen.getByText(/AI Prep Assistant/)).toBeInTheDocument();
  });

  it('should render a description of what the assistant does', () => {
    render(<AiPrepAssistant {...defaultProps} />);
    expect(screen.getByTestId('ai-description')).toBeInTheDocument();
    expect(screen.getByText(/past 1:1 notes/)).toBeInTheDocument();
  });

  it('should show loading state when generating', async () => {
    mockGenerateAiAgendaSuggestions.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ suggestions: [], error: null }), 100))
    );

    render(<AiPrepAssistant {...defaultProps} />);
    fireEvent.click(screen.getByTestId('ai-generate-btn'));

    expect(screen.getByTestId('ai-loading')).toBeInTheDocument();
    expect(screen.getByText('Synthesizing context...')).toBeInTheDocument();
    expect(screen.getByTestId('ai-generate-btn')).toBeDisabled();
  });

  it('should display suggestions on success', async () => {
    mockGenerateAiAgendaSuggestions.mockResolvedValue({
      suggestions: ['Follow up on project', 'Discuss blockers', 'Review goals'],
      error: null,
    });

    render(<AiPrepAssistant {...defaultProps} />);
    fireEvent.click(screen.getByTestId('ai-generate-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('ai-suggestions')).toBeInTheDocument();
    });

    expect(screen.getByText('Follow up on project')).toBeInTheDocument();
    expect(screen.getByText('Discuss blockers')).toBeInTheDocument();
    expect(screen.getByText('Review goals')).toBeInTheDocument();
  });

  it('should display error message on failure', async () => {
    mockGenerateAiAgendaSuggestions.mockResolvedValue({
      suggestions: [],
      error: 'Cannot connect to AI API.',
    });

    render(<AiPrepAssistant {...defaultProps} />);
    fireEvent.click(screen.getByTestId('ai-generate-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('ai-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Cannot connect to AI API.')).toBeInTheDocument();
  });

  it('should display error on API exception', async () => {
    mockGenerateAiAgendaSuggestions.mockRejectedValue(new Error('Network error'));

    render(<AiPrepAssistant {...defaultProps} />);
    fireEvent.click(screen.getByTestId('ai-generate-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('ai-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('should call onAddSuggestion when add button is clicked', async () => {
    mockGenerateAiAgendaSuggestions.mockResolvedValue({
      suggestions: ['Follow up on project', 'Discuss blockers'],
      error: null,
    });

    render(<AiPrepAssistant {...defaultProps} />);
    fireEvent.click(screen.getByTestId('ai-generate-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('ai-suggestion-0')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-add-suggestion-0'));

    expect(defaultProps.onAddSuggestion).toHaveBeenCalledWith('Follow up on project');
  });

  it('should show added state after adding a suggestion', async () => {
    mockGenerateAiAgendaSuggestions.mockResolvedValue({
      suggestions: ['Follow up on project'],
      error: null,
    });

    render(<AiPrepAssistant {...defaultProps} />);
    fireEvent.click(screen.getByTestId('ai-generate-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('ai-add-suggestion-0')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-add-suggestion-0'));

    expect(screen.getByText('✓ Added')).toBeInTheDocument();
    expect(screen.getByTestId('ai-add-suggestion-0')).toBeDisabled();
  });

  it('should clear previous suggestions when regenerating', async () => {
    mockGenerateAiAgendaSuggestions
      .mockResolvedValueOnce({
        suggestions: ['First suggestion'],
        error: null,
      })
      .mockResolvedValueOnce({
        suggestions: ['New suggestion'],
        error: null,
      });

    render(<AiPrepAssistant {...defaultProps} />);

    // First generation
    fireEvent.click(screen.getByTestId('ai-generate-btn'));
    await waitFor(() => {
      expect(screen.getByText('First suggestion')).toBeInTheDocument();
    });

    // Second generation
    fireEvent.click(screen.getByTestId('ai-generate-btn'));
    await waitFor(() => {
      expect(screen.getByText('New suggestion')).toBeInTheDocument();
    });

    expect(screen.queryByText('First suggestion')).not.toBeInTheDocument();
  });

  it('should call API with correct parameters', async () => {
    mockGenerateAiAgendaSuggestions.mockResolvedValue({
      suggestions: [],
      error: null,
    });

    render(<AiPrepAssistant {...defaultProps} />);
    fireEvent.click(screen.getByTestId('ai-generate-btn'));

    await waitFor(() => {
      expect(mockGenerateAiAgendaSuggestions).toHaveBeenCalledWith('test-token', 'person-123');
    });
  });

  it('should have accessible button labels', () => {
    render(<AiPrepAssistant {...defaultProps} />);
    expect(screen.getByLabelText('Generate suggested agenda items')).toBeInTheDocument();
  });
});
