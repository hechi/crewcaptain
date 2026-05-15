'use client';

import { useEffect, useState, useCallback } from 'react';
import { QuickNoteStatus, CreateQuickNoteRequest, PaginatedQuickNoteResponse, QuickNote } from '@/types/quick-note';
import {
  listQuickNotes,
  createQuickNote,
  archiveQuickNote,
  deleteQuickNote,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import QuickNoteForm from '@/components/quick-notes/QuickNoteForm';
import LoadingScreen from '@/components/LoadingScreen';
import Pagination from '@/components/Pagination';
import EmptyState from '@/components/EmptyState';

export default function MyNotesPage() {
  const { getToken, isAuthenticated, status } = useStableToken();

  const [notes, setNotes] = useState<PaginatedQuickNoteResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [statusFilter, setStatusFilter] = useState<QuickNoteStatus | null>(null);
  const [page, setPage] = useState(0);

  const fetchNotes = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await listQuickNotes(token, {
        selfAssigned: true,
        status: statusFilter || undefined,
        page,
        size: 20,
      });
      setNotes(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load my notes');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated, statusFilter, page]);

  useEffect(() => {
    fetchNotes();
  }, [fetchNotes]);

  const handleCreate = async (data: CreateQuickNoteRequest) => {
    const token = getToken();
    if (!token) return;
    setSubmitting(true);
    try {
      await createQuickNote(token, { ...data, selfAssigned: true });
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create note');
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
      setError(err instanceof Error ? err.message : 'Failed to archive note');
    }
  };

  const handleDelete = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await deleteQuickNote(token, id);
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete note');
    }
  };

  const handleStatusFilterChange = (newStatus: QuickNoteStatus | null) => {
    setStatusFilter(newStatus);
    setPage(0);
  };

  if (status === 'loading') {
    return <LoadingScreen message="Loading my notes" />;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  const statuses: (QuickNoteStatus | null)[] = [null, 'INBOX', 'ARCHIVED'];
  const statusLabels: Record<string, string> = {
    '': 'All',
    INBOX: 'Active',
    ARCHIVED: 'Archived',
  };

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
        My Notes
      </h1>

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-alert)', marginBottom: 'var(--space-4)' }}>
          {error}
        </div>
      )}

      {/* Create form */}
      <div style={{ marginBottom: 'var(--space-6)' }}>
        <QuickNoteForm onSubmit={handleCreate} isSubmitting={submitting} />
      </div>

      {/* Status filter */}
      <div
        data-testid="my-notes-status-filter"
        style={{ display: 'flex', gap: '8px', marginBottom: 'var(--space-4)', flexWrap: 'wrap' }}
      >
        {statuses.map((s) => (
          <button
            key={s ?? 'all'}
            type="button"
            onClick={() => handleStatusFilterChange(s)}
            data-testid={`filter-${s ?? 'all'}`}
            style={{
              padding: '4px 12px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: statusFilter === s
                ? '1px solid var(--color-primary)'
                : '1px solid var(--color-border)',
              borderRadius: 'var(--radius-full)',
              backgroundColor: statusFilter === s ? 'var(--color-primary-muted)' : 'transparent',
              color: statusFilter === s ? 'var(--color-primary)' : 'var(--color-text-secondary)',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            {statusLabels[s ?? '']}
          </button>
        ))}
      </div>

      {loading && !notes ? (
        <div data-testid="notes-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
          Loading my notes...
        </div>
      ) : (
        <>
          {notes && notes.content.length === 0 ? (
            <EmptyState message="No personal notes yet — capture something above!" />
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              {notes?.content.map((note) => (
                <MyNoteCard
                  key={note.id}
                  note={note}
                  onArchive={handleArchive}
                  onDelete={handleDelete}
                />
              ))}
            </div>
          )}

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

interface MyNoteCardProps {
  note: QuickNote;
  onArchive: (id: string) => void;
  onDelete: (id: string) => void;
}

function MyNoteCard({ note, onArchive, onDelete }: MyNoteCardProps) {
  const formattedDate = new Date(note.createdAt).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  const isInbox = note.status === 'INBOX';

  return (
    <div
      data-testid={`my-note-card-${note.id}`}
      style={{
        padding: 'var(--space-4)',
        border: note.sensitive
          ? '1px solid var(--color-warning-muted)'
          : '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
        transition: 'border-color 0.2s',
      }}
    >
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span
            data-testid="my-note-date"
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            {formattedDate}
          </span>
          {note.sensitive && (
            <span
              data-testid="my-note-sensitive-badge"
              style={{
                padding: '2px 6px',
                fontSize: '10px',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-warning)',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--color-warning-muted)',
                color: 'var(--color-warning)',
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
              }}
            >
              Sensitive
            </span>
          )}
        </div>
        <span
          data-testid="my-note-status"
          style={{
            padding: '2px 8px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-full)',
            color: 'var(--color-text-secondary)',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          {note.status}
        </span>
      </div>

      {/* Text */}
      <p
        data-testid="my-note-text"
        style={{
          margin: '0 0 12px',
          fontSize: 'var(--text-body)',
          color: 'var(--color-text-primary)',
          lineHeight: '1.5',
          whiteSpace: 'pre-wrap',
        }}
      >
        {note.text}
      </p>

      {/* Actions */}
      <div data-testid="my-note-actions" style={{ display: 'flex', gap: '8px' }}>
        {isInbox && (
          <button
            type="button"
            onClick={() => onArchive(note.id)}
            data-testid="my-note-archive-btn"
            aria-label="Archive"
            style={{
              padding: '4px 10px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'transparent',
              color: 'var(--color-text-secondary)',
              cursor: 'pointer',
            }}
          >
            Archive
          </button>
        )}
        <button
          type="button"
          onClick={() => onDelete(note.id)}
          data-testid="my-note-delete-btn"
          aria-label="Delete note"
          style={{
            padding: '4px 10px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-alert-muted)',
            borderRadius: 'var(--radius-small)',
            backgroundColor: 'transparent',
            color: 'var(--color-alert)',
            cursor: 'pointer',
          }}
        >
          Delete
        </button>
      </div>
    </div>
  );
}
