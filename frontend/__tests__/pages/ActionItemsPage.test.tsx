import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ActionItemsPage from '@/app/action-items/page';

jest.mock('@/lib/api-client', () => ({
  listAllActionItems: jest.fn(),
  completeActionItemById: jest.fn(),
  cancelActionItemById: jest.fn(),
  deleteActionItemById: jest.fn(),
}));

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: jest.fn(),
}));

import {
  listAllActionItems,
  completeActionItemById,
  cancelActionItemById,
  deleteActionItemById,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import { ActionItem } from '@/types/action-item';

const mockList = listAllActionItems as jest.MockedFunction<typeof listAllActionItems>;
const mockComplete = completeActionItemById as jest.MockedFunction<typeof completeActionItemById>;
const mockCancel = cancelActionItemById as jest.MockedFunction<typeof cancelActionItemById>;
const mockDelete = deleteActionItemById as jest.MockedFunction<typeof deleteActionItemById>;
const mockUseStableToken = useStableToken as jest.MockedFunction<typeof useStableToken>;

function item(partial: Partial<ActionItem> & { id: string; title: string }): ActionItem {
  return {
    personId: null,
    description: null,
    ownerType: 'MANAGER',
    dueDate: null,
    status: 'OPEN',
    originatingEntryId: null,
    originatingQuickNoteId: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...partial,
  };
}

function paginated(content: ActionItem[]) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 };
}

describe('ActionItemsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseStableToken.mockReturnValue({
      getToken: () => 'test-token',
      isAuthenticated: true,
      status: 'authenticated',
    });
    mockList.mockResolvedValue(paginated([]));
  });

  it('shows the unauthenticated message when not signed in', () => {
    mockUseStableToken.mockReturnValue({
      getToken: () => null,
      isAuthenticated: false,
      status: 'unauthenticated',
    });
    render(<ActionItemsPage />);
    expect(screen.getByTestId('unauthenticated')).toBeInTheDocument();
  });

  it('renders the page title and fetches all action items', async () => {
    render(<ActionItemsPage />);
    await waitFor(() => {
      expect(screen.getByText('Action Items')).toBeInTheDocument();
      expect(mockList).toHaveBeenCalledWith('test-token', { status: undefined, page: 0, size: 20 });
    });
  });

  it('renders a person-less (converted-note) TODO', async () => {
    mockList.mockResolvedValue(
      paginated([item({ id: 'a1', title: 'Prepare the budget deck', personId: null, dueDate: '2026-07-01' })])
    );
    render(<ActionItemsPage />);
    expect(await screen.findByText('Prepare the budget deck')).toBeInTheDocument();
    expect(screen.getByTestId('action-item-card')).toBeInTheDocument();
  });

  it('shows the empty state when there are no action items', async () => {
    render(<ActionItemsPage />);
    expect(await screen.findByTestId('empty-state')).toBeInTheDocument();
  });

  it('filters by status', async () => {
    render(<ActionItemsPage />);
    await waitFor(() => expect(screen.getByTestId('filter-DONE')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('filter-DONE'));
    await waitFor(() =>
      expect(mockList).toHaveBeenCalledWith('test-token', { status: 'DONE', page: 0, size: 20 })
    );
  });

  it('completes a person-less action item via the by-id endpoint', async () => {
    mockList.mockResolvedValue(paginated([item({ id: 'a1', title: 'Call vendor' })]));
    mockComplete.mockResolvedValue(item({ id: 'a1', title: 'Call vendor', status: 'DONE' }));
    render(<ActionItemsPage />);
    await screen.findByText('Call vendor');

    fireEvent.click(screen.getByTestId('action-item-complete-btn'));

    await waitFor(() => expect(mockComplete).toHaveBeenCalledWith('test-token', 'a1'));
  });

  it('cancels a person-less action item via the by-id endpoint', async () => {
    mockList.mockResolvedValue(paginated([item({ id: 'a1', title: 'Call vendor' })]));
    mockCancel.mockResolvedValue(item({ id: 'a1', title: 'Call vendor', status: 'CANCELED' }));
    render(<ActionItemsPage />);
    await screen.findByText('Call vendor');

    fireEvent.click(screen.getByTestId('action-item-cancel-btn'));

    await waitFor(() => expect(mockCancel).toHaveBeenCalledWith('test-token', 'a1'));
  });

  it('deletes a person-less action item via the by-id endpoint', async () => {
    mockList.mockResolvedValue(paginated([item({ id: 'a1', title: 'Call vendor' })]));
    mockDelete.mockResolvedValue(undefined);
    render(<ActionItemsPage />);
    await screen.findByText('Call vendor');

    fireEvent.click(screen.getByTestId('action-item-delete-btn'));

    await waitFor(() => expect(mockDelete).toHaveBeenCalledWith('test-token', 'a1'));
  });

  it('shows an error when the fetch fails', async () => {
    mockList.mockRejectedValue(new Error('Network error'));
    render(<ActionItemsPage />);
    await waitFor(() => expect(screen.getByTestId('error-message')).toHaveTextContent('Network error'));
  });
});
