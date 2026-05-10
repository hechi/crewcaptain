'use client';

import { Workspace } from '@/types/workspace';

interface WorkspaceSelectorProps {
  workspaces: Workspace[];
  selectedWorkspaceId: string | null;
  onWorkspaceChange: (workspaceId: string | null) => void;
}

export default function WorkspaceSelector({
  workspaces,
  selectedWorkspaceId,
  onWorkspaceChange,
}: WorkspaceSelectorProps) {
  const selectStyle = {
    padding: '8px 12px',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-medium)',
    fontSize: 'var(--text-body)',
    backgroundColor: 'var(--color-bg-elevated)',
    color: 'var(--color-text-primary)',
    transition: 'border-color 0.2s, box-shadow 0.2s',
    minWidth: '160px',
  };

  if (workspaces.length === 0) {
    return null;
  }

  return (
    <select
      data-testid="workspace-selector"
      value={selectedWorkspaceId || ''}
      onChange={(e) => onWorkspaceChange(e.target.value || null)}
      aria-label="Filter by workspace"
      style={selectStyle}
    >
      <option value="">All workspaces</option>
      {workspaces.map((ws) => (
        <option key={ws.id} value={ws.id}>
          {ws.name}
        </option>
      ))}
    </select>
  );
}
