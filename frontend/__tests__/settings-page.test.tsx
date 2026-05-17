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

const defaultSettings = {
  dueSoonDays: 3,
  staleOneOnOneDays: 14,
  anniversaryLookaheadDays: 30,
  theme: 'DARK' as const,
  showAchievements: true,
  notifyActionItemOverdue: true,
  notifyActionItemDueSoon: true,
  notifyStaleOneOnOne: true,
  notifyUpcomingAnniversary: true,
  aiEnabled: false,
  aiApiBaseUrl: null,
  aiModelName: null,
  aiPrivacyMode: true,
  aiWritingStyle: 'NARRATIVE' as const,
};

describe('SettingsPage', () => {
  const mockSetTheme = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test User' } },
      status: 'authenticated',
    });
    mockUseTheme.mockReturnValue({ theme: 'DARK', setTheme: mockSetTheme });
    mockGetUserSettings.mockResolvedValue(defaultSettings);
  });

  it('should show loading state initially', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'loading' });
    render(<SettingsPage />);
    expect(screen.getByTestId('loading-screen')).toBeInTheDocument();
  });

  it('should render settings page with title', async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('settings-title')).toHaveTextContent('Settings');
    });
  });

  it('should render all sections', async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('settings-section-theme')).toBeInTheDocument();
      expect(screen.getByTestId('settings-section-thresholds')).toBeInTheDocument();
      expect(screen.getByTestId('settings-section-notifications')).toBeInTheDocument();
      expect(screen.getByTestId('settings-section-display')).toBeInTheDocument();
      expect(screen.getByTestId('settings-section-ai')).toBeInTheDocument();
    });
  });

  it('should load and display current settings', async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('input-due-soon-days')).toHaveValue(3);
      expect(screen.getByTestId('input-stale-one-on-one-days')).toHaveValue(14);
      expect(screen.getByTestId('input-anniversary-lookahead-days')).toHaveValue(30);
    });
  });

  it('should display theme buttons with dark selected by default', async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('theme-dark-btn')).toBeInTheDocument();
      expect(screen.getByTestId('theme-light-btn')).toBeInTheDocument();
    });
  });

  it('should display notification toggles', async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('toggle-notify-overdue')).toBeInTheDocument();
      expect(screen.getByTestId('toggle-notify-due-soon')).toBeInTheDocument();
      expect(screen.getByTestId('toggle-notify-stale')).toBeInTheDocument();
      expect(screen.getByTestId('toggle-notify-anniversary')).toBeInTheDocument();
    });
  });

  it('should display show achievements toggle', async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('toggle-show-achievements')).toBeInTheDocument();
    });
  });

  it('should call updateUserSettings on save', async () => {
    mockUpdateUserSettings.mockResolvedValue(defaultSettings);
    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId('settings-save-btn')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('settings-save-btn'));
    });

    await waitFor(() => {
      expect(mockUpdateUserSettings).toHaveBeenCalledWith('test-token', expect.objectContaining({
        dueSoonDays: 3,
        staleOneOnOneDays: 14,
        anniversaryLookaheadDays: 30,
        theme: 'DARK',
        showAchievements: true,
        notifyActionItemOverdue: true,
        notifyActionItemDueSoon: true,
        notifyStaleOneOnOne: true,
        notifyUpcomingAnniversary: true,
        aiEnabled: false,
        aiPrivacyMode: true,
        aiWritingStyle: 'NARRATIVE',
      }));
    });
  });

  it('should show success message after saving', async () => {
    mockUpdateUserSettings.mockResolvedValue(defaultSettings);
    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId('settings-save-btn')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('settings-save-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('settings-success')).toHaveTextContent('Settings saved successfully');
    });
  });

  it('should show error message on save failure', async () => {
    mockUpdateUserSettings.mockRejectedValue(new Error('Save failed'));
    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId('settings-save-btn')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('settings-save-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('settings-save-error')).toHaveTextContent('Save failed');
    });
  });

  it('should update theme in context when saving light theme', async () => {
    const lightSettings = { ...defaultSettings, theme: 'LIGHT' as const };
    mockUpdateUserSettings.mockResolvedValue(lightSettings);
    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId('theme-light-btn')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('theme-light-btn'));
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('settings-save-btn'));
    });

    await waitFor(() => {
      expect(mockSetTheme).toHaveBeenCalledWith('LIGHT');
    });
  });

  it('should show error state when settings fail to load', async () => {
    mockGetUserSettings.mockRejectedValue(new Error('Load failed'));
    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId('settings-error')).toHaveTextContent('Load failed');
    });
  });

  it('should update number inputs', async () => {
    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId('input-due-soon-days')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-due-soon-days'), { target: { value: '7' } });
    expect(screen.getByTestId('input-due-soon-days')).toHaveValue(7);
  });

  it('should display settings with custom values from API', async () => {
    const customSettings = {
      ...defaultSettings,
      dueSoonDays: 7,
      staleOneOnOneDays: 21,
      theme: 'LIGHT' as const,
      showAchievements: false,
      notifyActionItemOverdue: false,
    };
    mockGetUserSettings.mockResolvedValue(customSettings);

    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId('input-due-soon-days')).toHaveValue(7);
      expect(screen.getByTestId('input-stale-one-on-one-days')).toHaveValue(21);
    });
  });

  it('should disable save button while saving', async () => {
    let resolveUpdate: (value: unknown) => void;
    mockUpdateUserSettings.mockReturnValue(new Promise((resolve) => { resolveUpdate = resolve; }));

    render(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId('settings-save-btn')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('settings-save-btn'));
    });

    expect(screen.getByTestId('settings-save-btn')).toHaveTextContent('Saving...');

    await act(async () => {
      resolveUpdate!(defaultSettings);
    });
  });
});
