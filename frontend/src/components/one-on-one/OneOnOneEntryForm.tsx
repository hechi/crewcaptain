'use client';

import { useState } from 'react';
import { OneOnOneEntry } from '@/types/one-on-one';
import AgendaItemList, { AgendaItemInput } from './AgendaItemList';
import MarkdownEditor from './MarkdownEditor';
import SensitiveToggle from './SensitiveToggle';

interface OneOnOneEntryFormProps {
  /** Existing entry for edit mode; undefined for create mode */
  entry?: OneOnOneEntry;
  /** Template markdown to prefill notes in create mode */
  templateMarkdown?: string | null;
  /** Called on form submission with form data */
  onSubmit: (data: OneOnOneEntryFormData) => void;
  /** Called when user cancels */
  onCancel?: () => void;
  /** Whether the form is currently submitting */
  isSubmitting?: boolean;
}

export interface OneOnOneEntryFormData {
  meetingDate: string;
  agendaItems: { text: string; checked: boolean }[];
  notesMarkdown: string | null;
  outcomesMarkdown: string | null;
  sensitive: boolean;
}

/**
 * Form for creating or editing a 1:1 entry.
 * Includes date/time picker, AgendaItemList, MarkdownEditor for notes/outcomes,
 * and SensitiveToggle. Validates meeting date presence before submission.
 */
export default function OneOnOneEntryForm({
  entry,
  templateMarkdown,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: OneOnOneEntryFormProps) {
  const isEditMode = !!entry;

  // Initialize form state
  const [meetingDate, setMeetingDate] = useState<string>(() => {
    if (entry?.meetingDate) {
      // Convert ISO string to datetime-local format
      const date = new Date(entry.meetingDate);
      return toDatetimeLocalString(date);
    }
    return toDatetimeLocalString(new Date());
  });

  const [agendaItems, setAgendaItems] = useState<AgendaItemInput[]>(() => {
    if (entry?.agendaItems) {
      return entry.agendaItems.map((item) => ({
        id: item.id,
        text: item.text,
        checked: item.checked,
      }));
    }
    return [];
  });

  const [notesMarkdown, setNotesMarkdown] = useState<string>(() => {
    if (entry?.notesMarkdown) return entry.notesMarkdown;
    if (!isEditMode && templateMarkdown) return templateMarkdown;
    return '';
  });

  const [outcomesMarkdown, setOutcomesMarkdown] = useState<string>(
    entry?.outcomesMarkdown || ''
  );

  const [sensitive, setSensitive] = useState<boolean>(entry?.sensitive || false);
  const [dateError, setDateError] = useState<string | null>(null);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    // Validate meeting date
    if (!meetingDate.trim()) {
      setDateError('Meeting date is required');
      return;
    }

    setDateError(null);

    const formData: OneOnOneEntryFormData = {
      meetingDate: new Date(meetingDate).toISOString(),
      agendaItems: agendaItems.map((item) => ({
        text: item.text,
        checked: item.checked,
      })),
      notesMarkdown: notesMarkdown || null,
      outcomesMarkdown: outcomesMarkdown || null,
      sensitive,
    };

    onSubmit(formData);
  }

  return (
    <form
      data-testid="one-on-one-entry-form"
      onSubmit={handleSubmit}
      style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)' }}
    >
      {/* Meeting Date */}
      <div>
        <label
          htmlFor="meeting-date"
          style={{ display: 'block', fontSize: 'var(--text-body)', fontWeight: 'var(--weight-medium)', marginBottom: '6px', color: 'var(--color-neutral-text)' }}
        >
          Meeting Date *
        </label>
        <input
          id="meeting-date"
          type="datetime-local"
          value={meetingDate}
          onChange={(e) => {
            setMeetingDate(e.target.value);
            if (dateError) setDateError(null);
          }}
          data-testid="meeting-date-input"
          aria-required="true"
          aria-invalid={!!dateError}
          aria-describedby={dateError ? 'meeting-date-error' : undefined}
          style={{
            width: '100%',
            padding: '8px 12px',
            border: `1px solid ${dateError ? 'var(--color-error)' : 'var(--color-neutral-border)'}`,
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            boxSizing: 'border-box',
          }}
        />
        {dateError && (
          <p
            id="meeting-date-error"
            data-testid="meeting-date-error"
            role="alert"
            style={{ margin: '4px 0 0', fontSize: 'var(--text-caption)', color: 'var(--color-error)' }}
          >
            {dateError}
          </p>
        )}
      </div>

      {/* Agenda Items */}
      <AgendaItemList items={agendaItems} onChange={setAgendaItems} />

      {/* Notes */}
      <div>
        <label
          style={{ display: 'block', fontSize: 'var(--text-body)', fontWeight: 'var(--weight-medium)', marginBottom: '6px', color: 'var(--color-neutral-text)' }}
        >
          Notes
        </label>
        <MarkdownEditor
          value={notesMarkdown}
          onChange={setNotesMarkdown}
          placeholder="Write your meeting notes in Markdown..."
          label="Notes"
        />
      </div>

      {/* Outcomes */}
      <div>
        <label
          style={{ display: 'block', fontSize: 'var(--text-body)', fontWeight: 'var(--weight-medium)', marginBottom: '6px', color: 'var(--color-neutral-text)' }}
        >
          Outcomes
        </label>
        <MarkdownEditor
          value={outcomesMarkdown}
          onChange={setOutcomesMarkdown}
          placeholder="Record outcomes and action items..."
          label="Outcomes"
        />
      </div>

      {/* Sensitive Toggle */}
      <SensitiveToggle checked={sensitive} onChange={setSensitive} />

      {/* Actions */}
      <div style={{ display: 'flex', gap: 'var(--space-3)', justifyContent: 'flex-end', paddingTop: '8px' }}>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            data-testid="entry-form-cancel"
            style={{
              padding: '10px 20px',
              border: '1px solid var(--color-neutral-border)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              fontWeight: 'var(--weight-medium)',
              cursor: 'pointer',
              backgroundColor: 'var(--color-neutral-surface)',
              color: 'var(--color-neutral-text)',
            }}
          >
            Cancel
          </button>
        )}
        <button
          type="submit"
          disabled={isSubmitting}
          data-testid="entry-form-submit"
          style={{
            padding: '10px 20px',
            backgroundColor: isSubmitting ? 'var(--color-accent-light)' : 'var(--color-accent)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-medium)',
            cursor: isSubmitting ? 'not-allowed' : 'pointer',
          }}
        >
          {isSubmitting ? 'Saving...' : isEditMode ? 'Update Entry' : 'Create Entry'}
        </button>
      </div>
    </form>
  );
}

/** Convert a Date to a datetime-local input value string (YYYY-MM-DDTHH:mm) */
function toDatetimeLocalString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}
