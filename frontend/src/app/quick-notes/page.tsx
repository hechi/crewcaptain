'use client';

import { useEffect, useState, useCallback } from 'react';
import { QuickNoteStatus, CreateQuickNoteRequest, PaginatedQuickNoteResponse } from '@/types/quick-note';
import { Person, PaginatedResponse } from '@/types/person';
import { OneOnOneEntry } from '@/types/one-on-one';
import {
  listQuickNotes,
  createQuickNote,
  archiveQuickNote,
  convertQuickNote,
  attachQuickNote,
  assignQuickNoteToPerson,
  deleteQuickNote,
  listPersons,
  listOneOnOneEntries,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import QuickNoteList from '@/components/quick-notes/QuickNoteList';
import Pagination from '@/components/Pagination';

export default function QuickNotesPage() {
  const { getToken, isAuthenticated, status } = useStableToken();

  const [notes, setNotes] = useState<PaginatedQuickNoteResponse | null>(null);
  const [persons, setPersons] = useState<Person[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [statusFilter, setStatusFilter] = useState<QuickNoteStatus | null>('INBOX');
  const [page, setPage] = useState(0);

  const fetchNotes = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await listQuickNotes(token, {
        status: statusFilter || undefined,
        page,
        size: 20,
      });
      setNotes(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load quick notes');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated, statusFilter, page]);

  const fetchPersons = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;
    try {
      const result = await listPersons(token, { size: 100 });
      setPersons(result.content);
    } catch {
      // Non-critical — persons list is for the picker
    }
  }, [getToken, isAuthenticated]);

  useEffect(() => {
    fetchNotes();
  }, [fetchNotes]);

  useEffect(() => {
    fetchPersons();
  }, [fetchPersons]);

  const handleCreate = async (data: CreateQuickNoteRequest) => {
    const token = getToken();
    if (!token) return;
    setSubmitting(true);
    try {
      await createQuickNote(token, data);
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create quick note');
    } finally {
      setSubmitting(false);
    }
  };

  const handleArchive = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await archiveQuickNote(token, id);
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to archive quick note');
    }
  };

  const handleConvert = async (id: string, personId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await convertQuickNote(token, id, { personId });
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to convert quick note');
    }
  };

  const handleAttach = async (id: string, entryId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await attachQuickNote(token, id, { entryId });
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to attach quick note');
    }
  };

  const handleAssignPerson = async (id: string, personId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await assignQuickNoteToPerson(token, id, { personId });
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to assign quick note to person');
    }
  };

  const handleDelete = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await deleteQuickNote(token, id);
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete quick note');
    }
  };

  const handleFetchEntries = async (personId: string): Promise<OneOnOneEntry[]> => {
    const token = getToken();
    if (!token) return [];
    try {
      const result = await listOneOnOneEntries(token, personId, 0, 20);
      return result.content;
    } catch {
      return [];
    }
  };

  const handleStatusFilterChange = (newStatus: QuickNoteStatus | null) => {
    setStatusFilter(newStatus);
    setPage(0);
  };

  if (status === 'loading') {
    return <div data-testid="loading">Loading...</div>;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: 'var(--space-6)' }}>
      <h1
        style={{
          margin: '0 0 var(--space-6)',
          fontSize: 'var(--text-h2)',
          fontWeight: '700',
          fontFamily: 'var(--font-heading)',
          color: 'var(--color-text-primary)',
          letterSpacing: '-0.3px',
        }}
      >
        Quick Notes
      </h1>

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-alert)', marginBottom: 'var(--space-4)' }}>
          {error}
        </div>
      )}

      {loading && !notes ? (
        <div data-testid="notes-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
          Loading quick notes...
        </div>
      ) : (
        <>
          <QuickNoteList
            quickNotes={notes?.content || []}
            persons={persons}
            onCreateNote={handleCreate}
            onArchive={handleArchive}
            onConvert={handleConvert}
            onAttach={handleAttach}
            onAssignPerson={handleAssignPerson}
            onDelete={handleDelete}
            onFetchEntries={handleFetchEntries}
            isSubmitting={submitting}
            statusFilter={statusFilter}
            onStatusFilterChange={handleStatusFilterChange}
          />

          {notes && notes.totalPages > 1 && (
            <div style={{ marginTop: 'var(--space-4)' }}>
              <Pagination
                currentPage={notes.page}
                totalPages={notes.totalPages}
                onPageChange={setPage}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}
