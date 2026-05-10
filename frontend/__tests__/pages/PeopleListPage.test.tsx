import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PeopleListPage from '@/app/people/page';
import { Person, PaginatedResponse } from '@/types/person';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  listPersons: jest.fn(),
}));

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

import { useSession } from 'next-auth/react';
import { listPersons } from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockListPersons = listPersons as jest.MockedFunction<typeof listPersons>;

const mockPerson: Person = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  name: 'Jane Smith',
  preferredName: 'Jane',
  roleTitle: 'Senior Engineer',
  timezone: 'Europe/Berlin',
  startDate: '2024-03-15',
  email: 'jane@example.com',
  tags: ['engineering'],
  moraleStatus: 'GREEN',
  moraleNote: null,
  pinnedRememberItems: [],
  atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-08T12:00:00Z',
};

const mockPaginatedResponse: PaginatedResponse<Person> = {
  content: [mockPerson],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

describe('PeopleListPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state while session is loading', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'loading', update: jest.fn() });
    render(<PeopleListPage />);
    expect(screen.getByTestId('loading')).toBeInTheDocument();
  });

  it('should show unauthenticated message when not signed in', () => {
    mockUseSession.mockReturnValue({ data: null, status: 'unauthenticated', update: jest.fn() });
    render(<PeopleListPage />);
    expect(screen.getByTestId('unauthenticated')).toBeInTheDocument();
  });

  it('should render people list when data is loaded', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockListPersons.mockResolvedValue(mockPaginatedResponse);

    render(<PeopleListPage />);

    await waitFor(() => {
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    });

    expect(screen.getByText('Senior Engineer')).toBeInTheDocument();
    expect(mockListPersons).toHaveBeenCalledWith('test-token', {
      page: 0,
      size: 20,
      tag: undefined,
      morale: undefined,
    });
  });

  it('should render empty state when no people exist', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockListPersons.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    render(<PeopleListPage />);

    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
  });

  it('should navigate to /people/new when Add Person button is clicked', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockListPersons.mockResolvedValue(mockPaginatedResponse);

    render(<PeopleListPage />);

    await waitFor(() => {
      expect(screen.getByTestId('add-person-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('add-person-button'));
    expect(mockPush).toHaveBeenCalledWith('/people/new');
  });

  it('should render pagination when multiple pages exist', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockListPersons.mockResolvedValue({
      ...mockPaginatedResponse,
      totalPages: 3,
      totalElements: 50,
    });

    render(<PeopleListPage />);

    await waitFor(() => {
      expect(screen.getByTestId('pagination')).toBeInTheDocument();
    });

    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument();
  });

  it('should not render pagination when only one page exists', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockListPersons.mockResolvedValue(mockPaginatedResponse);

    render(<PeopleListPage />);

    await waitFor(() => {
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    });

    expect(screen.queryByTestId('pagination')).not.toBeInTheDocument();
  });

  it('should display error message when API call fails', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockListPersons.mockRejectedValue(new Error('Network error'));

    render(<PeopleListPage />);

    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toBeInTheDocument();
    });

    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('should render Trash button that navigates to /people/trash', async () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockListPersons.mockResolvedValue(mockPaginatedResponse);

    render(<PeopleListPage />);

    await waitFor(() => {
      expect(screen.getByTestId('trash-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('trash-button'));
    expect(mockPush).toHaveBeenCalledWith('/people/trash');
  });
});
