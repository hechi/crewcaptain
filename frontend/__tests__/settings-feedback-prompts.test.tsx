import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import SettingsPage from '@/app/settings/page';

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  getUserSettings: jest.fn(),
  updateUserSettings: jest.fn(),
}));

jest.mock('@/components/ThemeProvider', () => ({
  useTheme: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { getUserSettings, updateUserSettings } from '@/lib/api-client';
import { useTheme } from '@/components/ThemeProvider';

const mockUseSession = useSession as jest.Mock;
const mockGetUserSettings = getUserSettings as jest.Mock;
const mockUpdateUserSettings = updateUserSettings as jest.Mock;
const mockUseTheme = useTheme as jest.Mock;

// AI must be enabled for the prompt fields to render.
const aiEnabledSettings = {
  dueSoonDays: 3,
  staleOneOnOneDays: 14,
  anniversaryLookaheadDays: 30,
  theme: 'DARK' as const,
  showAchievements: true,
  notifyActionItemOverdue: true,
  notifyActionItemDueSoon: true,
  notifyStaleOneOnOne: true,
  notifyUpcomingAnniversary: true,
  aiEnabled: true,
  aiApiBaseUrl: null,
  aiModelName: null,
  aiPrivacyMode: true,
  aiWritingStyle: 'NARRATIVE' as const,
  aiAutoExecuteCommands: false,
  kudosRefinementPrompt: null,
  pdpOptimizationPrompt: null,
  agendaPrepPrompt: null,
  narrativePrompt: null,
  outcomeExtractorPrompt: null,
  trendRadarPrompt: null,
  linkSuggestionsPrompt: null,
  triageHintPrompt: null,
  commandTerminalPrompt: null,
  feedbackTemplatePrompt: null,
  feedbackSummaryPrompt: null,
  aiConfigSource: 'USER_SETTINGS' as const,
  aiAvailable: true,
};

describe('SettingsPage feedback prompts', () => {
  const mockSetTheme = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test User' } },
      status: 'authenticated',
    });
    mockUseTheme.mockReturnValue({ theme: 'DARK', setTheme: mockSetTheme });
    mockGetUserSettings.mockResolvedValue(aiEnabledSettings);
  });

  it('renders the two feedback prompt textareas', async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('input-feedback-template-prompt')).toBeInTheDocument();
      expect(screen.getByTestId('input-feedback-summary-prompt')).toBeInTheDocument();
    });
  });

  it('loads existing prompt values from the settings response', async () => {
    mockGetUserSettings.mockResolvedValue({
      ...aiEnabledSettings,
      feedbackTemplatePrompt: 'template prompt',
      feedbackSummaryPrompt: 'summary prompt',
    });
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('input-feedback-template-prompt')).toHaveValue('template prompt');
      expect(screen.getByTestId('input-feedback-summary-prompt')).toHaveValue('summary prompt');
    });
  });

  it('includes the two prompts in the save payload', async () => {
    mockUpdateUserSettings.mockResolvedValue(aiEnabledSettings);
    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId('input-feedback-template-prompt')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-feedback-template-prompt'), { target: { value: 'my template prompt' } });
    fireEvent.change(screen.getByTestId('input-feedback-summary-prompt'), { target: { value: 'my summary prompt' } });

    await act(async () => {
      fireEvent.click(screen.getByTestId('settings-save-btn'));
    });

    await waitFor(() => {
      expect(mockUpdateUserSettings).toHaveBeenCalledWith('test-token', expect.objectContaining({
        feedbackTemplatePrompt: 'my template prompt',
        feedbackSummaryPrompt: 'my summary prompt',
      }));
    });
  });

  it('resets a prompt to empty via Reset to Default', async () => {
    mockGetUserSettings.mockResolvedValue({
      ...aiEnabledSettings,
      feedbackTemplatePrompt: 'something',
    });
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('input-feedback-template-prompt')).toHaveValue('something');
    });

    fireEvent.click(screen.getByTestId('input-feedback-template-prompt-reset-btn'));
    expect(screen.getByTestId('input-feedback-template-prompt')).toHaveValue('');
  });
});
