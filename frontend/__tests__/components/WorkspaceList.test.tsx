import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import WorkspaceList from '@/components/workspace/WorkspaceList';
import { Workspace } from '@/types/workspace';

describe('WorkspaceList', () => {
  const mockWorkspaces: Workspace[] = [
    { id: 'ws-1', name: 'My Team', description: 'Direct reports', displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
    { id: 'ws-2', name: 'Mentees', description: null, displayOrder: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  ];

  const mockOnEdit = jest.fn();
  const mockOnDelete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render empty state when no workspaces', () => {
    render(<WorkspaceList workspaces={[]} onEdit={mockOnEdit} onDelete={mockOnDelete} />);
    expect(screen.getByTestId('workspace-list-empty')).toBeInTheDocument();
    expect(screen.getByText('No workspaces yet')).toBeInTheDocument();
  });

  it('should render workspace items', () => {
    render(<WorkspaceList workspaces={mockWorkspaces} onEdit={mockOnEdit} onDelete={mockOnDelete} />);
    expect(screen.getByText('My Team')).toBeInTheDocument();
    expect(screen.getByText('Direct reports')).toBeInTheDocument();
    expect(screen.getByText('Mentees')).toBeInTheDocument();
  });

  it('should call onEdit when edit button clicked', () => {
    render(<WorkspaceList workspaces={mockWorkspaces} onEdit={mockOnEdit} onDelete={mockOnDelete} />);
    fireEvent.click(screen.getByLabelText('Edit workspace My Team'));
    expect(mockOnEdit).toHaveBeenCalledWith(mockWorkspaces[0]);
  });

  it('should call onDelete when delete button clicked', () => {
    render(<WorkspaceList workspaces={mockWorkspaces} onEdit={mockOnEdit} onDelete={mockOnDelete} />);
    fireEvent.click(screen.getByLabelText('Delete workspace My Team'));
    expect(mockOnDelete).toHaveBeenCalledWith(mockWorkspaces[0]);
  });

  it('should not render description when null', () => {
    render(<WorkspaceList workspaces={[mockWorkspaces[1]]} onEdit={mockOnEdit} onDelete={mockOnDelete} />);
    expect(screen.getByText('Mentees')).toBeInTheDocument();
    expect(screen.queryByText('Direct reports')).not.toBeInTheDocument();
  });

  it('should render edit and delete buttons for each workspace', () => {
    render(<WorkspaceList workspaces={mockWorkspaces} onEdit={mockOnEdit} onDelete={mockOnDelete} />);
    expect(screen.getByLabelText('Edit workspace My Team')).toBeInTheDocument();
    expect(screen.getByLabelText('Delete workspace My Team')).toBeInTheDocument();
    expect(screen.getByLabelText('Edit workspace Mentees')).toBeInTheDocument();
    expect(screen.getByLabelText('Delete workspace Mentees')).toBeInTheDocument();
  });
});
