import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import WorkspaceAssignment from '@/components/workspace/WorkspaceAssignment';

jest.mock('@/lib/api-client', () => ({
  listWorkspaces: jest.fn(),
  assignPersonToWorkspace: jest.fn(),
}));

import { listWorkspaces, assignPersonToWorkspace } from '@/lib/api-client';

const mockListWorkspaces = listWorkspaces as jest.MockedFunction<typeof listWorkspaces>;
const mockAssignPersonToWorkspace = assignPersonToWorkspace as jest.MockedFunction<typeof assignPersonToWorkspace>;

describe('WorkspaceAssignment', () => {
  const defaultProps = {
    token: 'test-token',
    personId: 'person-1',
    currentWorkspaceId: null as string | null,
    onAssigned: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render nothing when no workspaces exist', async () => {
    mockListWorkspaces.mockResolvedValue([]);
    const { container } = render(<WorkspaceAssignment {...defaultProps} />);
    await waitFor(() => {
      expect(mockListWorkspaces).toHaveBeenCalled();
    });
    expect(container.querySelector('[data-testid="workspace-assignment"]')).not.toBeInTheDocument();
  });

  it('should render workspace select when workspaces exist', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: null, displayOrder: 0, createdAt: '', updatedAt: '' },
      { id: 'ws-2', name: 'Mentees', description: null, displayOrder: 1, createdAt: '', updatedAt: '' },
    ]);
    render(<WorkspaceAssignment {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByTestId('workspace-assignment')).toBeInTheDocument();
    });
    expect(screen.getByLabelText('Assign to workspace')).toBeInTheDocument();
    expect(screen.getByText('None')).toBeInTheDocument();
    expect(screen.getByText('My Team')).toBeInTheDocument();
    expect(screen.getByText('Mentees')).toBeInTheDocument();
  });

  it('should show current workspace as selected', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: null, displayOrder: 0, createdAt: '', updatedAt: '' },
    ]);
    render(<WorkspaceAssignment {...defaultProps} currentWorkspaceId="ws-1" />);
    await waitFor(() => {
      expect(screen.getByTestId('workspace-assign-select')).toBeInTheDocument();
    });
    expect((screen.getByTestId('workspace-assign-select') as HTMLSelectElement).value).toBe('ws-1');
  });

  it('should call assignPersonToWorkspace when selection changes', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: null, displayOrder: 0, createdAt: '', updatedAt: '' },
    ]);
    mockAssignPersonToWorkspace.mockResolvedValue({
      id: 'person-1', name: 'Alice', workspaceId: 'ws-1',
      preferredName: null, roleTitle: null, timezone: null, startDate: null,
      email: null, tags: [], moraleStatus: 'UNKNOWN', moraleNote: null,
      pinnedRememberItems: [], atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
      createdAt: '', updatedAt: '', deletedAt: null,
    });

    render(<WorkspaceAssignment {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByTestId('workspace-assign-select')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('workspace-assign-select'), { target: { value: 'ws-1' } });

    await waitFor(() => {
      expect(mockAssignPersonToWorkspace).toHaveBeenCalledWith('test-token', 'person-1', { workspaceId: 'ws-1' });
    });
    expect(defaultProps.onAssigned).toHaveBeenCalledWith('ws-1');
  });

  it('should call assignPersonToWorkspace with null when None selected', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: null, displayOrder: 0, createdAt: '', updatedAt: '' },
    ]);
    mockAssignPersonToWorkspace.mockResolvedValue({
      id: 'person-1', name: 'Alice', workspaceId: null,
      preferredName: null, roleTitle: null, timezone: null, startDate: null,
      email: null, tags: [], moraleStatus: 'UNKNOWN', moraleNote: null,
      pinnedRememberItems: [], atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
      createdAt: '', updatedAt: '', deletedAt: null,
    });

    render(<WorkspaceAssignment {...defaultProps} currentWorkspaceId="ws-1" />);
    await waitFor(() => {
      expect(screen.getByTestId('workspace-assign-select')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('workspace-assign-select'), { target: { value: '' } });

    await waitFor(() => {
      expect(mockAssignPersonToWorkspace).toHaveBeenCalledWith('test-token', 'person-1', { workspaceId: null });
    });
    expect(defaultProps.onAssigned).toHaveBeenCalledWith(null);
  });

  it('should revert selection on API failure', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: null, displayOrder: 0, createdAt: '', updatedAt: '' },
    ]);
    mockAssignPersonToWorkspace.mockRejectedValue(new Error('Network error'));

    render(<WorkspaceAssignment {...defaultProps} currentWorkspaceId={null} />);
    await waitFor(() => {
      expect(screen.getByTestId('workspace-assign-select')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('workspace-assign-select'), { target: { value: 'ws-1' } });

    await waitFor(() => {
      expect((screen.getByTestId('workspace-assign-select') as HTMLSelectElement).value).toBe('');
    });
    expect(defaultProps.onAssigned).not.toHaveBeenCalled();
  });

  it('should show Saving indicator while request is in flight', async () => {
    mockListWorkspaces.mockResolvedValue([
      { id: 'ws-1', name: 'My Team', description: null, displayOrder: 0, createdAt: '', updatedAt: '' },
    ]);
    // Never resolve to keep saving state
    mockAssignPersonToWorkspace.mockReturnValue(new Promise(() => {}));

    render(<WorkspaceAssignment {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByTestId('workspace-assign-select')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('workspace-assign-select'), { target: { value: 'ws-1' } });

    await waitFor(() => {
      expect(screen.getByText('Saving...')).toBeInTheDocument();
    });
  });
});
