import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import OneOnOneActionItems from '@/components/one-on-one/OneOnOneActionItems';
import { ActionItem } from '@/types/action-item';

jest.mock('@/lib/api-client', () => ({
  listActionItemsByPerson: jest.fn(),
  createActionItem: jest.fn(),
  completeActionItem: jest.fn(),
}));

import {
  listActionItemsByPerson,
  createActionItem,
  completeActionItem,
} from '@/lib/api-client';

const mockListActionItemsByPerson = listActionItemsByPerson as jest.MockedFunction<typeof listActionItemsByPerson>;
const mockCreateActionItem = createActionItem as jest.MockedFunction<typeof createActionItem>;
const mockCompleteActionItem = completeActionItem as jest.MockedFunction<typeof completeActionItem>;

const openItem: ActionItem = {
  id: 'item-1',
  personId: 'person-1',
  title: 'Follow up on design review',
  description: null,
  ownerType: 'MANAGER',
  dueDate: '2026-06-01',
  status: 'OPEN',
  originatingEntryId: null,
  createdAt: '2026-05-10T10:00:00Z',
  updatedAt: '2026-05-10T10:00:00Z',
};

const sessionItem: ActionItem = {
  id: 'item-2',
  personId: 'person-1',
  title: 'Send meeting notes',
  description: null,
  ownerType: 'MANAGER',
  dueDate: '2026-05-20',
  status: 'OPEN',
  originatingEntryId: 'entry-1',
  createdAt: '2026-05-15T10:00:00Z',
  updatedAt: '2026-05-15T10:00:00Z',
};

const completedSessionItem: ActionItem = {
  ...sessionItem,
  id: 'item-3',
  title: 'Completed task from session',
  status: 'DONE',
};

const defaultProps = {
  token: 'test-token',
  personId: 'person-1',
  entryId: 'entry-1',
};

describe('OneOnOneActionItems', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state initially', () => {
    mockListActionItemsByPerson.mockReturnValue(new Promise(() => {}));
    render(<OneOnOneActionItems {...defaultProps} />);
    expect(screen.getByTestId('action-items-loading')).toBeInTheDocument();
  });

  it('should render the quick-add form', async () => {
    mockListActionItemsByPerson.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('quick-add-action-item-form')).toBeInTheDocument();
    });

    expect(screen.getByTestId('quick-action-title-input')).toBeInTheDocument();
    expect(screen.getByTestId('quick-action-due-date-input')).toBeInTheDocument();
    expect(screen.getByTestId('quick-add-submit-btn')).toBeInTheDocument();
  });

  it('should show empty state when no action items exist', async () => {
    mockListActionItemsByPerson.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('no-action-items')).toBeInTheDocument();
    });

    expect(screen.getByText(/No action items yet/)).toBeInTheDocument();
  });

  it('should display open action items for the person', async () => {
    mockListActionItemsByPerson.mockImplementation(async (_token, _personId, params) => {
      if (params?.status === 'OPEN') {
        return { content: [openItem], page: 0, size: 50, totalElements: 1, totalPages: 1 };
      }
      return { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
    });

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('other-open-action-items')).toBeInTheDocument();
    });

    expect(screen.getByText('Follow up on design review')).toBeInTheDocument();
  });

  it('should display session action items separately with highlight', async () => {
    mockListActionItemsByPerson.mockImplementation(async (_token, _personId, params) => {
      if (params?.originatingEntryId === 'entry-1') {
        return { content: [sessionItem], page: 0, size: 50, totalElements: 1, totalPages: 1 };
      }
      if (params?.status === 'OPEN') {
        return { content: [openItem, sessionItem], page: 0, size: 50, totalElements: 2, totalPages: 1 };
      }
      return { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
    });

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('session-action-items')).toBeInTheDocument();
    });

    expect(screen.getByText('From this session')).toBeInTheDocument();
    expect(screen.getByText('Send meeting notes')).toBeInTheDocument();
  });

  it('should display completed session items', async () => {
    mockListActionItemsByPerson.mockImplementation(async (_token, _personId, params) => {
      if (params?.originatingEntryId === 'entry-1') {
        return { content: [completedSessionItem], page: 0, size: 50, totalElements: 1, totalPages: 1 };
      }
      if (params?.status === 'OPEN') {
        return { content: [openItem], page: 0, size: 50, totalElements: 1, totalPages: 1 };
      }
      return { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
    });

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('completed-session-items')).toBeInTheDocument();
    });

    expect(screen.getByText('Completed task from session')).toBeInTheDocument();
  });

  it('should create action item with originatingEntryId on quick-add', async () => {
    mockListActionItemsByPerson.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    mockCreateActionItem.mockResolvedValue(sessionItem);

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('quick-add-action-item-form')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('quick-action-title-input'), { target: { value: 'New task' } });
    fireEvent.change(screen.getByTestId('quick-action-due-date-input'), { target: { value: '2026-06-15' } });
    fireEvent.click(screen.getByTestId('quick-add-submit-btn'));

    await waitFor(() => {
      expect(mockCreateActionItem).toHaveBeenCalledWith('test-token', 'person-1', {
        title: 'New task',
        dueDate: '2026-06-15',
        originatingEntryId: 'entry-1',
      });
    });
  });

  it('should show validation error when title is empty', async () => {
    mockListActionItemsByPerson.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('quick-add-action-item-form')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('quick-add-submit-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('quick-add-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Title is required')).toBeInTheDocument();
    expect(mockCreateActionItem).not.toHaveBeenCalled();
  });

  it('should clear form after successful creation', async () => {
    mockListActionItemsByPerson.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    mockCreateActionItem.mockResolvedValue(sessionItem);

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('quick-add-action-item-form')).toBeInTheDocument();
    });

    const titleInput = screen.getByTestId('quick-action-title-input') as HTMLInputElement;
    fireEvent.change(titleInput, { target: { value: 'New task' } });
    fireEvent.click(screen.getByTestId('quick-add-submit-btn'));

    await waitFor(() => {
      expect(titleInput.value).toBe('');
    });
  });

  it('should mark action item as done when checkbox is clicked', async () => {
    mockListActionItemsByPerson.mockImplementation(async (_token, _personId, params) => {
      if (params?.status === 'OPEN') {
        return { content: [openItem], page: 0, size: 50, totalElements: 1, totalPages: 1 };
      }
      return { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
    });
    mockCompleteActionItem.mockResolvedValue({ ...openItem, status: 'DONE' });

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Follow up on design review')).toBeInTheDocument();
    });

    const checkboxes = screen.getAllByTestId('action-item-complete-checkbox');
    fireEvent.click(checkboxes[0]);

    await waitFor(() => {
      expect(mockCompleteActionItem).toHaveBeenCalledWith('test-token', 'person-1', 'item-1');
    });
  });

  it('should show error when API call fails', async () => {
    mockListActionItemsByPerson.mockRejectedValue(new Error('Network error'));

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('action-items-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('should show due date on action item rows', async () => {
    mockListActionItemsByPerson.mockImplementation(async (_token, _personId, params) => {
      if (params?.status === 'OPEN') {
        return { content: [openItem], page: 0, size: 50, totalElements: 1, totalPages: 1 };
      }
      return { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
    });

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('other-open-action-items')).toBeInTheDocument();
    });

    expect(screen.getByTestId('action-item-row-due-date')).toBeInTheDocument();
  });

  it('should call listActionItemsByPerson with correct params', async () => {
    mockListActionItemsByPerson.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });

    render(<OneOnOneActionItems {...defaultProps} />);

    await waitFor(() => {
      expect(mockListActionItemsByPerson).toHaveBeenCalledWith('test-token', 'person-1', { status: 'OPEN', size: 50 });
      expect(mockListActionItemsByPerson).toHaveBeenCalledWith('test-token', 'person-1', { originatingEntryId: 'entry-1', size: 50 });
    });
  });
});
