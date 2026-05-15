import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import CreatePersonPage from '@/app/people/new/page';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  createPerson: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { createPerson } from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockCreatePerson = createPerson as jest.MockedFunction<typeof createPerson>;

describe('CreatePersonPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state while session is loading', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'loading', update: jest.fn() });
    render(<CreatePersonPage />);
    expect(screen.getByTestId('loading-screen')).toBeInTheDocument();
  });

  it('should show unauthenticated message when not signed in', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'unauthenticated', update: jest.fn() });
    render(<CreatePersonPage />);
    expect(screen.getByTestId('unauthenticated')).toBeInTheDocument();
  });

  it('should render the create form when authenticated', () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);

    render(<CreatePersonPage />);

    expect(screen.getByText('Add New Person')).toBeInTheDocument();
    expect(screen.getByTestId('person-form')).toBeInTheDocument();
    expect(screen.getByText('Create Person')).toBeInTheDocument();
  });

  it('should submit form and navigate to person detail on success', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);

    const createdPerson = {
      id: 'new-person-id',
      name: 'John Doe',
      preferredName: null,
      roleTitle: 'Engineer',
      timezone: null,
      startDate: null,
      email: null,
      tags: [],
      moraleStatus: 'UNKNOWN' as const,
      moraleNote: null,
      pinnedRememberItems: [],
      atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
      createdAt: '2025-05-08T12:00:00Z',
      updatedAt: '2025-05-08T12:00:00Z',
    };
    mockCreatePerson.mockResolvedValue(createdPerson);

    render(<CreatePersonPage />);

    const nameInput = screen.getByRole('textbox', { name: /^Name/ });
    fireEvent.change(nameInput, { target: { value: 'John Doe' } });

    const roleInput = screen.getByLabelText(/Role/);
    fireEvent.change(roleInput, { target: { value: 'Engineer' } });

    const submitButton = screen.getByText('Create Person');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockCreatePerson).toHaveBeenCalledWith('test-token', expect.objectContaining({
        name: 'John Doe',
        roleTitle: 'Engineer',
      }));
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/people/new-person-id');
    });
  });

  it('should display error message when creation fails', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockCreatePerson.mockRejectedValue(new Error('Server error'));

    render(<CreatePersonPage />);

    const nameInput = screen.getByRole('textbox', { name: /^Name/ });
    fireEvent.change(nameInput, { target: { value: 'John Doe' } });

    const submitButton = screen.getByText('Create Person');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toBeInTheDocument();
    });

    expect(screen.getByText('Server error')).toBeInTheDocument();
  });

  it('should navigate back to /people when Cancel is clicked', () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);

    render(<CreatePersonPage />);

    const cancelButton = screen.getByText('Cancel');
    fireEvent.click(cancelButton);

    expect(mockPush).toHaveBeenCalledWith('/people');
  });

  it('should navigate back to /people when back button is clicked', () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);

    render(<CreatePersonPage />);

    const backButton = screen.getByText('← Back to People');
    fireEvent.click(backButton);

    expect(mockPush).toHaveBeenCalledWith('/people');
  });
});
