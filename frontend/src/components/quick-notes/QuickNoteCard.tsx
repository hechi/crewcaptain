'use client';

import { useState } from 'react';
import { QuickNote } from '@/types/quick-note';
import { Person } from '@/types/person';
import { OneOnOneEntry } from '@/types/one-on-one';

interface QuickNoteCardProps {
  quickNote: QuickNote;
  persons: Person[];
  onArchive: (id: string) => void;
  onConvert: (id: string, personId: string) => void;
  onAttach: (id: string, entryId: string) => void;
  onAssignPerson: (id: string, personId: string) => void;
  onDelete: (id: string) => void;
  onFetchEntries?: (personId: string) => Promise<OneOnOneEntry[]>;
}

/**
 * Displays a single quick note with status, text, and action buttons.
 * Includes person picker for assignment and 1:1 entry picker for attaching.
 */
export default function QuickNoteCard({
  quickNote,
  persons,
  onArchive,
  onConvert,
  onAttach,
  onAssignPerson,
  onDelete,
  onFetchEntries,
}: QuickNoteCardProps) {
  const [showPersonPicker, setShowPersonPicker] = useState(false);
  const [showAttachPersonPicker, setShowAttachPersonPicker] = useState(false);
  const [attachSelectedPersonId, setAttachSelectedPersonId] = useState<string | null>(null);
  const [showConvertPersonPicker, setShowConvertPersonPicker] = useState(false);
  const [showEntryPicker, setShowEntryPicker] = useState(false);
  const [entries, setEntries] = useState<OneOnOneEntry[]>([]);
  const [entriesLoading, setEntriesLoading] = useState(false);

  const formattedDate = new Date(quickNote.createdAt).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  const isInbox = quickNote.status === 'INBOX';

  const assignedPerson = persons.find((p) => p.id === quickNote.personId);

  const handleAssignPerson = (personId: string) => {
    onAssignPerson(quickNote.id, personId);
    setShowPersonPicker(false);
  };

  const handleConvertWithPerson = (personId: string) => {
    onConvert(quickNote.id, personId);
    setShowConvertPersonPicker(false);
  };

  const handleConvertClick = () => {
    if (quickNote.personId) {
      onConvert(quickNote.id, quickNote.personId);
    } else {
      setShowConvertPersonPicker(true);
    }
  };

  const handleShowEntryPicker = async () => {
    if (!quickNote.personId) {
      // Show combined person+entry picker flow
      setShowAttachPersonPicker(true);
      return;
    }
    setShowEntryPicker(true);
    if (onFetchEntries) {
      setEntriesLoading(true);
      try {
        const result = await onFetchEntries(quickNote.personId);
        setEntries(result);
      } finally {
        setEntriesLoading(false);
      }
    }
  };

  const handleAttachPersonSelected = async (personId: string) => {
    // Person selected in the attach flow — immediately load their entries
    setShowAttachPersonPicker(false);
    setAttachSelectedPersonId(personId);
    setShowEntryPicker(true);
    if (onFetchEntries) {
      setEntriesLoading(true);
      try {
        const result = await onFetchEntries(personId);
        setEntries(result);
      } finally {
        setEntriesLoading(false);
      }
    }
  };

  const handleAttachToEntry = (entryId: string) => {
    // If we selected a person during this flow, assign first
    if (attachSelectedPersonId && !quickNote.personId) {
      onAssignPerson(quickNote.id, attachSelectedPersonId);
    }
    onAttach(quickNote.id, entryId);
    setShowEntryPicker(false);
    setAttachSelectedPersonId(null);
  };

  return (
    <div
      data-testid={`quick-note-card-${quickNote.id}`}
      style={{
        padding: 'var(--space-4)',
        border: quickNote.sensitive
          ? '1px solid var(--color-warning-muted)'
          : '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
        transition: 'border-color 0.2s',
      }}
    >
      {/* Header: date + status + sensitive badge */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span
            data-testid="quick-note-date"
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            {formattedDate}
          </span>
          {quickNote.sensitive && (
            <span
              data-testid="quick-note-sensitive-badge"
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
          {assignedPerson && (
            <span
              data-testid="quick-note-person-badge"
              style={{
                padding: '2px 8px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-secondary)',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--color-secondary-muted, rgba(168, 85, 247, 0.15))',
                color: 'var(--color-secondary)',
              }}
            >
              {assignedPerson.name}
            </span>
          )}
        </div>
        <span
          data-testid="quick-note-status"
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
          {quickNote.status}
        </span>
      </div>

      {/* Text */}
      <p
        data-testid="quick-note-text"
        style={{
          margin: '0 0 12px',
          fontSize: 'var(--text-body)',
          color: 'var(--color-text-primary)',
          lineHeight: '1.5',
          whiteSpace: 'pre-wrap',
        }}
      >
        {quickNote.text}
      </p>

      {/* Person Picker */}
      {showPersonPicker && (
        <div data-testid="person-picker" style={{ marginBottom: '12px', padding: '8px', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-elevated)' }}>
          <label style={{ display: 'block', fontSize: 'var(--text-caption)', fontFamily: 'var(--font-mono)', color: 'var(--color-text-muted)', marginBottom: '6px' }}>
            Assign to person:
          </label>
          <select
            data-testid="person-picker-select"
            onChange={(e) => { if (e.target.value) handleAssignPerson(e.target.value); }}
            defaultValue=""
            style={{
              width: '100%',
              padding: '6px 10px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              fontSize: 'var(--text-body)',
            }}
          >
            <option value="" disabled>Select a person...</option>
            {persons.map((p) => (
              <option key={p.id} value={p.id}>{p.name}{p.roleTitle ? ` — ${p.roleTitle}` : ''}</option>
            ))}
          </select>
          <button
            type="button"
            onClick={() => setShowPersonPicker(false)}
            style={{ marginTop: '6px', padding: '4px 8px', fontSize: 'var(--text-caption)', border: 'none', background: 'none', color: 'var(--color-text-muted)', cursor: 'pointer' }}
          >
            Cancel
          </button>
        </div>
      )}

      {/* Attach Person Picker — shown when attaching without a person assigned */}
      {showAttachPersonPicker && (
        <div data-testid="attach-person-picker" style={{ marginBottom: '12px', padding: '8px', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-elevated)' }}>
          <label style={{ display: 'block', fontSize: 'var(--text-caption)', fontFamily: 'var(--font-mono)', color: 'var(--color-text-muted)', marginBottom: '6px' }}>
            Select person to see their 1:1s:
          </label>
          <select
            data-testid="attach-person-picker-select"
            onChange={(e) => { if (e.target.value) handleAttachPersonSelected(e.target.value); }}
            defaultValue=""
            style={{
              width: '100%',
              padding: '6px 10px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              fontSize: 'var(--text-body)',
            }}
          >
            <option value="" disabled>Select a person...</option>
            {persons.map((p) => (
              <option key={p.id} value={p.id}>{p.name}{p.roleTitle ? ` — ${p.roleTitle}` : ''}</option>
            ))}
          </select>
          <button
            type="button"
            onClick={() => setShowAttachPersonPicker(false)}
            style={{ marginTop: '6px', padding: '4px 8px', fontSize: 'var(--text-caption)', border: 'none', background: 'none', color: 'var(--color-text-muted)', cursor: 'pointer' }}
          >
            Cancel
          </button>
        </div>
      )}

      {/* Entry Picker */}
      {showEntryPicker && (
        <div data-testid="entry-picker" style={{ marginBottom: '12px', padding: '8px', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-elevated)' }}>
          <label style={{ display: 'block', fontSize: 'var(--text-caption)', fontFamily: 'var(--font-mono)', color: 'var(--color-text-muted)', marginBottom: '6px' }}>
            Attach to 1:1 entry:
          </label>
          {entriesLoading ? (
            <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)' }}>Loading entries...</span>
          ) : entries.length === 0 ? (
            <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)' }}>No 1:1 entries found for this person.</span>
          ) : (
            <select
              data-testid="entry-picker-select"
              onChange={(e) => { if (e.target.value) handleAttachToEntry(e.target.value); }}
              defaultValue=""
              style={{
                width: '100%',
                padding: '6px 10px',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-small)',
                backgroundColor: 'var(--color-bg-elevated)',
                color: 'var(--color-text-primary)',
                fontSize: 'var(--text-body)',
              }}
            >
              <option value="" disabled>Select a 1:1 entry...</option>
              {entries.map((entry) => (
                <option key={entry.id} value={entry.id}>
                  {new Date(entry.meetingDate).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
                  {entry.notesMarkdown ? ` — ${entry.notesMarkdown.substring(0, 40)}...` : ''}
                </option>
              ))}
            </select>
          )}
          <button
            type="button"
            onClick={() => setShowEntryPicker(false)}
            style={{ marginTop: '6px', padding: '4px 8px', fontSize: 'var(--text-caption)', border: 'none', background: 'none', color: 'var(--color-text-muted)', cursor: 'pointer' }}
          >
            Cancel
          </button>
        </div>
      )}

      {/* Convert Person Picker */}
      {showConvertPersonPicker && (
        <div data-testid="convert-person-picker" style={{ marginBottom: '12px', padding: '8px', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-elevated)' }}>
          <label style={{ display: 'block', fontSize: 'var(--text-caption)', fontFamily: 'var(--font-mono)', color: 'var(--color-text-muted)', marginBottom: '6px' }}>
            Create action item for:
          </label>
          <select
            data-testid="convert-person-picker-select"
            onChange={(e) => { if (e.target.value) handleConvertWithPerson(e.target.value); }}
            defaultValue=""
            style={{
              width: '100%',
              padding: '6px 10px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              fontSize: 'var(--text-body)',
            }}
          >
            <option value="" disabled>Select a person...</option>
            {persons.map((p) => (
              <option key={p.id} value={p.id}>{p.name}{p.roleTitle ? ` — ${p.roleTitle}` : ''}</option>
            ))}
          </select>
          <button
            type="button"
            onClick={() => setShowConvertPersonPicker(false)}
            style={{ marginTop: '6px', padding: '4px 8px', fontSize: 'var(--text-caption)', border: 'none', background: 'none', color: 'var(--color-text-muted)', cursor: 'pointer' }}
          >
            Cancel
          </button>
        </div>
      )}

      {/* Actions — only show for INBOX notes */}
      {isInbox && (
        <div data-testid="quick-note-actions" style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button
            type="button"
            onClick={handleShowEntryPicker}
            data-testid="quick-note-attach-btn"
            aria-label="Attach to 1:1"
            style={{
              padding: '4px 10px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-primary-muted)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'transparent',
              color: 'var(--color-primary)',
              cursor: 'pointer',
            }}
          >
            Attach to 1:1
          </button>
          <button
            type="button"
            onClick={handleConvertClick}
            data-testid="quick-note-convert-btn"
            aria-label="Convert to action item"
            style={{
              padding: '4px 10px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-secondary)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'transparent',
              color: 'var(--color-secondary)',
              cursor: 'pointer',
            }}
          >
            → Action Item
          </button>
          {!quickNote.personId && (
            <button
              type="button"
              onClick={() => setShowPersonPicker(true)}
              data-testid="quick-note-assign-btn"
              aria-label="Assign to person"
              style={{
                padding: '4px 10px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-success-muted, rgba(57, 255, 133, 0.15))',
                borderRadius: 'var(--radius-small)',
                backgroundColor: 'transparent',
                color: 'var(--color-success)',
                cursor: 'pointer',
              }}
            >
              Assign Person
            </button>
          )}
          <button
            type="button"
            onClick={() => onArchive(quickNote.id)}
            data-testid="quick-note-archive-btn"
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
          <button
            type="button"
            onClick={() => onDelete(quickNote.id)}
            data-testid="quick-note-delete-btn"
            aria-label="Delete quick note"
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
      )}

      {/* Non-inbox notes only show delete */}
      {!isInbox && (
        <div data-testid="quick-note-actions" style={{ display: 'flex', gap: '8px' }}>
          <button
            type="button"
            onClick={() => onDelete(quickNote.id)}
            data-testid="quick-note-delete-btn"
            aria-label="Delete quick note"
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
      )}
    </div>
  );
}
