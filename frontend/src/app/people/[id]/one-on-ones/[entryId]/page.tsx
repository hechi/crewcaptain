'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import {
  getOneOnOneEntry,
  updateOneOnOneEntry,
  deleteOneOnOneEntry,
  getPerson,
  getUserSettings,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import { Person } from '@/types/person';
import { OneOnOneEntry } from '@/types/one-on-one';
import { UserSettings } from '@/types/settings';
import OneOnOneEntryForm, { OneOnOneEntryFormData } from '@/components/one-on-one/OneOnOneEntryForm';
import OneOnOneActionItems from '@/components/one-on-one/OneOnOneActionItems';
import AiPrepAssistant from '@/components/one-on-one/AiPrepAssistant';
import LoadingScreen from '@/components/LoadingScreen';

export default function OneOnOneEntryDetailPage() {
  const { getToken, isAuthenticated, status } = useStableToken();
  const router = useRouter();
  const params = useParams();
  const personId = params.id as string;
  const entryId = params.entryId as string;

  const [person, setPerson] = useState<Person | null>(null);
  const [entry, setEntry] = useState<OneOnOneEntry | null>(null);
  const [settings, setSettings] = useState<UserSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [lastAddedSuggestion, setLastAddedSuggestion] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const [personResult, entryResult, settingsResult] = await Promise.all([
        getPerson(token, personId),
        getOneOnOneEntry(token, personId, entryId),
        getUserSettings(token).catch(() => null),
      ]);
      setPerson(personResult);
      setEntry(entryResult);
      setSettings(settingsResult);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load entry');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated, personId, entryId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSubmit = async (data: OneOnOneEntryFormData) => {
    const token = getToken();
    if (!token) return;
    setIsSubmitting(true);
    setError(null);
    try {
      const updated = await updateOneOnOneEntry(token, personId, entryId, {
        meetingDate: data.meetingDate,
        agendaItems: data.agendaItems.map((item) => ({
          text: item.text,
          checked: item.checked,
        })),
        notesMarkdown: data.notesMarkdown,
        outcomesMarkdown: data.outcomesMarkdown,
        sensitive: data.sensitive,
      });
      setEntry(updated);
      setIsSubmitting(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update 1:1 entry');
      setIsSubmitting(false);
    }
  };

  const handleAddAiSuggestion = async (text: string) => {
    const token = getToken();
    if (!token || !entry) return;

    // Add the suggestion as a new agenda item to the existing entry
    const newAgendaItems = [
      ...entry.agendaItems.map((item) => ({ text: item.text, checked: item.checked })),
      { text, checked: false },
    ];

    // Update form immediately via externalAgendaItem
    setLastAddedSuggestion(text);

    try {
      const updated = await updateOneOnOneEntry(token, personId, entryId, {
        agendaItems: newAgendaItems,
      });
      setEntry(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add suggestion');
    }
  };

  const handleDelete = async () => {
    const token = getToken();
    if (!token) return;
    setIsDeleting(true);
    setError(null);
    try {
      await deleteOneOnOneEntry(token, personId, entryId);
      router.push(`/people/${personId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete 1:1 entry');
      setIsDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  const handleCancel = () => {
    router.push(`/people/${personId}`);
  };

  if (status === 'loading') {
    return <LoadingScreen message="Loading 1:1 entry" />;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  if (loading) {
    return <LoadingScreen message="Loading 1:1 entry" />;
  }

  if (error && !entry) {
    return (
      <div data-testid="error-message" style={{ color: 'var(--color-alert)', padding: 'var(--space-6)' }}>
        {error}
      </div>
    );
  }

  if (!person || !entry) {
    return <div data-testid="not-found">Entry not found.</div>;
  }

  const meetingDate = new Date(entry.meetingDate);
  const formattedDate = meetingDate.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  const showAiAssistant = settings?.aiEnabled === true;
  const token = getToken();

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: 'var(--space-6)' }}>
      <button
        type="button"
        onClick={handleCancel}
        style={{
          marginBottom: 'var(--space-4)',
          padding: '6px 12px',
          background: 'var(--color-bg-elevated)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
          cursor: 'pointer',
          fontSize: 'var(--text-body)',
          color: 'var(--color-text-secondary)',
          transition: 'border-color 0.2s',
        }}
      >
        ← Back to {person.preferredName || person.name}
      </button>

      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 'var(--space-6)' }}>
        <div>
          <h1 style={{
            margin: '0 0 4px',
            fontSize: 'var(--text-h2)',
            fontWeight: 'var(--weight-bold)',
            fontFamily: 'var(--font-heading)',
            color: 'var(--color-text-primary)',
            letterSpacing: '-0.3px',
          }}>
            1:1 — {formattedDate}
          </h1>
          <p style={{ margin: 0, fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
            with {person.preferredName || person.name}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowDeleteConfirm(true)}
          data-testid="delete-entry-button"
          style={{
            padding: '8px 16px',
            border: '1px solid var(--color-alert-muted)',
            borderRadius: 'var(--radius-medium)',
            cursor: 'pointer',
            background: 'var(--color-alert-muted)',
            color: 'var(--color-alert)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-medium)',
            fontFamily: 'var(--font-mono)',
          }}
        >
          Delete
        </button>
      </div>

      {/* Delete confirmation */}
      {showDeleteConfirm && (
        <div data-testid="delete-entry-confirmation" style={{ padding: 'var(--space-4)', border: '1px solid var(--color-alert)', borderRadius: 'var(--radius-medium)', marginBottom: 'var(--space-6)', background: 'var(--color-alert-muted)' }}>
          <p style={{ margin: '0 0 12px', fontWeight: 'var(--weight-medium)', color: 'var(--color-text-primary)' }}>Are you sure you want to delete this 1:1 entry?</p>
          <p style={{ margin: '0 0 12px', fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>This action cannot be undone.</p>
          <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
            <button
              type="button"
              onClick={handleDelete}
              disabled={isDeleting}
              data-testid="confirm-delete-entry-button"
              style={{
                padding: '8px 16px',
                backgroundColor: isDeleting ? 'var(--color-alert-muted)' : 'var(--color-alert)',
                color: isDeleting ? 'var(--color-alert)' : '#fff',
                border: 'none',
                borderRadius: 'var(--radius-medium)',
                cursor: isDeleting ? 'not-allowed' : 'pointer',
                fontSize: 'var(--text-body)',
                fontFamily: 'var(--font-mono)',
                boxShadow: isDeleting ? 'none' : 'var(--glow-alert)',
              }}
            >
              {isDeleting ? 'Deleting...' : 'Yes, Delete'}
            </button>
            <button
              type="button"
              onClick={() => setShowDeleteConfirm(false)}
              disabled={isDeleting}
              data-testid="cancel-delete-entry-button"
              style={{
                padding: '8px 16px',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                cursor: 'pointer',
                background: 'var(--color-bg-elevated)',
                fontSize: 'var(--text-body)',
                color: 'var(--color-text-secondary)',
              }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-alert)', marginBottom: 'var(--space-4)', padding: '12px', border: '1px solid var(--color-alert)', borderRadius: 'var(--radius-medium)', background: 'var(--color-alert-muted)' }}>
          {error}
        </div>
      )}

      {/* AI Prep Assistant — only shown when AI is enabled in settings */}
      {showAiAssistant && token && (
        <AiPrepAssistant
          token={token}
          personId={personId}
          onAddSuggestion={handleAddAiSuggestion}
        />
      )}

      {/* Entry Form in edit mode */}
      <OneOnOneEntryForm
        entry={entry}
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        isSubmitting={isSubmitting}
        externalAgendaItem={lastAddedSuggestion}
        actionItemsSlot={(() => {
          const formToken = getToken();
          return formToken ? (
            <OneOnOneActionItems
              token={formToken}
              personId={personId}
              entryId={entryId}
            />
          ) : null;
        })()}
      />
    </div>
  );
}
