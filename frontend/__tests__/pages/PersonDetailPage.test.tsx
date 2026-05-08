import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PersonDetailPage from '@/app/people/[id]/page';
import { Person } from '@/types/person';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => ({ id: '123e4567-e89b-12d3-a456-426614174000' }),
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  getPerson: jest.fn(),
  updatePerson: jest.fn(),
  deletePerson: jest.fn(),
  setMorale: jest.fn(),
  addRememberItem: jest.fn(),
  removeRememberItem: jest.fn(),
  reorderRememberItems: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { getPerson, updatePerson, deletePerson, setMorale } from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockGetPerson = getPerson as jest.MockedFunction<typeof getPerson>;
const mockUpdatePerson = updatePerson as jest.MockedFunction<typeof updatePerson>;
const mockDeletePerson = deletePerson as jest.MockedFunction<typeof deletePerson>;
const mockSetMorale = setMorale as jest.MockedFunction<typeof setMorale>;

const mockPerson: Person = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  name: 'Jane Smith',
  preferredName: 'Jane',
  roleTitle: 'Senior Engineer',
  timezone: 'Europe/Berlin',
  startDate: '2024-03-15',
  email: 'jane@example.com',
  tags: ['engineering', 'senior'],
  moraleStatus: 'GREEN',
  moraleNote: 'Doing great',
  pinnedRememberItems: [
    { id: 'item-1', text: 'Prefers async', displayOrder: 0, createdAt: '2025-05-08T12:00:00Z' },
  ],
  atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-08T12:00:00Z',
};

describe('PersonDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state initially', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'loading', update: jest.fn() });
    render(<PersonDetailPage />);
    expect(screen.getByTestId('loading')).toBeInTheDocument();
  });

  it('should show unauthenticated message when not signed in', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'unauthenticated', update: jest.fn() });
    render(<PersonDetailPage />);
    expect(screen.getByTestId('unauthenticated')).toBeInTheDocument();
  });

  it('should render person details when loaded', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    });

    expect(screen.getByText('Senior Engineer')).toBeInTheDocument();
    expect(screen.getByText('Jane')).toBeInTheDocument();
    expect(screen.getByText('jane@example.com')).toBeInTheDocument();
    expect(screen.getByText('Europe/Berlin')).toBeInTheDocument();
    expect(screen.getByText('2024-03-15')).toBeInTheDocument();
    expect(screen.getByText('engineering, senior')).toBeInTheDocument();
  });

  it('should render morale indicator', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('morale-indicator')).toBeInTheDocument();
    });
  });

  it('should render remember items', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Prefers async')).toBeInTheDocument();
    });
  });

  it('should render at-a-glance section with placeholders', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('at-a-glance-section')).toBeInTheDocument();
    });

    expect(screen.getByText("No 1:1s yet")).toBeInTheDocument();
    expect(screen.getByText('N/A')).toBeInTheDocument();
    expect(screen.getByText('No goals set')).toBeInTheDocument();
  });

  it('should show edit form when Edit button is clicked', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('edit-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('edit-button'));

    expect(screen.getByTestId('person-form')).toBeInTheDocument();
  });

  it('should handle person update via edit form', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);
    const updatedPerson = { ...mockPerson, name: 'Jane Updated' };
    mockUpdatePerson.mockResolvedValue(updatedPerson);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('edit-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('edit-button'));

    const nameInput = screen.getByRole('textbox', { name: /^Name/ });
    fireEvent.change(nameInput, { target: { value: 'Jane Updated' } });

    const submitButton = screen.getByText('Save Changes');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockUpdatePerson).toHaveBeenCalled();
    });
  });

  it('should show delete confirmation when Delete button is clicked', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('delete-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('delete-button'));

    expect(screen.getByTestId('delete-confirmation')).toBeInTheDocument();
    expect(screen.getByText('Are you sure you want to delete this person?')).toBeInTheDocument();
  });

  it('should delete person and navigate to /people on confirm', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);
    mockDeletePerson.mockResolvedValue(undefined);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('delete-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('delete-button'));
    fireEvent.click(screen.getByTestId('confirm-delete-button'));

    await waitFor(() => {
      expect(mockDeletePerson).toHaveBeenCalledWith('test-token', '123e4567-e89b-12d3-a456-426614174000');
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/people');
    });
  });

  it('should cancel delete when Cancel button is clicked', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('delete-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('delete-button'));
    expect(screen.getByTestId('delete-confirmation')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('cancel-delete-button'));
    expect(screen.queryByTestId('delete-confirmation')).not.toBeInTheDocument();
  });

  it('should render morale update controls', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('morale-status-select')).toBeInTheDocument();
    });

    expect(screen.getByTestId('morale-note-input')).toBeInTheDocument();
    expect(screen.getByTestId('update-morale-button')).toBeInTheDocument();
  });

  it('should update morale when Update Morale button is clicked', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockResolvedValue(mockPerson);
    mockSetMorale.mockResolvedValue({ ...mockPerson, moraleStatus: 'YELLOW', moraleNote: 'Needs support' });

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('morale-status-select')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('morale-status-select'), { target: { value: 'YELLOW' } });
    fireEvent.change(screen.getByTestId('morale-note-input'), { target: { value: 'Needs support' } });
    fireEvent.click(screen.getByTestId('update-morale-button'));

    await waitFor(() => {
      expect(mockSetMorale).toHaveBeenCalledWith('test-token', '123e4567-e89b-12d3-a456-426614174000', {
        status: 'YELLOW',
        note: 'Needs support',
      });
    });
  });

  it('should display error when person fetch fails', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockGetPerson.mockRejectedValue(new Error('Person not found'));

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toBeInTheDocument();
    });

    expect(screen.getByText('Person not found')).toBeInTheDocument();
  });
});
