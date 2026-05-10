import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import TrashPage from '@/app/people/trash/page';
import { Person, PaginatedResponse } from '@/types/person';

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  listDeletedPersons: jest.fn(),
  restorePerson: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { listDeletedPersons, restorePerson } from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockListDeletedPersons = listDeletedPersons as jest.MockedFunction<typeof listDeletedPersons>;
const mockRestorePerson = restorePerson as jest.MockedFunction<typeof restorePerson>;

const mockDeletedPerson: Person = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  name: 'Deleted Person',
  preferredName: null,
  roleTitle: 'Engineer',
  timezone: null,
  startDate: null,
  email: null,
  tags: [],
  moraleStatus: 'UNKNOWN',
  moraleNote: null,
  pinnedRememberItems: [],
  atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-10T12:00:00Z',
  deletedAt: '2025-05-10T12:00:00Z',
};

const mockPaginatedResponse: PaginatedResponse<Person> = {
  content: [mockDeletedPerson],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

describe('TrashPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state initially', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'loading',
      update: jest.fn(),
    });

    render(<TrashPage />);
    expect(screen.getByTestId('loading')).toBeInTheDocument();
  });

  it('should show unauthenticated message when not signed in', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    render(<TrashPage />);
    expect(screen.getByTestId('unauthenticated')).toBeInTheDocument();
  });

  it('should display deleted people when authenticated', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test' }, expires: '2099-01-01' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockListDeletedPersons.mockResolvedValue(mockPaginatedResponse);

    render(<TrashPage />);

    await waitFor(() => {
      expect(screen.getByText('Deleted Person')).toBeInTheDocument();
    });
    expect(screen.getByText('Engineer')).toBeInTheDocument();
  });

  it('should show empty state when no deleted people', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test' }, expires: '2099-01-01' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockListDeletedPersons.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    render(<TrashPage />);

    await waitFor(() => {
      expect(screen.getByText('Trash is empty. Deleted people will appear here.')).toBeInTheDocument();
    });
  });

  it('should show error message on API failure', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test' }, expires: '2099-01-01' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockListDeletedPersons.mockRejectedValue(new Error('Network error'));

    render(<TrashPage />);

    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toBeInTheDocument();
    });
    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('should restore a person when restore button is clicked', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test' }, expires: '2099-01-01' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockListDeletedPersons
      .mockResolvedValueOnce(mockPaginatedResponse)
      .mockResolvedValueOnce({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    mockRestorePerson.mockResolvedValue({ ...mockDeletedPerson, deletedAt: null });

    render(<TrashPage />);

    await waitFor(() => {
      expect(screen.getByText('Deleted Person')).toBeInTheDocument();
    });

    const restoreButton = screen.getByTestId(`restore-button-${mockDeletedPerson.id}`);
    fireEvent.click(restoreButton);

    await waitFor(() => {
      expect(mockRestorePerson).toHaveBeenCalledWith('test-token', mockDeletedPerson.id);
    });

    // After restore, the list should be refreshed
    await waitFor(() => {
      expect(mockListDeletedPersons).toHaveBeenCalledTimes(2);
    });
  });

  it('should show error when restore fails', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test' }, expires: '2099-01-01' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockListDeletedPersons.mockResolvedValue(mockPaginatedResponse);
    mockRestorePerson.mockRejectedValue(new Error('Restore failed'));

    render(<TrashPage />);

    await waitFor(() => {
      expect(screen.getByText('Deleted Person')).toBeInTheDocument();
    });

    const restoreButton = screen.getByTestId(`restore-button-${mockDeletedPerson.id}`);
    fireEvent.click(restoreButton);

    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toBeInTheDocument();
    });
  });

  it('should display the Trash heading', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test' }, expires: '2099-01-01' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockListDeletedPersons.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    render(<TrashPage />);

    expect(screen.getByText('Trash')).toBeInTheDocument();
  });

  it('should display deleted date for each person', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test' }, expires: '2099-01-01' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockListDeletedPersons.mockResolvedValue(mockPaginatedResponse);

    render(<TrashPage />);

    await waitFor(() => {
      expect(screen.getByText(/Deleted/)).toBeInTheDocument();
    });
  });

  it('should show Restoring state on button during restore', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test' }, expires: '2099-01-01' },
      status: 'authenticated',
      update: jest.fn(),
    });
    mockListDeletedPersons.mockResolvedValue(mockPaginatedResponse);
    // Make restore hang
    mockRestorePerson.mockImplementation(() => new Promise(() => {}));

    render(<TrashPage />);

    await waitFor(() => {
      expect(screen.getByText('Deleted Person')).toBeInTheDocument();
    });

    const restoreButton = screen.getByTestId(`restore-button-${mockDeletedPerson.id}`);
    fireEvent.click(restoreButton);

    await waitFor(() => {
      expect(screen.getByText('Restoring...')).toBeInTheDocument();
    });
  });
});
