import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import WorkspacesPage from '@/app/workspaces/page';

// Mock next-auth
jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

// Mock API client
jest.mock('@/lib/api-client', () => ({
  listWorkspaces: jest.fn(),
  createWorkspace: jest.fn(),
  updateWorkspace: jest.fn(),
  deleteWorkspace: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { listWorkspaces, createWorkspace, updateWorkspace, deleteWorkspace } from '@/lib/api-client';

const mockUseSession = useSession as jest.Mock;
const mockListWorkspaces = listWorkspaces as jest.MockedFunction<typeof listWorkspaces>;
const mockCreateWorkspace = createWorkspace as jest.MockedFunction<typeof createWorkspace>;
const mockUpdateWorkspace = updateWorkspace as jest.MockedFunction<typeof updateWorkspace>;
const mockDeleteWorkspace = deleteWorkspace as jest.MockedFunction<typeof deleteWorkspace>;

describe('WorkspacesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: { name: 'Test User' } },
      status: 'authenticated',
    });
    mockListWorkspaces.mockResolvedValue([]);
  });

  it('should render page title', async () => {
    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByText('Workspaces')).toBeInTheDocument();
    });
  });

  it('should show create button', async () => {
    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('create-workspace-button')).toBeInTheDocument();
    });
  });

  it('should show empty state when no workspaces', async () => {
    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByText('No workspaces yet')).toBeInTheDocument();
    });
  });

  it('should show workspace list when workspaces exist', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: 'Direct reports', displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
    ]);
    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByText('My Team')).toBeInTheDocument();
    });
  });

  it('should show create form when New Workspace clicked', async () => {
    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('create-workspace-button')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('create-workspace-button'));
    expect(screen.getByText('Create Workspace')).toBeInTheDocument();
  });

  it('should create workspace and refresh list', async () => {
    mockCreateWorkspace.mockResolvedValue({
      id: 'ws-new', name: 'Mentees', description: null, displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    });
    mockListWorkspaces
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([
        { id: 'ws-new', name: 'Mentees', description: null, displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
      ]);

    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('create-workspace-button')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('create-workspace-button'));
    fireEvent.change(screen.getByLabelText(/Name/), { target: { value: 'Mentees' } });
    fireEvent.click(screen.getByText('Create'));

    await waitFor(() => {
      expect(mockCreateWorkspace).toHaveBeenCalledWith('test-token', { name: 'Mentees', description: undefined });
    });
  });

  it('should show edit form when edit clicked', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: 'Direct reports', displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
    ]);
    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByText('My Team')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByLabelText('Edit workspace My Team'));
    expect(screen.getByText('Edit Workspace')).toBeInTheDocument();
  });

  it('should show delete confirmation when delete clicked', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: 'Direct reports', displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
    ]);
    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByText('My Team')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByLabelText('Delete workspace My Team'));
    expect(screen.getByTestId('delete-confirmation')).toBeInTheDocument();
  });

  it('should delete workspace when confirmed', async () => {
    mockDeleteWorkspace.mockResolvedValue(undefined);
    mockListWorkspaces
      .mockResolvedValueOnce([
        { id: 'ws-1', name: 'My Team', description: null, displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
      ])
      .mockResolvedValueOnce([]);

    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByText('My Team')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByLabelText('Delete workspace My Team'));
    fireEvent.click(screen.getByTestId('confirm-delete-button'));

    await waitFor(() => {
      expect(mockDeleteWorkspace).toHaveBeenCalledWith('test-token', 'ws-1');
    });
  });

  it('should cancel delete when cancel clicked', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: null, displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
    ]);
    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByText('My Team')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByLabelText('Delete workspace My Team'));
    expect(screen.getByTestId('delete-confirmation')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('cancel-delete-button'));
    expect(screen.queryByTestId('delete-confirmation')).not.toBeInTheDocument();
  });

  it('should show error message on API failure', async () => {
    mockListWorkspaces.mockRejectedValue(new Error('Network error'));
    render(<WorkspacesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('workspace-error')).toBeInTheDocument();
    });
  });
});
