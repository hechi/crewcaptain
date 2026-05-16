import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import QuickNoteOverlay from '@/components/quick-notes/QuickNoteOverlay';
import { createQuickNote } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

jest.mock('@/lib/api-client', () => ({
  createQuickNote: jest.fn(),
}));

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: jest.fn(),
}));

const mockCreateQuickNote = createQuickNote as jest.MockedFunction<typeof createQuickNote>;
const mockUseStableToken = useStableToken as jest.MockedFunction<typeof useStableToken>;

describe('QuickNoteOverlay', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    mockUseStableToken.mockReturnValue({
      getToken: () => 'test-token',
      isAuthenticated: true,
      status: 'authenticated',
    });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should render the floating action button when authenticated', () => {
    render(<QuickNoteOverlay />);
    expect(screen.getByTestId('quick-note-fab')).toBeInTheDocument();
  });

  it('should not render anything when not authenticated', () => {
    mockUseStableToken.mockReturnValue({
      getToken: () => null,
      isAuthenticated: false,
      status: 'unauthenticated',
    });
    const { container } = render(<QuickNoteOverlay />);
    expect(container.innerHTML).toBe('');
  });

  it('should open overlay when FAB is clicked', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));
    expect(screen.getByTestId('quick-note-overlay')).toBeInTheDocument();
  });

  it('should open overlay on Ctrl+Shift+Q', () => {
    render(<QuickNoteOverlay />);
    fireEvent.keyDown(document, { key: 'Q', ctrlKey: true, shiftKey: true });
    expect(screen.getByTestId('quick-note-overlay')).toBeInTheDocument();
  });

  it('should close overlay on Escape', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));
    expect(screen.getByTestId('quick-note-overlay')).toBeInTheDocument();

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByTestId('quick-note-overlay')).not.toBeInTheDocument();
  });

  it('should close overlay when backdrop is clicked', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));
    expect(screen.getByTestId('quick-note-overlay')).toBeInTheDocument();

    // Click the overlay container (backdrop area)
    fireEvent.click(screen.getByTestId('quick-note-overlay'));
    expect(screen.queryByTestId('quick-note-overlay')).not.toBeInTheDocument();
  });

  it('should close overlay when cancel button is clicked', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));
    expect(screen.getByTestId('quick-note-overlay')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('quick-note-overlay-cancel'));
    expect(screen.queryByTestId('quick-note-overlay')).not.toBeInTheDocument();
  });

  it('should toggle overlay with Ctrl+Shift+Q', () => {
    render(<QuickNoteOverlay />);
    fireEvent.keyDown(document, { key: 'Q', ctrlKey: true, shiftKey: true });
    expect(screen.getByTestId('quick-note-overlay')).toBeInTheDocument();

    fireEvent.keyDown(document, { key: 'Q', ctrlKey: true, shiftKey: true });
    expect(screen.queryByTestId('quick-note-overlay')).not.toBeInTheDocument();
  });

  it('should have proper accessibility attributes', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    const overlay = screen.getByTestId('quick-note-overlay');
    expect(overlay).toHaveAttribute('role', 'dialog');
    expect(overlay).toHaveAttribute('aria-modal', 'true');
    expect(overlay).toHaveAttribute('aria-labelledby', 'quick-note-overlay-title');
  });

  it('should render textarea and submit button when open', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    expect(screen.getByTestId('quick-note-overlay-input')).toBeInTheDocument();
    expect(screen.getByTestId('quick-note-overlay-submit')).toBeInTheDocument();
  });

  it('should disable submit button when text is empty', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    expect(screen.getByTestId('quick-note-overlay-submit')).toBeDisabled();
  });

  it('should enable submit button when text is entered', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: 'Test note' },
    });

    expect(screen.getByTestId('quick-note-overlay-submit')).not.toBeDisabled();
  });

  it('should call createQuickNote on submit', async () => {
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

    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: 'Test note' },
    });

    fireEvent.click(screen.getByTestId('quick-note-overlay-submit'));

    await waitFor(() => {
      expect(mockCreateQuickNote).toHaveBeenCalledWith('test-token', { text: 'Test note' });
    });
  });

  it('should show success message after saving', async () => {
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

    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: 'Test note' },
    });

    fireEvent.click(screen.getByTestId('quick-note-overlay-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('quick-note-overlay-success')).toBeInTheDocument();
    });
  });

  it('should auto-close after successful save', async () => {
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

    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: 'Test note' },
    });

    fireEvent.click(screen.getByTestId('quick-note-overlay-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('quick-note-overlay-success')).toBeInTheDocument();
    });

    act(() => {
      jest.advanceTimersByTime(900);
    });

    expect(screen.queryByTestId('quick-note-overlay')).not.toBeInTheDocument();
  });

  it('should show error message on API failure', async () => {
    mockCreateQuickNote.mockRejectedValue(new Error('Network error'));

    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: 'Test note' },
    });

    fireEvent.click(screen.getByTestId('quick-note-overlay-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('quick-note-overlay-error')).toBeInTheDocument();
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  it('should not close on error — allow retry', async () => {
    mockCreateQuickNote.mockRejectedValue(new Error('Network error'));

    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: 'Test note' },
    });

    fireEvent.click(screen.getByTestId('quick-note-overlay-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('quick-note-overlay-error')).toBeInTheDocument();
    });

    // Overlay should still be open
    expect(screen.getByTestId('quick-note-overlay')).toBeInTheDocument();
    // Text should still be there for retry
    expect(screen.getByTestId('quick-note-overlay-input')).toHaveValue('Test note');
  });

  it('should trim whitespace from note text before submitting', async () => {
    mockCreateQuickNote.mockResolvedValue({
      id: '1',
      text: 'Trimmed note',
      personId: null,
      sensitive: false,
      selfAssigned: false,
      status: 'INBOX',
      attachedEntryId: null,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });

    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: '  Trimmed note  ' },
    });

    fireEvent.click(screen.getByTestId('quick-note-overlay-submit'));

    await waitFor(() => {
      expect(mockCreateQuickNote).toHaveBeenCalledWith('test-token', { text: 'Trimmed note' });
    });
  });

  it('should not submit when text is only whitespace', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: '   ' },
    });

    expect(screen.getByTestId('quick-note-overlay-submit')).toBeDisabled();
  });

  it('should show keyboard shortcut hint in the overlay', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    expect(screen.getByText('Ctrl+Shift+Q')).toBeInTheDocument();
    expect(screen.getByText('Ctrl+Enter to save')).toBeInTheDocument();
  });

  it('should have aria-label on the FAB', () => {
    render(<QuickNoteOverlay />);
    const fab = screen.getByTestId('quick-note-fab');
    expect(fab).toHaveAttribute('aria-label', 'Quick note (Ctrl+Shift+Q)');
  });

  it('should reset text when reopened after close', () => {
    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: 'Some text' },
    });

    fireEvent.click(screen.getByTestId('quick-note-overlay-cancel'));

    // Reopen
    fireEvent.click(screen.getByTestId('quick-note-fab'));
    expect(screen.getByTestId('quick-note-overlay-input')).toHaveValue('');
  });

  it('should submit on Ctrl+Enter', async () => {
    mockCreateQuickNote.mockResolvedValue({
      id: '1',
      text: 'Keyboard submit',
      personId: null,
      sensitive: false,
      selfAssigned: false,
      status: 'INBOX',
      attachedEntryId: null,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });

    render(<QuickNoteOverlay />);
    fireEvent.click(screen.getByTestId('quick-note-fab'));

    fireEvent.change(screen.getByTestId('quick-note-overlay-input'), {
      target: { value: 'Keyboard submit' },
    });

    fireEvent.keyDown(screen.getByTestId('quick-note-overlay-input'), {
      key: 'Enter',
      ctrlKey: true,
    });

    await waitFor(() => {
      expect(mockCreateQuickNote).toHaveBeenCalledWith('test-token', { text: 'Keyboard submit' });
    });
  });

  it('should not open overlay when not authenticated', () => {
    mockUseStableToken.mockReturnValue({
      getToken: () => null,
      isAuthenticated: false,
      status: 'unauthenticated',
    });

    render(<QuickNoteOverlay />);
    // FAB should not be rendered
    expect(screen.queryByTestId('quick-note-fab')).not.toBeInTheDocument();
    // Keyboard shortcut should not open overlay
    fireEvent.keyDown(document, { key: 'Q', ctrlKey: true, shiftKey: true });
    expect(screen.queryByTestId('quick-note-overlay')).not.toBeInTheDocument();
  });
});
