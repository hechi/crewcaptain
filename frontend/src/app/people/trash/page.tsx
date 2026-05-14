'use client';

import { useEffect, useState, useCallback } from 'react';
import { Person, PaginatedResponse } from '@/types/person';
import { listDeletedPersons, restorePerson, permanentlyDeletePerson } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import Pagination from '@/components/Pagination';
import EmptyState from '@/components/EmptyState';

export default function TrashPage() {
  const { getToken, isAuthenticated, status } = useStableToken();

  const [deletedPeople, setDeletedPeople] = useState<PaginatedResponse<Person> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [restoringId, setRestoringId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

  const fetchDeletedPeople = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await listDeletedPersons(token, {
        page,
        size: 20,
      });
      setDeletedPeople(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load deleted people');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated, page]);

  useEffect(() => {
    fetchDeletedPeople();
  }, [fetchDeletedPeople]);

  const handleRestore = async (personId: string) => {
    const token = getToken();
    if (!token) return;

    setRestoringId(personId);
    try {
      await restorePerson(token, personId);
      await fetchDeletedPeople();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to restore person');
    } finally {
      setRestoringId(null);
    }
  };

  const handlePermanentDelete = async (personId: string) => {
    const token = getToken();
    if (!token) return;

    setDeletingId(personId);
    try {
      await permanentlyDeletePerson(token, personId);
      setConfirmDeleteId(null);
      await fetchDeletedPeople();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to permanently delete person');
      setConfirmDeleteId(null);
    } finally {
      setDeletingId(null);
    }
  };

  if (status === 'loading') {
    return <div data-testid="loading">Loading...</div>;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: 'var(--space-6)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-6)' }}>
        <h1 style={{
          margin: 0,
          fontSize: 'var(--text-h2)',
          fontWeight: 'var(--weight-bold)',
          fontFamily: 'var(--font-heading)',
          color: 'var(--color-text-primary)',
          letterSpacing: '-0.3px',
        }}>
          Trash
        </h1>
      </div>

      <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--space-4)' }}>
        Deleted people are kept here. You can restore them or permanently delete them.
      </p>

      {loading && <div data-testid="loading" style={{ color: 'var(--color-text-secondary)' }}>Loading...</div>}

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-alert)', padding: 'var(--space-4)' }}>
          {error}
        </div>
      )}

      {!loading && !error && deletedPeople && deletedPeople.content.length === 0 && (
        <EmptyState
          message="Trash is empty. Deleted people will appear here."
        />
      )}

      {!loading && !error && deletedPeople && deletedPeople.content.length > 0 && (
        <>
          <div data-testid="trash-list" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
            {deletedPeople.content.map((person) => (
              <div
                key={person.id}
                data-testid={`trash-item-${person.id}`}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: 'var(--space-4)',
                  background: 'var(--color-bg-card)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                }}
              >
                <div>
                  <div style={{
                    fontWeight: 'var(--weight-semibold)',
                    color: 'var(--color-text-primary)',
                    fontSize: 'var(--text-body)',
                  }}>
                    {person.name}
                  </div>
                  {person.roleTitle && (
                    <div style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--text-small)' }}>
                      {person.roleTitle}
                    </div>
                  )}
                  {person.deletedAt && (
                    <div style={{ color: 'var(--color-text-muted)', fontSize: 'var(--text-small)', marginTop: 'var(--space-1)' }}>
                      Deleted {new Date(person.deletedAt).toLocaleDateString()}
                    </div>
                  )}
                </div>
                <div style={{ display: 'flex', gap: 'var(--space-2)', alignItems: 'center' }}>
                  <button
                    type="button"
                    onClick={() => handleRestore(person.id)}
                    disabled={restoringId === person.id || deletingId === person.id}
                    data-testid={`restore-button-${person.id}`}
                    style={{
                      padding: '8px 16px',
                      backgroundColor: 'transparent',
                      color: 'var(--color-primary)',
                      border: '1px solid var(--color-primary)',
                      borderRadius: 'var(--radius-medium)',
                      fontSize: 'var(--text-small)',
                      fontWeight: 'var(--weight-semibold)',
                      cursor: restoringId === person.id ? 'not-allowed' : 'pointer',
                      opacity: restoringId === person.id ? 0.5 : 1,
                    }}
                  >
                    {restoringId === person.id ? 'Restoring...' : 'Restore'}
                  </button>
                  {confirmDeleteId === person.id ? (
                    <div style={{ display: 'flex', gap: 'var(--space-2)', alignItems: 'center' }}>
                      <span style={{ color: 'var(--color-alert)', fontSize: 'var(--text-small)' }}>
                        Are you sure?
                      </span>
                      <button
                        type="button"
                        onClick={() => handlePermanentDelete(person.id)}
                        disabled={deletingId === person.id}
                        data-testid={`confirm-delete-button-${person.id}`}
                        style={{
                          padding: '8px 16px',
                          backgroundColor: 'var(--color-alert)',
                          color: '#fff',
                          border: 'none',
                          borderRadius: 'var(--radius-medium)',
                          fontSize: 'var(--text-small)',
                          fontWeight: 'var(--weight-semibold)',
                          cursor: deletingId === person.id ? 'not-allowed' : 'pointer',
                          opacity: deletingId === person.id ? 0.5 : 1,
                        }}
                      >
                        {deletingId === person.id ? 'Deleting...' : 'Yes, Delete'}
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmDeleteId(null)}
                        data-testid={`cancel-delete-button-${person.id}`}
                        style={{
                          padding: '8px 16px',
                          backgroundColor: 'transparent',
                          color: 'var(--color-text-secondary)',
                          border: '1px solid var(--color-border)',
                          borderRadius: 'var(--radius-medium)',
                          fontSize: 'var(--text-small)',
                          fontWeight: 'var(--weight-semibold)',
                          cursor: 'pointer',
                        }}
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setConfirmDeleteId(person.id)}
                      disabled={restoringId === person.id || deletingId === person.id}
                      data-testid={`permanent-delete-button-${person.id}`}
                      style={{
                        padding: '8px 16px',
                        backgroundColor: 'transparent',
                        color: 'var(--color-alert)',
                        border: '1px solid var(--color-alert)',
                        borderRadius: 'var(--radius-medium)',
                        fontSize: 'var(--text-small)',
                        fontWeight: 'var(--weight-semibold)',
                        cursor: restoringId === person.id || deletingId === person.id ? 'not-allowed' : 'pointer',
                        opacity: restoringId === person.id || deletingId === person.id ? 0.5 : 1,
                      }}
                    >
                      Delete Forever
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>

          <Pagination
            currentPage={deletedPeople.page}
            totalPages={deletedPeople.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  );
}
