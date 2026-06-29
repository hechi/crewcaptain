import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import TriagePage from '@/app/triage/page';

// Mock next-auth
jest.mock('next-auth/react', () => ({
  useSession: () => ({
    data: { accessToken: 'test-token', user: { name: 'Test User' } },
    status: 'authenticated',
  }),
  signOut: jest.fn(),
}));

// Mock useStableToken
const stableGetToken = () => 'test-token';
jest.mock('@/lib/useStableToken', () => ({
  useStableToken: () => ({
    getToken: stableGetToken,
    isAuthenticated: true,
    status: 'authenticated',
  }),
}));

// Mock api-client
const mockGetTriageQueue = jest.fn();
const mockCompleteActionItem = jest.fn();
const mockSnoozeTriageItem = jest.fn();
const mockGetUserSettings = jest.fn();
const mockGetTriageHint = jest.fn();

jest.mock('@/lib/api-client', () => ({
  getTriageQueue: (...args: unknown[]) => mockGetTriageQueue(...args),
  completeActionItem: (...args: unknown[]) => mockCompleteActionItem(...args),
  cancelActionItem: jest.fn().mockResolvedValue({}),
  updateActionItem: jest.fn().mockResolvedValue({}),
  createQuickNote: jest.fn().mockResolvedValue({}),
  snoozeTriageItem: (...args: unknown[]) => mockSnoozeTriageItem(...args),
  getUserSettings: (...args: unknown[]) => mockGetUserSettings(...args),
  getTriageHint: (...args: unknown[]) => mockGetTriageHint(...args),
  getPerson: jest.fn().mockResolvedValue({ id: 'p1', name: 'Alice', moraleStatus: 'GREEN' }),
  listOneOnOneEntries: jest.fn().mockResolvedValue({ content: [] }),
  listActionItemsByPerson: jest.fn().mockResolvedValue({ content: [] }),
  listKudosByPerson: jest.fn().mockResolvedValue({ content: [] }),
}));

describe('TriagePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetUserSettings.mockResolvedValue({ aiEnabled: false, aiAvailable: false });
  });

  it('renders loading state initially', () => {
    mockGetTriageQueue.mockReturnValue(new Promise(() => {})); // never resolves
    render(<TriagePage />);
    expect(screen.getByText('Loading triage queue')).toBeInTheDocument();
  });

  it('renders empty state when no items', async () => {
    mockGetTriageQueue.mockResolvedValue({ items: [], totalCount: 0 });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-empty-state')).toBeInTheDocument();
    });
    expect(screen.getByText("You're all clear")).toBeInTheDocument();
  });

  it('renders triage items', async () => {
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-123',
          type: 'ACTION_ITEM_OVERDUE',
          criticality: 'OVERDUE',
          title: 'Follow up on project',
          personId: 'person-1',
          personName: 'Alice',
          workspaceId: null,
          workspaceName: null,
          sensitive: false,
          dueDate: '2026-05-01',
          daysOverdue: 5,
          daysUntilDue: null,
          ownerType: 'MANAGER',
          sourceActionItemId: 'action-1',
          snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-list')).toBeInTheDocument();
    });
    expect(screen.getByText('Follow up on project')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('5d overdue')).toBeInTheDocument();
  });

  it('renders error state', async () => {
    mockGetTriageQueue.mockRejectedValue(new Error('Network error'));
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-error')).toBeInTheDocument();
    });
    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('renders title with item count', async () => {
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-1', type: 'ACTION_ITEM_OVERDUE', criticality: 'OVERDUE',
          title: 'Task 1', personId: 'p1', personName: 'Alice',
          workspaceId: null, workspaceName: null, sensitive: false,
          dueDate: null, daysOverdue: 1, daysUntilDue: null,
          ownerType: null, sourceActionItemId: null, snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-title')).toBeInTheDocument();
    });
    expect(screen.getByText(/1 item/)).toBeInTheDocument();
  });

  it('shows filter bar', async () => {
    mockGetTriageQueue.mockResolvedValue({ items: [], totalCount: 0 });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-filter-bar')).toBeInTheDocument();
    });
  });

  it('calls API with MINE scope when filter changed', async () => {
    mockGetTriageQueue.mockResolvedValue({ items: [], totalCount: 0 });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('scope-mine')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('scope-mine'));
    });

    await waitFor(() => {
      expect(mockGetTriageQueue).toHaveBeenCalledWith('test-token', expect.objectContaining({ scope: 'MINE' }));
    });
  });

  it('renders sensitive items with lock icon instead of title', async () => {
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-sensitive', type: 'ACTION_ITEM_OVERDUE', criticality: 'OVERDUE',
          title: 'Secret task', personId: 'p1', personName: 'Alice',
          workspaceId: null, workspaceName: null, sensitive: true,
          dueDate: null, daysOverdue: 1, daysUntilDue: null,
          ownerType: null, sourceActionItemId: null, snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByText('[Sensitive]')).toBeInTheDocument();
    });
    expect(screen.queryByText('Secret task')).not.toBeInTheDocument();
  });

  it('renders Open People link in empty state', async () => {
    mockGetTriageQueue.mockResolvedValue({ items: [], totalCount: 0 });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByText('Open People')).toBeInTheDocument();
    });
    expect(screen.getByText('Open People').closest('a')).toHaveAttribute('href', '/people');
  });

  it('shows AI hint button when AI is enabled and item is not sensitive', async () => {
    mockGetUserSettings.mockResolvedValue({ aiEnabled: true, aiAvailable: true });
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-1', type: 'ACTION_ITEM_OVERDUE', criticality: 'OVERDUE',
          title: 'Task', personId: 'p1', personName: 'Alice',
          workspaceId: null, workspaceName: null, sensitive: false,
          dueDate: null, daysOverdue: 1, daysUntilDue: null,
          ownerType: 'MANAGER', sourceActionItemId: 'action-1', snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-hint-btn')).toBeInTheDocument();
    });
  });

  it('shows AI hint button when AI is available via admin defaults (aiEnabled false)', async () => {
    // Admin provided team-wide defaults: user has not enabled their own AI,
    // but AI is effectively available. Features must still be visible.
    mockGetUserSettings.mockResolvedValue({ aiEnabled: false, aiAvailable: true });
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-1', type: 'ACTION_ITEM_OVERDUE', criticality: 'OVERDUE',
          title: 'Task', personId: 'p1', personName: 'Alice',
          workspaceId: null, workspaceName: null, sensitive: false,
          dueDate: null, daysOverdue: 1, daysUntilDue: null,
          ownerType: 'MANAGER', sourceActionItemId: 'action-1', snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-hint-btn')).toBeInTheDocument();
    });
  });

  it('does not show AI hint button when item is sensitive', async () => {
    mockGetUserSettings.mockResolvedValue({ aiEnabled: true, aiAvailable: true });
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-sensitive', type: 'ACTION_ITEM_OVERDUE', criticality: 'OVERDUE',
          title: 'Secret', personId: 'p1', personName: 'Alice',
          workspaceId: null, workspaceName: null, sensitive: true,
          dueDate: null, daysOverdue: 1, daysUntilDue: null,
          ownerType: null, sourceActionItemId: null, snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-list')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('triage-hint-btn')).not.toBeInTheDocument();
  });

  it('does not show AI hint button when AI is disabled', async () => {
    mockGetUserSettings.mockResolvedValue({ aiEnabled: false, aiAvailable: false });
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-1', type: 'ACTION_ITEM_OVERDUE', criticality: 'OVERDUE',
          title: 'Task', personId: 'p1', personName: 'Alice',
          workspaceId: null, workspaceName: null, sensitive: false,
          dueDate: null, daysOverdue: 1, daysUntilDue: null,
          ownerType: null, sourceActionItemId: null, snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-list')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('triage-hint-btn')).not.toBeInTheDocument();
  });

  it('shows hint pill after clicking hint button', async () => {
    mockGetUserSettings.mockResolvedValue({ aiEnabled: true, aiAvailable: true });
    mockGetTriageHint.mockResolvedValue({ hint: 'Set due to Friday', error: null });
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-1', type: 'ACTION_ITEM_OVERDUE', criticality: 'OVERDUE',
          title: 'Task', personId: 'p1', personName: 'Alice',
          workspaceId: null, workspaceName: null, sensitive: false,
          dueDate: null, daysOverdue: 1, daysUntilDue: null,
          ownerType: 'MANAGER', sourceActionItemId: 'action-1', snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-hint-btn')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('triage-hint-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('triage-hint-pill')).toBeInTheDocument();
    });
    expect(screen.getByText(/Set due to Friday/)).toBeInTheDocument();
  });

  it('shows inline action menu with all buttons when item is selected', async () => {
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-1', type: 'ACTION_ITEM_OVERDUE', criticality: 'OVERDUE',
          title: 'Task', personId: 'p1', personName: 'Alice',
          workspaceId: null, workspaceName: null, sensitive: false,
          dueDate: null, daysOverdue: 1, daysUntilDue: null,
          ownerType: 'MANAGER', sourceActionItemId: 'action-1', snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-item-actions')).toBeInTheDocument();
    });
    // Verify all action buttons present
    expect(screen.getByLabelText('Mark Done')).toBeInTheDocument();
    expect(screen.getByLabelText('Cancel')).toBeInTheDocument();
    expect(screen.getByLabelText('Snooze')).toBeInTheDocument();
    expect(screen.getByLabelText('Reassign Owner')).toBeInTheDocument();
    expect(screen.getByLabelText('Set Due Date')).toBeInTheDocument();
    expect(screen.getByLabelText('Add to next 1:1')).toBeInTheDocument();
    expect(screen.getByLabelText('Save as Quick Note')).toBeInTheDocument();
  });

  it('shows snooze submenu with 1d, 3d, 7d options', async () => {
    mockGetTriageQueue.mockResolvedValue({
      items: [
        {
          id: 'ai-1', type: 'ACTION_ITEM_OVERDUE', criticality: 'OVERDUE',
          title: 'Task', personId: 'p1', personName: 'Alice',
          workspaceId: null, workspaceName: null, sensitive: false,
          dueDate: null, daysOverdue: 1, daysUntilDue: null,
          ownerType: 'MANAGER', sourceActionItemId: 'action-1', snoozedUntil: null,
          createdAt: '2026-05-01T00:00:00Z',
        },
      ],
      totalCount: 1,
    });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByTestId('triage-snooze-btn')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('triage-snooze-btn'));
    });

    expect(screen.getByTestId('triage-snooze-menu')).toBeInTheDocument();
    expect(screen.getByText('1d')).toBeInTheDocument();
    expect(screen.getByText('3d')).toBeInTheDocument();
    expect(screen.getByText('7d')).toBeInTheDocument();
  });

  it('shows keyboard shortcut help in subtitle', async () => {
    mockGetTriageQueue.mockResolvedValue({ items: [], totalCount: 0 });
    await act(async () => { render(<TriagePage />); });

    await waitFor(() => {
      expect(screen.getByText(/Enter peek/)).toBeInTheDocument();
    });
  });
});
