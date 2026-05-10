'use client';

import { Workspace } from '@/types/workspace';

interface WorkspaceListProps {
  workspaces: Workspace[];
  onEdit: (workspace: Workspace) => void;
  onDelete: (workspace: Workspace) => void;
}

export default function WorkspaceList({ workspaces, onEdit, onDelete }: WorkspaceListProps) {
  if (workspaces.length === 0) {
    return (
      <div data-testid="workspace-list-empty" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
        <p style={{ fontSize: 'var(--text-body)' }}>No workspaces yet</p>
        <p style={{ fontSize: 'var(--text-small)' }}>Create a workspace to organize your people into groups.</p>
      </div>
    );
  }

  const cardStyle = {
    padding: 'var(--space-4)',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-medium)',
    backgroundColor: 'var(--color-bg-elevated)',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    transition: 'border-color 0.2s',
  };

  const buttonStyle = {
    padding: '6px 12px',
    borderRadius: 'var(--radius-small)',
    fontSize: 'var(--text-small)',
    cursor: 'pointer',
    border: '1px solid var(--color-border)',
    backgroundColor: 'transparent',
    color: 'var(--color-text-secondary)',
    transition: 'background-color 0.2s',
  };

  return (
    <div data-testid="workspace-list" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
      {workspaces.map((workspace) => (
        <div key={workspace.id} data-testid={`workspace-item-${workspace.id}`} style={cardStyle}>
          <div>
            <h3 style={{ margin: 0, fontSize: 'var(--text-body)', fontWeight: '600', color: 'var(--color-text-primary)' }}>
              {workspace.name}
            </h3>
            {workspace.description && (
              <p style={{ margin: '4px 0 0', fontSize: 'var(--text-small)', color: 'var(--color-text-muted)' }}>
                {workspace.description}
              </p>
            )}
          </div>
          <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
            <button
              onClick={() => onEdit(workspace)}
              style={buttonStyle}
              aria-label={`Edit workspace ${workspace.name}`}
            >
              Edit
            </button>
            <button
              onClick={() => onDelete(workspace)}
              style={{ ...buttonStyle, color: 'var(--color-danger)' }}
              aria-label={`Delete workspace ${workspace.name}`}
            >
              Delete
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
