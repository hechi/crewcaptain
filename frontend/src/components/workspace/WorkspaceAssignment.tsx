'use client';

import { useEffect, useState, useCallback } from 'react';
import { Workspace } from '@/types/workspace';
import { listWorkspaces, assignPersonToWorkspace } from '@/lib/api-client';

interface WorkspaceAssignmentProps {
  token: string;
  personId: string;
  currentWorkspaceId: string | null;
  onAssigned: (workspaceId: string | null) => void;
}

export default function WorkspaceAssignment({
  token,
  personId,
  currentWorkspaceId,
  onAssigned,
}: WorkspaceAssignmentProps) {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [selectedId, setSelectedId] = useState<string>(currentWorkspaceId || '');

  const fetchWorkspaces = useCallback(async () => {
    try {
      const result = await listWorkspaces(token);
      setWorkspaces(result);
    } catch {
      // Silently fail — workspace feature is opt-in
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchWorkspaces();
  }, [fetchWorkspaces]);

  useEffect(() => {
    setSelectedId(currentWorkspaceId || '');
  }, [currentWorkspaceId]);

  const handleChange = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newValue = e.target.value;
    setSelectedId(newValue);
    setSaving(true);
    try {
      const updated = await assignPersonToWorkspace(token, personId, {
        workspaceId: newValue || null,
      });
      onAssigned(updated.workspaceId);
    } catch {
      // Revert on failure
      setSelectedId(currentWorkspaceId || '');
    } finally {
      setSaving(false);
    }
  };

  // Don't render if no workspaces exist (opt-in feature)
  if (!loading && workspaces.length === 0) {
    return null;
  }

  if (loading) {
    return null;
  }

  const currentWorkspace = workspaces.find(w => w.id === currentWorkspaceId);

  return (
    <div data-testid="workspace-assignment" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
      <label
        htmlFor="workspace-assign-select"
        style={{
          fontSize: 'var(--text-caption)',
          color: 'var(--color-text-muted)',
          fontFamily: 'var(--font-mono)',
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
          whiteSpace: 'nowrap',
        }}
      >
        Workspace
      </label>
      <select
        id="workspace-assign-select"
        data-testid="workspace-assign-select"
        value={selectedId}
        onChange={handleChange}
        disabled={saving}
        aria-label="Assign to workspace"
        style={{
          opacity: saving ? 0.6 : 1,
          cursor: saving ? 'not-allowed' : 'pointer',
          minWidth: '140px',
        }}
      >
        <option value="">None</option>
        {workspaces.map((ws) => (
          <option key={ws.id} value={ws.id}>
            {ws.name}
          </option>
        ))}
      </select>
      {saving && (
        <span style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-muted)' }}>
          Saving...
        </span>
      )}
    </div>
  );
}
