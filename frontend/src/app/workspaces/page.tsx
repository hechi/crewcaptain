'use client';

import { useEffect, useState, useCallback } from 'react';
import { Workspace } from '@/types/workspace';
import { listWorkspaces, createWorkspace, updateWorkspace, deleteWorkspace } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import WorkspaceForm from '@/components/workspace/WorkspaceForm';
import WorkspaceList from '@/components/workspace/WorkspaceList';

export default function WorkspacesPage() {
  const { getToken, isAuthenticated, status } = useStableToken();
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingWorkspace, setEditingWorkspace] = useState<Workspace | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Workspace | null>(null);

  const fetchWorkspaces = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await listWorkspaces(token);
      setWorkspaces(result);
    } catch (err) {
      setError('Failed to load workspaces');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated]);

  useEffect(() => {
    fetchWorkspaces();
  }, [fetchWorkspaces]);

  const handleCreate = async (data: { name: string; description?: string }) => {
    const token = getToken();
    if (!token) return;
    setIsSubmitting(true);
    try {
      await createWorkspace(token, data);
      setShowForm(false);
      await fetchWorkspaces();
    } catch (err) {
      setError('Failed to create workspace');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdate = async (data: { name: string; description?: string }) => {
    const token = getToken();
    if (!token || !editingWorkspace) return;
    setIsSubmitting(true);
    try {
      await updateWorkspace(token, editingWorkspace.id, data);
      setEditingWorkspace(null);
      await fetchWorkspaces();
    } catch (err) {
      setError('Failed to update workspace');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (workspace: Workspace) => {
    const token = getToken();
    if (!token) return;
    try {
      await deleteWorkspace(token, workspace.id);
      setConfirmDelete(null);
      await fetchWorkspaces();
    } catch (err) {
      setError('Failed to delete workspace');
    }
  };

  const handleEdit = (workspace: Workspace) => {
    setEditingWorkspace(workspace);
    setShowForm(false);
  };

  const handleDeleteClick = (workspace: Workspace) => {
    setConfirmDelete(workspace);
  };

  if (status === 'loading' || loading) {
    return (
      <div style={{ padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
        Loading workspaces...
      </div>
    );
  }

  const pageStyle = {
    maxWidth: '700px',
    margin: '0 auto',
    padding: 'var(--space-6)',
  };

  const headerStyle = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 'var(--space-5)',
  };

  const buttonStyle = {
    padding: '10px 20px',
    borderRadius: 'var(--radius-medium)',
    fontSize: 'var(--text-body)',
    fontWeight: '500' as const,
    cursor: 'pointer',
    border: 'none',
    backgroundColor: 'var(--color-accent)',
    color: 'var(--color-bg-base)',
    transition: 'background-color 0.2s',
  };

  return (
    <div style={pageStyle}>
      <div style={headerStyle}>
        <h1 style={{ margin: 0, fontSize: 'var(--text-heading)', fontFamily: 'var(--font-heading)', color: 'var(--color-text-primary)' }}>
          Workspaces
        </h1>
        {!showForm && !editingWorkspace && (
          <button
            onClick={() => setShowForm(true)}
            style={buttonStyle}
            data-testid="create-workspace-button"
          >
            New Workspace
          </button>
        )}
      </div>

      {error && (
        <div data-testid="workspace-error" style={{ padding: 'var(--space-3)', marginBottom: 'var(--space-4)', borderRadius: 'var(--radius-medium)', backgroundColor: 'rgba(255, 59, 48, 0.1)', color: 'var(--color-danger)', fontSize: 'var(--text-small)' }}>
          {error}
        </div>
      )}

      {showForm && (
        <div style={{ marginBottom: 'var(--space-5)' }}>
          <h2 style={{ fontSize: 'var(--text-body)', fontWeight: '600', marginBottom: 'var(--space-3)', color: 'var(--color-text-primary)' }}>
            Create Workspace
          </h2>
          <WorkspaceForm
            onSubmit={handleCreate}
            onCancel={() => setShowForm(false)}
            isSubmitting={isSubmitting}
          />
        </div>
      )}

      {editingWorkspace && (
        <div style={{ marginBottom: 'var(--space-5)' }}>
          <h2 style={{ fontSize: 'var(--text-body)', fontWeight: '600', marginBottom: 'var(--space-3)', color: 'var(--color-text-primary)' }}>
            Edit Workspace
          </h2>
          <WorkspaceForm
            workspace={editingWorkspace}
            onSubmit={handleUpdate}
            onCancel={() => setEditingWorkspace(null)}
            isSubmitting={isSubmitting}
          />
        </div>
      )}

      {confirmDelete && (
        <div data-testid="delete-confirmation" style={{ padding: 'var(--space-4)', marginBottom: 'var(--space-4)', borderRadius: 'var(--radius-medium)', border: '1px solid var(--color-danger)', backgroundColor: 'rgba(255, 59, 48, 0.05)' }}>
          <p style={{ margin: '0 0 var(--space-3)', color: 'var(--color-text-primary)' }}>
            Delete workspace &quot;{confirmDelete.name}&quot;? People in this workspace will become unassigned.
          </p>
          <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
            <button
              onClick={() => handleDelete(confirmDelete)}
              data-testid="confirm-delete-button"
              style={{ ...buttonStyle, backgroundColor: 'var(--color-danger)' }}
            >
              Yes, Delete
            </button>
            <button
              onClick={() => setConfirmDelete(null)}
              data-testid="cancel-delete-button"
              style={{ ...buttonStyle, backgroundColor: 'var(--color-bg-elevated)', color: 'var(--color-text-secondary)', border: '1px solid var(--color-border)' }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <WorkspaceList
        workspaces={workspaces}
        onEdit={handleEdit}
        onDelete={handleDeleteClick}
      />
    </div>
  );
}
