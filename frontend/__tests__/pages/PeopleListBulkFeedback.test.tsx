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

jest.mock('@/lib/useStableToken', () => {
  const getToken = () => 'test-token';
  const value = { getToken, isAuthenticated: true, status: 'authenticated' };
  return { useStableToken: jest.fn(() => value) };
});

jest.mock('@/lib/api-client', () => ({
  listPersons: jest.fn(),
  listWorkspaces: jest.fn(),
  listFeedbackTemplates: jest.fn(),
  bulkCreateFeedbackLinks: jest.fn(),
}));

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

import { useSession } from 'next-auth/react';
import { listPersons, listWorkspaces, listFeedbackTemplates, bulkCreateFeedbackLinks } from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockListPersons = listPersons as jest.MockedFunction<typeof listPersons>;
const mockListWorkspaces = listWorkspaces as jest.MockedFunction<typeof listWorkspaces>;
const mockListTemplates = listFeedbackTemplates as jest.MockedFunction<typeof listFeedbackTemplates>;
const mockBulkCreate = bulkCreateFeedbackLinks as jest.MockedFunction<typeof bulkCreateFeedbackLinks>;

function makePerson(id: string, name: string): Person {
  return {
    id,
    name,
    preferredName: null,
    roleTitle: 'Engineer',
    timezone: null,
    startDate: null,
    email: null,
    tags: [],
    moraleStatus: 'GREEN',
    moraleNote: null,
    pinnedRememberItems: [],
    workspaceId: null,
    atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
    createdAt: '2025-05-08T12:00:00Z',
    updatedAt: '2025-05-08T12:00:00Z',
    deletedAt: null,
  };
}

const paginated: PaginatedResponse<Person> = {
  content: [makePerson('p-1', 'Jane Smith'), makePerson('p-2', 'John Doe')],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
};

describe('PeopleListPage - Bulk feedback links', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as ReturnType<typeof useSession>);
    mockListWorkspaces.mockResolvedValue([]);
    mockListPersons.mockResolvedValue(paginated);
    mockListTemplates.mockResolvedValue([
      { id: 'tpl-1', title: 'Peer feedback', description: null, questions: [] },
    ]);
    mockBulkCreate.mockResolvedValue({
      links: [
        { personId: 'p-1', personName: 'Jane Smith', token: 'tok-jane', linkId: 'link-1' },
        { personId: 'p-2', personName: 'John Doe', token: 'tok-john', linkId: 'link-2' },
      ],
    });
    Object.assign(navigator, { clipboard: { writeText: jest.fn().mockResolvedValue(undefined) } });
  });

  it('opens the bulk modal, selects people + template, and submits calling bulkCreateFeedbackLinks', async () => {
    render(<PeopleListPage />);

    await waitFor(() => {
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('bulk-feedback-links-button'));

    await waitFor(() => {
      expect(screen.getByTestId('bulk-feedback-links-modal')).toBeInTheDocument();
    });

    // Template list loaded
    await waitFor(() => {
      expect(screen.getByRole('option', { name: 'Peer feedback' })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('bulk-feedback-links-template-select'), { target: { value: 'tpl-1' } });
    fireEvent.click(screen.getByTestId('bulk-feedback-links-person-p-1'));
    fireEvent.click(screen.getByTestId('bulk-feedback-links-person-p-2'));

    fireEvent.click(screen.getByTestId('bulk-feedback-links-submit'));

    await waitFor(() => {
      expect(mockBulkCreate).toHaveBeenCalledWith(
        'test-token',
        expect.objectContaining({ personIds: ['p-1', 'p-2'], templateId: 'tpl-1', expiresInDays: 14 })
      );
    });
  });

  it('renders a results table and a Download CSV button after creating links', async () => {
    render(<PeopleListPage />);

    await waitFor(() => {
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('bulk-feedback-links-button'));

    await waitFor(() => {
      expect(screen.getByRole('option', { name: 'Peer feedback' })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('bulk-feedback-links-template-select'), { target: { value: 'tpl-1' } });
    fireEvent.click(screen.getByTestId('bulk-feedback-links-person-p-1'));
    fireEvent.click(screen.getByTestId('bulk-feedback-links-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('bulk-feedback-links-table')).toBeInTheDocument();
    });

    expect(screen.getByTestId('bulk-feedback-link-row-p-1')).toHaveTextContent('Jane Smith');
    expect(screen.getByTestId('bulk-feedback-link-row-p-1')).toHaveTextContent(
      `${window.location.origin}/f/tok-jane`
    );
    expect(screen.getByTestId('bulk-feedback-links-download-csv')).toBeInTheDocument();
  });
});
