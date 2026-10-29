'use client';

import { useEffect, useState, useCallback } from 'react';
import { QuickNoteStatus, CreateQuickNoteRequest, PaginatedQuickNoteResponse } from '@/types/quick-note';
import {
  listQuickNotes,
  createQuickNote,
  archiveQuickNote,
  deleteQuickNote,
  convertQuickNote,
  updateQuickNote,
  moveQuickNote,
  reorderQuickNote,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import QuickNoteForm from '@/components/quick-notes/QuickNoteForm';
import MyNoteCard from '@/components/quick-notes/MyNoteCard';
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
  const [tagFilter, setTagFilter] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  // Drag-and-drop reordering state.
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [dropTargetId, setDropTargetId] = useState<string | null>(null);

  const fetchNotes = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await listQuickNotes(token, {
        selfAssigned: true,
        status: statusFilter || undefined,
        tag: tagFilter || undefined,
        page,
        size: 20,
      });
      setNotes(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load my notes');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated, statusFilter, tagFilter, page]);

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

  const handleConvert = async (id: string, dueDate?: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await convertQuickNote(token, id, { dueDate: dueDate || undefined });
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to convert note to a TODO');
    }
  };

  const handleUpdate = async (id: string, text: string, tag: string | null) => {
    const token = getToken();
    if (!token) return;
    try {
      await updateQuickNote(token, id, { text, tag, updateTag: true });
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update note');
    }
  };

  const handleMove = async (id: string, direction: 'UP' | 'DOWN') => {
    const token = getToken();
    if (!token) return;
    try {
      await moveQuickNote(token, id, direction);
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to move note');
    }
  };

  const handleDragStart = (id: string) => setDraggingId(id);

  const handleDragOverNote = (id: string) => {
    if (draggingId && id !== draggingId) setDropTargetId(id);
  };

  const handleDragEnd = () => {
    setDraggingId(null);
    setDropTargetId(null);
  };

  // Drop the dragged note onto a target note: reorder to the target's index and persist.
  const handleDropOnNote = async (targetId: string) => {
    const sourceId = draggingId;
    setDraggingId(null);
    setDropTargetId(null);
    if (!sourceId || sourceId === targetId) return;

    const targetIndex = (notes?.content ?? []).findIndex((n) => n.id === targetId);
    if (targetIndex < 0) return;

    const token = getToken();
    if (!token) return;
    try {
      await reorderQuickNote(token, sourceId, targetIndex);
      fetchNotes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reorder note');
    }
  };

  const handleStatusFilterChange = (newStatus: QuickNoteStatus | null) => {
    setStatusFilter(newStatus);
    setPage(0);
  };

  const handleTagFilterChange = (newTag: string | null) => {
    setTagFilter((current) => (current === newTag ? null : newTag));
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

      {/* Tag filter */}
      {(() => {
        const tags = Array.from(
          new Set((notes?.content ?? []).map((n) => n.tag).filter((t): t is string => !!t))
        );
        const hasTags = tags.length > 0 || tagFilter !== null;
        if (!hasTags) return null;
        const visibleTags = tagFilter && !tags.includes(tagFilter) ? [tagFilter, ...tags] : tags;
        return (
          <div
            data-testid="my-notes-tag-filter"
            style={{ display: 'flex', gap: '8px', marginBottom: 'var(--space-4)', flexWrap: 'wrap', alignItems: 'center' }}
          >
            <span
              style={{
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                color: 'var(--color-text-muted)',
              }}
            >
              Tags:
            </span>
            {visibleTags.map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => handleTagFilterChange(t)}
                data-testid={`tag-filter-${t}`}
                style={{
                  padding: '4px 12px',
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  border: tagFilter === t
                    ? '1px solid var(--color-secondary)'
                    : '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: tagFilter === t ? 'var(--color-secondary-hover)' : 'transparent',
                  color: tagFilter === t ? 'var(--color-bg-base)' : 'var(--color-text-secondary)',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
              >
                #{t}
              </button>
            ))}
          </div>
        );
      })()}

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
              {notes?.content.map((note, index) => {
                // Reordering applies to the active (non-archived) list only.
                const reorderable = statusFilter !== 'ARCHIVED';
                return (
                  <MyNoteCard
                    key={note.id}
                    note={note}
                    isFirst={index === 0}
                    isLast={index === (notes?.content.length ?? 0) - 1}
                    isDragging={draggingId === note.id}
                    isDropTarget={dropTargetId === note.id}
                    onArchive={handleArchive}
                    onDelete={handleDelete}
                    onConvert={handleConvert}
                    onEdit={handleUpdate}
                    onMove={reorderable ? handleMove : undefined}
                    onDragStart={reorderable ? handleDragStart : undefined}
                    onDragOverNote={reorderable ? handleDragOverNote : undefined}
                    onDragEnd={reorderable ? handleDragEnd : undefined}
                    onDropOnNote={reorderable ? handleDropOnNote : undefined}
                  />
                );
              })}
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
