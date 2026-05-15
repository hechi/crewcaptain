'use client';

import { QuickNote, QuickNoteStatus, CreateQuickNoteRequest } from '@/types/quick-note';
import { Person } from '@/types/person';
import { OneOnOneEntry } from '@/types/one-on-one';
import QuickNoteCard from './QuickNoteCard';
import QuickNoteForm from './QuickNoteForm';
import EmptyState from '@/components/EmptyState';

interface QuickNoteListProps {
  quickNotes: QuickNote[];
  persons: Person[];
  onCreateNote: (data: CreateQuickNoteRequest) => void;
  onArchive: (id: string) => void;
  onConvert: (id: string, personId: string) => void;
  onAttach: (id: string, entryId: string) => void;
  onAssignPerson: (id: string, personId: string) => void;
  onAssignSelf?: (id: string) => void;
  onDelete: (id: string) => void;
  onFetchEntries?: (personId: string) => Promise<OneOnOneEntry[]>;
  isSubmitting?: boolean;
  statusFilter: QuickNoteStatus | null;
  onStatusFilterChange: (status: QuickNoteStatus | null) => void;
}

/**
 * Displays the Quick Notes inbox with create form, status filter, and list of notes.
 */
export default function QuickNoteList({
  quickNotes,
  persons,
  onCreateNote,
  onArchive,
  onConvert,
  onAttach,
  onAssignPerson,
  onAssignSelf,
  onDelete,
  onFetchEntries,
  isSubmitting = false,
  statusFilter,
  onStatusFilterChange,
}: QuickNoteListProps) {
  const statuses: (QuickNoteStatus | null)[] = [null, 'INBOX', 'ATTACHED', 'CONVERTED', 'ARCHIVED'];
  const statusLabels: Record<string, string> = {
    '': 'All',
    INBOX: 'Inbox',
    ATTACHED: 'Attached',
    CONVERTED: 'Converted',
    ARCHIVED: 'Archived',
  };

  return (
    <div data-testid="quick-note-list">
      {/* Create form */}
      <div style={{ marginBottom: 'var(--space-6)' }}>
        <QuickNoteForm onSubmit={onCreateNote} isSubmitting={isSubmitting} />
      </div>

      {/* Status filter */}
      <div
        data-testid="quick-note-status-filter"
        style={{ display: 'flex', gap: '8px', marginBottom: 'var(--space-4)', flexWrap: 'wrap' }}
      >
        {statuses.map((s) => (
          <button
            key={s ?? 'all'}
            type="button"
            onClick={() => onStatusFilterChange(s)}
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

      {/* Notes list */}
      {quickNotes.length === 0 ? (
        <EmptyState message="No quick notes yet — capture something above!" />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          {quickNotes.map((note) => (
            <QuickNoteCard
              key={note.id}
              quickNote={note}
              persons={persons}
              onArchive={onArchive}
              onConvert={onConvert}
              onAttach={onAttach}
              onAssignPerson={onAssignPerson}
              onAssignSelf={onAssignSelf}
              onDelete={onDelete}
              onFetchEntries={onFetchEntries}
            />
          ))}
        </div>
      )}
    </div>
  );
}
