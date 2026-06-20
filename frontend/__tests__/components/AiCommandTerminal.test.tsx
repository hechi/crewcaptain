import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import AiCommandTerminal from '@/components/ai-terminal/AiCommandTerminal';
import { parseAiCommand, getPersonDirectory, getUserSettings, createQuickNote, createActionItem, createKudos, createOneOnOneEntry } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

jest.mock('@/lib/api-client', () => ({
  parseAiCommand: jest.fn(),
  getPersonDirectory: jest.fn(),
  getUserSettings: jest.fn(),
  createQuickNote: jest.fn(),
  createActionItem: jest.fn(),
  createKudos: jest.fn(),
  createOneOnOneEntry: jest.fn(),
}));

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: jest.fn(),
}));

// Mock crypto.randomUUID
let uuidCounter = 0;
Object.defineProperty(global, 'crypto', {
  value: {
    randomUUID: () => `test-uuid-${++uuidCounter}`,
  },
});

// Mock scrollIntoView
Element.prototype.scrollIntoView = jest.fn();

const mockParseAiCommand = parseAiCommand as jest.MockedFunction<typeof parseAiCommand>;
const mockGetPersonDirectory = getPersonDirectory as jest.MockedFunction<typeof getPersonDirectory>;
const mockGetUserSettings = getUserSettings as jest.MockedFunction<typeof getUserSettings>;
const mockCreateQuickNote = createQuickNote as jest.MockedFunction<typeof createQuickNote>;
const mockCreateActionItem = createActionItem as jest.MockedFunction<typeof createActionItem>;
const mockCreateKudos = createKudos as jest.MockedFunction<typeof createKudos>;
const mockCreateOneOnOneEntry = createOneOnOneEntry as jest.MockedFunction<typeof createOneOnOneEntry>;
const mockUseStableToken = useStableToken as jest.MockedFunction<typeof useStableToken>;

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
  aiApiBaseUrl: 'http://localhost:11434/v1',
  aiModelName: 'llama3',
  aiPrivacyMode: false,
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
};

describe('AiCommandTerminal', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    uuidCounter = 0;
    mockUseStableToken.mockReturnValue({
      getToken: () => 'test-token',
      isAuthenticated: true,
      status: 'authenticated',
    });
    mockGetUserSettings.mockResolvedValue(aiEnabledSettings);
    mockGetPersonDirectory.mockResolvedValue([
      { id: 'person-1', preferredName: 'Alice' },
    ]);
  });

  it('should not render when not authenticated', async () => {
    mockUseStableToken.mockReturnValue({
      getToken: () => null,
      isAuthenticated: false,
      status: 'unauthenticated',
    });

    const { container } = render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(container.innerHTML).toBe('');
    });
  });

  it('should not render when AI is disabled', async () => {
    mockGetUserSettings.mockResolvedValue({ ...aiEnabledSettings, aiEnabled: false });

    const { container } = render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.queryByTestId('ai-terminal-fab')).not.toBeInTheDocument();
    });
  });

  it('should render FAB when AI is enabled', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });
  });

  it('should open overlay when FAB is clicked', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    expect(screen.getByTestId('ai-terminal-overlay')).toBeInTheDocument();
  });

  it('should open overlay on Ctrl+K', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.keyDown(document, { key: 'k', ctrlKey: true });
    expect(screen.getByTestId('ai-terminal-overlay')).toBeInTheDocument();
  });

  it('should close overlay on Escape', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    expect(screen.getByTestId('ai-terminal-overlay')).toBeInTheDocument();

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByTestId('ai-terminal-overlay')).not.toBeInTheDocument();
  });

  it('should close overlay when backdrop is clicked', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    expect(screen.getByTestId('ai-terminal-overlay')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('ai-terminal-backdrop'));
    expect(screen.queryByTestId('ai-terminal-overlay')).not.toBeInTheDocument();
  });

  it('should close overlay when X button is clicked', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    expect(screen.getByTestId('ai-terminal-overlay')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('ai-terminal-close'));
    expect(screen.queryByTestId('ai-terminal-overlay')).not.toBeInTheDocument();
  });

  it('should have proper accessibility attributes', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));

    const overlay = screen.getByTestId('ai-terminal-overlay');
    expect(overlay).toHaveAttribute('role', 'dialog');
    expect(overlay).toHaveAttribute('aria-modal', 'true');
    expect(overlay).toHaveAttribute('aria-labelledby', 'ai-terminal-title');
  });

  it('should show empty state when no messages', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    expect(screen.getByTestId('ai-terminal-empty')).toBeInTheDocument();
  });

  it('should disable submit button when input is empty', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    expect(screen.getByTestId('ai-terminal-submit')).toBeDisabled();
  });

  it('should enable submit button when input has text', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'Create a note' },
    });

    expect(screen.getByTestId('ai-terminal-submit')).not.toBeDisabled();
  });

  it('should send command and show preview card', async () => {
    mockParseAiCommand.mockResolvedValue({
      intent: 'create_quick_note',
      targetPersonId: null,
      content: 'Remember team offsite',
      dueDate: null,
      tags: [],
      sensitive: false,
      error: null,
    });

    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'Note: Remember team offsite' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    await waitFor(() => {
      expect(mockParseAiCommand).toHaveBeenCalledWith('test-token', 'Note: Remember team offsite');
    });

    await waitFor(() => {
      expect(screen.getByText(/Action: Quick Note/)).toBeInTheDocument();
      expect(screen.getByText(/Content: Remember team offsite/)).toBeInTheDocument();
      expect(screen.getByTestId('ai-terminal-confirm')).toBeInTheDocument();
    });
  });

  it('should show error message when AI returns error', async () => {
    mockParseAiCommand.mockResolvedValue({
      intent: null,
      targetPersonId: null,
      content: null,
      dueDate: null,
      tags: [],
      sensitive: false,
      error: 'AI not configured',
    });

    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'Do something' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    await waitFor(() => {
      expect(screen.getByText('AI not configured')).toBeInTheDocument();
    });
  });

  it('should execute quick note on confirm', async () => {
    mockParseAiCommand.mockResolvedValue({
      intent: 'create_quick_note',
      targetPersonId: null,
      content: 'Test note',
      dueDate: null,
      tags: [],
      sensitive: false,
      error: null,
    });
    mockCreateQuickNote.mockResolvedValue({
      id: '1',
      text: 'Test note',
      personId: null,
      sensitive: false,
      selfAssigned: false,
      status: 'INBOX',
      attachedEntryId: null,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });

    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'Take a note: Test note' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-confirm')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-confirm'));

    await waitFor(() => {
      expect(mockCreateQuickNote).toHaveBeenCalledWith('test-token', {
        text: 'Test note',
        personId: undefined,
        sensitive: false,
      });
    });

    await waitFor(() => {
      expect(screen.getByText(/Quick Note created/)).toBeInTheDocument();
    });
  });

  it('should auto-execute when setting is enabled', async () => {
    mockGetUserSettings.mockResolvedValue({ ...aiEnabledSettings, aiAutoExecuteCommands: true });
    mockParseAiCommand.mockResolvedValue({
      intent: 'create_quick_note',
      targetPersonId: null,
      content: 'Auto note',
      dueDate: null,
      tags: [],
      sensitive: false,
      error: null,
    });
    mockCreateQuickNote.mockResolvedValue({
      id: '1',
      text: 'Auto note',
      personId: null,
      sensitive: false,
      selfAssigned: false,
      status: 'INBOX',
      attachedEntryId: null,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });

    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'Note: Auto note' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    await waitFor(() => {
      expect(mockCreateQuickNote).toHaveBeenCalled();
    });

    // Should NOT show confirm button (auto-execute bypasses it)
    expect(screen.queryByTestId('ai-terminal-confirm')).not.toBeInTheDocument();

    // Should show undo toast
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-undo-toast')).toBeInTheDocument();
    });
  });

  it('should show privacy warning when privacy mode is on and content is sensitive', async () => {
    mockGetUserSettings.mockResolvedValue({ ...aiEnabledSettings, aiPrivacyMode: true });
    mockParseAiCommand.mockResolvedValue({
      intent: 'create_quick_note',
      targetPersonId: 'person-1',
      content: 'PIP discussion',
      dueDate: null,
      tags: [],
      sensitive: true,
      error: null,
    });

    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'Private note about Alice: PIP discussion' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    await waitFor(() => {
      expect(screen.getByText(/Privacy Mode is ON/)).toBeInTheDocument();
    });
  });

  it('should fetch person directory when opening', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));

    await waitFor(() => {
      expect(mockGetPersonDirectory).toHaveBeenCalledWith('test-token');
    });
  });

  it('should have aria-label on the FAB', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      const fab = screen.getByTestId('ai-terminal-fab');
      expect(fab).toHaveAttribute('aria-label', 'AI Command Terminal (Ctrl+K)');
    });
  });

  it('should show thinking indicator while processing', async () => {
    // Make parseAiCommand hang so we can see loading state
    let resolveCommand: (value: any) => void;
    const pendingPromise = new Promise((resolve) => { resolveCommand = resolve; });
    mockParseAiCommand.mockReturnValue(pendingPromise as any);

    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'Do something' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-thinking')).toBeInTheDocument();
      expect(screen.getByText('thinking...')).toBeInTheDocument();
    });

    // Resolve to clean up
    resolveCommand!({
      intent: null, targetPersonId: null, content: null,
      dueDate: null, meetingDate: null, tags: [], sensitive: false,
      error: 'test done',
    });

    await waitFor(() => {
      expect(screen.queryByTestId('ai-terminal-thinking')).not.toBeInTheDocument();
    });
  });

  it('should execute 1:1 entry creation on confirm', async () => {
    mockParseAiCommand.mockResolvedValue({
      intent: 'create_one_on_one_entry',
      targetPersonId: 'person-1',
      content: 'Discussed project timeline and blockers',
      dueDate: null,
      meetingDate: '2026-06-06',
      tags: ['project'],
      sensitive: false,
      error: null,
    });
    mockCreateOneOnOneEntry.mockResolvedValue({
      id: 'entry-1',
      personId: 'person-1',
      meetingDate: '2026-06-06T00:00:00Z',
      agendaItems: [],
      notesMarkdown: 'Discussed project timeline and blockers',
      outcomesMarkdown: null,
      sensitive: false,
      createdAt: '2026-06-06T00:00:00Z',
      updatedAt: '2026-06-06T00:00:00Z',
    });

    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'Had a chat with Alice about the project timeline' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    await waitFor(() => {
      expect(screen.getByText(/1:1 Entry/)).toBeInTheDocument();
      expect(screen.getByTestId('ai-terminal-confirm')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-confirm'));

    await waitFor(() => {
      expect(mockCreateOneOnOneEntry).toHaveBeenCalledWith('test-token', 'person-1', {
        meetingDate: '2026-06-06T00:00:00Z',
        notesMarkdown: 'Discussed project timeline and blockers',
        sensitive: false,
      });
    });

    await waitFor(() => {
      expect(screen.getByText(/1:1 Entry created/)).toBeInTheDocument();
    });
  });

  it('should show help message when user types "help"', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'help' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    // Should NOT call the AI API
    expect(mockParseAiCommand).not.toHaveBeenCalled();

    // Should show the help content
    await waitFor(() => {
      expect(screen.getByText(/Available Commands/)).toBeInTheDocument();
      expect(screen.getByText(/Action Item/)).toBeInTheDocument();
      expect(screen.getByText(/Kudos/)).toBeInTheDocument();
      expect(screen.getByText(/Quick Note/)).toBeInTheDocument();
      expect(screen.getByText(/1:1 Entry/)).toBeInTheDocument();
    });

    // Empty state should no longer be visible (messages exist now)
    expect(screen.queryByTestId('ai-terminal-empty')).not.toBeInTheDocument();
  });

  it('should show help message when user types "HELP" (case-insensitive)', async () => {
    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'HELP' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    expect(mockParseAiCommand).not.toHaveBeenCalled();

    await waitFor(() => {
      expect(screen.getByText(/Available Commands/)).toBeInTheDocument();
    });
  });

  it('should show error when 1:1 entry has no target person', async () => {
    mockParseAiCommand.mockResolvedValue({
      intent: 'create_one_on_one_entry',
      targetPersonId: null,
      content: 'Some meeting notes',
      dueDate: null,
      meetingDate: '2026-06-06',
      tags: [],
      sensitive: false,
      error: null,
    });

    render(<AiCommandTerminal />);
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-fab')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-fab'));
    fireEvent.change(screen.getByTestId('ai-terminal-input'), {
      target: { value: 'Had a meeting about stuff' },
    });
    fireEvent.click(screen.getByTestId('ai-terminal-submit'));

    // Preview shown, user confirms
    await waitFor(() => {
      expect(screen.getByTestId('ai-terminal-confirm')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-terminal-confirm'));

    await waitFor(() => {
      expect(screen.getByText(/1:1 entries require a target person/)).toBeInTheDocument();
    });
  });
});
