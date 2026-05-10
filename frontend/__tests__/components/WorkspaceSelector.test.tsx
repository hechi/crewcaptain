import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import WorkspaceSelector from '@/components/workspace/WorkspaceSelector';
import { Workspace } from '@/types/workspace';

describe('WorkspaceSelector', () => {
  const mockWorkspaces: Workspace[] = [
    { id: 'ws-1', name: 'My Team', description: 'Direct reports', displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
    { id: 'ws-2', name: 'Mentees', description: null, displayOrder: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  ];

  it('should render nothing when no workspaces exist', () => {
    const { container } = render(
      <WorkspaceSelector workspaces={[]} selectedWorkspaceId={null} onWorkspaceChange={jest.fn()} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('should render select with workspace options', () => {
    render(
      <WorkspaceSelector workspaces={mockWorkspaces} selectedWorkspaceId={null} onWorkspaceChange={jest.fn()} />
    );
    expect(screen.getByLabelText('Filter by workspace')).toBeInTheDocument();
    expect(screen.getByText('All workspaces')).toBeInTheDocument();
    expect(screen.getByText('My Team')).toBeInTheDocument();
    expect(screen.getByText('Mentees')).toBeInTheDocument();
  });

  it('should show selected workspace', () => {
    render(
      <WorkspaceSelector workspaces={mockWorkspaces} selectedWorkspaceId="ws-1" onWorkspaceChange={jest.fn()} />
    );
    const select = screen.getByLabelText('Filter by workspace') as HTMLSelectElement;
    expect(select.value).toBe('ws-1');
  });

  it('should call onWorkspaceChange when selection changes', () => {
    const onWorkspaceChange = jest.fn();
    render(
      <WorkspaceSelector workspaces={mockWorkspaces} selectedWorkspaceId={null} onWorkspaceChange={onWorkspaceChange} />
    );
    const select = screen.getByLabelText('Filter by workspace');
    fireEvent.change(select, { target: { value: 'ws-2' } });
    expect(onWorkspaceChange).toHaveBeenCalledWith('ws-2');
  });

  it('should call onWorkspaceChange with null when All workspaces selected', () => {
    const onWorkspaceChange = jest.fn();
    render(
      <WorkspaceSelector workspaces={mockWorkspaces} selectedWorkspaceId="ws-1" onWorkspaceChange={onWorkspaceChange} />
    );
    const select = screen.getByLabelText('Filter by workspace');
    fireEvent.change(select, { target: { value: '' } });
    expect(onWorkspaceChange).toHaveBeenCalledWith(null);
  });
});
