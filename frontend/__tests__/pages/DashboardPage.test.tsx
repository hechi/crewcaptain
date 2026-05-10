import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import DashboardPage from '@/app/dashboard/page';

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  getDashboard: jest.fn(),
  getGamificationStats: jest.fn(),
  getUserSettings: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { getDashboard, getGamificationStats, getUserSettings } from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockGetDashboard = getDashboard as jest.MockedFunction<typeof getDashboard>;
const mockGetGamificationStats = getGamificationStats as jest.MockedFunction<typeof getGamificationStats>;
const mockGetUserSettings = getUserSettings as jest.MockedFunction<typeof getUserSettings>;

const mockDashboardData = {
  overdueActionItems: [
    {
      id: '1',
      personId: 'person-1',
      personName: 'Alice Smith',
      title: 'Review PR',
      dueDate: '2026-05-07',
      ownerType: 'PERSON' as const,
    },
  ],
  dueSoonActionItems: [
    {
      id: '2',
      personId: 'person-2',
      personName: 'Bob Jones',
      title: 'Submit report',
      dueDate: '2026-05-12',
      ownerType: 'MANAGER' as const,
    },
  ],
  staleOneOnOnes: [
    {
      personId: 'person-3',
      personName: 'Charlie Brown',
      cadenceType: 'WEEKLY' as const,
      customIntervalDays: null,
      lastMeetingDate: '2026-04-25T10:00:00Z',
      daysSinceLastMeeting: 15,
      expectedIntervalDays: 7,
    },
  ],
  upcomingAnniversaries: [
    {
      personId: 'person-4',
      personName: 'Diana Prince',
      startDate: '2023-05-15',
      anniversaryDate: '2026-05-15',
      yearsCompleted: 3,
      daysUntil: 5,
    },
  ],
};

const mockGamificationData = {
  streaks: { currentStreak: 3, longestStreak: 5, totalOneOnOnesHeld: 15 },
  achievements: [
    { type: 'FIRST_ONE_ON_ONE' as const, unlocked: true, label: 'First 1:1', description: 'Hold your first 1:1 meeting', current: 15, target: 1 },
    { type: 'TEN_ONE_ON_ONES' as const, unlocked: true, label: '10 1:1s', description: 'Hold 10 one-on-one meetings', current: 15, target: 10 },
    { type: 'FIFTY_ONE_ON_ONES' as const, unlocked: false, label: '50 1:1s', description: 'Hold 50 one-on-one meetings', current: 15, target: 50 },
  ],
  activityHeatmap: [
    { date: '2026-05-09', count: 1 },
    { date: '2026-05-10', count: 0 },
  ],
  pdpProgress: { totalActive: 2, totalAchieved: 1, totalPaused: 0, totalDropped: 0, completionPercentage: 33 },
};

describe('DashboardPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetGamificationStats.mockResolvedValue(mockGamificationData);
    mockGetUserSettings.mockResolvedValue({
      dueSoonDays: 3,
      staleOneOnOneDays: 14,
      anniversaryLookaheadDays: 30,
      theme: 'DARK',
      showAchievements: true,
      notifyActionItemOverdue: true,
      notifyActionItemDueSoon: true,
      notifyStaleOneOnOne: true,
      notifyUpcomingAnniversary: true,
    });
  });

  it('should show loading state initially', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockReturnValue(new Promise(() => {})); // Never resolves
    mockGetGamificationStats.mockReturnValue(new Promise(() => {})); // Never resolves
    mockGetUserSettings.mockReturnValue(new Promise(() => {})); // Never resolves

    render(<DashboardPage />);
    expect(screen.getByTestId('dashboard-loading')).toBeInTheDocument();
  });

  it('should show loading state when session is loading', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'loading',
      update: jest.fn(),
    });

    render(<DashboardPage />);
    expect(screen.getByTestId('dashboard-loading')).toBeInTheDocument();
  });

  it('should render dashboard with all sections when data loads', async () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockResolvedValue(mockDashboardData);

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
    });

    expect(screen.getByTestId('dashboard-title')).toHaveTextContent('Dashboard');
    expect(screen.getByTestId('dashboard-section-overdue')).toBeInTheDocument();
    expect(screen.getByTestId('dashboard-section-due-soon')).toBeInTheDocument();
    expect(screen.getByTestId('dashboard-section-stale')).toBeInTheDocument();
    expect(screen.getByTestId('dashboard-section-anniversaries')).toBeInTheDocument();
  });

  it('should display alert summary with correct count', async () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockResolvedValue(mockDashboardData);

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByTestId('dashboard-alert-summary')).toBeInTheDocument();
    });

    // 1 overdue + 1 stale = 2 items need attention
    expect(screen.getByTestId('dashboard-alert-summary')).toHaveTextContent('2 items need your attention');
  });

  it('should show error state when API call fails', async () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockRejectedValue(new Error('Network error'));

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByTestId('dashboard-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('should render empty dashboard when no data', async () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockResolvedValue({
      overdueActionItems: [],
      dueSoonActionItems: [],
      staleOneOnOnes: [],
      upcomingAnniversaries: [],
    });

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
    });

    expect(screen.getByTestId('overdue-items-empty')).toBeInTheDocument();
    expect(screen.getByTestId('due-soon-items-empty')).toBeInTheDocument();
    expect(screen.getByTestId('stale-1on1s-empty')).toBeInTheDocument();
    expect(screen.getByTestId('anniversaries-empty')).toBeInTheDocument();
  });

  it('should not show alert summary when no alerts', async () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockResolvedValue({
      overdueActionItems: [],
      dueSoonActionItems: [],
      staleOneOnOnes: [],
      upcomingAnniversaries: [],
    });

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
    });

    expect(screen.queryByTestId('dashboard-alert-summary')).not.toBeInTheDocument();
  });

  it('should call getDashboard with access token', async () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'my-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockResolvedValue({
      overdueActionItems: [],
      dueSoonActionItems: [],
      staleOneOnOnes: [],
      upcomingAnniversaries: [],
    });

    render(<DashboardPage />);

    await waitFor(() => {
      expect(mockGetDashboard).toHaveBeenCalledWith('my-token');
    });
  });

  it('should display overdue action items in the overdue section', async () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockResolvedValue(mockDashboardData);

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Review PR')).toBeInTheDocument();
    });

    expect(screen.getByText('Alice Smith')).toBeInTheDocument();
  });

  it('should display stale 1:1 reminders', async () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockResolvedValue(mockDashboardData);

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Charlie Brown')).toBeInTheDocument();
    });

    expect(screen.getByText('15 days since last 1:1')).toBeInTheDocument();
  });

  it('should display upcoming anniversaries', async () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockGetDashboard.mockResolvedValue(mockDashboardData);

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Diana Prince')).toBeInTheDocument();
    });

    expect(screen.getByText('3 years')).toBeInTheDocument();
  });
});
