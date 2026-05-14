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
  if (workspaces.length === 0) {
    return null;
  }

  return (
    <select
      data-testid="workspace-selector"
      value={selectedWorkspaceId || ''}
      onChange={(e) => onWorkspaceChange(e.target.value || null)}
      aria-label="Filter by workspace"
      style={{ minWidth: '160px' }}
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
