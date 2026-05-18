'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import {
  createOneOnOneEntry,
  getOneOnOneSeries,
  getPerson,
  getUserSettings,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import { Person } from '@/types/person';
import { OneOnOneSeries } from '@/types/one-on-one';
import { UserSettings } from '@/types/settings';
import OneOnOneEntryForm, { OneOnOneEntryFormData } from '@/components/one-on-one/OneOnOneEntryForm';
import OneOnOneActionItems from '@/components/one-on-one/OneOnOneActionItems';
import AiPrepAssistant from '@/components/one-on-one/AiPrepAssistant';
import LoadingScreen from '@/components/LoadingScreen';
import { Sparkles } from 'lucide-react';

export default function CreateOneOnOneEntryPage() {
  const { getToken, isAuthenticated, status } = useStableToken();
  const router = useRouter();
  const params = useParams();
  const personId = params.id as string;

  const [person, setPerson] = useState<Person | null>(null);
  const [series, setSeries] = useState<OneOnOneSeries | null>(null);
  const [settings, setSettings] = useState<UserSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pendingSuggestions, setPendingSuggestions] = useState<string[]>([]);

  const fetchData = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const personResult = await getPerson(token, personId);
      setPerson(personResult);

      // Try to fetch series for template prefill
      try {
        const seriesResult = await getOneOnOneSeries(token, personId);
        setSeries(seriesResult);
      } catch {
        // No series configured — that's fine
        setSeries(null);
      }

      // Fetch user settings for AI assistant
      try {
        const settingsResult = await getUserSettings(token);
        setSettings(settingsResult);
      } catch {
        setSettings(null);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated, personId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSubmit = async (data: OneOnOneEntryFormData) => {
    const token = getToken();
    if (!token) return;
    setIsSubmitting(true);
    setError(null);
    try {
      const entry = await createOneOnOneEntry(token, personId, {
        meetingDate: data.meetingDate,
        agendaItems: data.agendaItems.map((item) => ({
          text: item.text,
          checked: item.checked,
        })),
        notesMarkdown: data.notesMarkdown,
        outcomesMarkdown: data.outcomesMarkdown,
        sensitive: data.sensitive,
      });
      // Navigate to the newly created entry detail page
      router.push(`/people/${personId}/one-on-ones/${entry.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create 1:1 entry');
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    router.push(`/people/${personId}`);
  };

  if (status === 'loading') {
    return <LoadingScreen message="Loading" />;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  if (loading) {
    return <LoadingScreen message="Loading 1:1 form" />;
  }

  if (error && !person) {
    return (
      <div data-testid="error-message" style={{ color: 'var(--color-alert)', padding: 'var(--space-6)' }}>
        {error}
      </div>
    );
  }

  if (!person) {
    return <div data-testid="not-found">Person not found.</div>;
  }

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

      <h1 style={{
        margin: '0 0 8px',
        fontSize: 'var(--text-h2)',
        fontWeight: 'var(--weight-bold)',
        fontFamily: 'var(--font-heading)',
        color: 'var(--color-text-primary)',
        letterSpacing: '-0.3px',
      }}>
        New 1:1 with {person.preferredName || person.name}
      </h1>
      <p style={{ margin: '0 0 var(--space-6)', fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
        Record your meeting notes, agenda items, and outcomes.
      </p>

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-alert)', marginBottom: 'var(--space-4)', padding: '12px', border: '1px solid var(--color-alert)', borderRadius: 'var(--radius-medium)', background: 'var(--color-alert-muted)' }}>
          {error}
        </div>
      )}

      {/* AI Prep Assistant — only shown when AI is enabled in settings */}
      {settings?.aiEnabled && (() => {
        const token = getToken();
        return token ? (
          <AiPrepAssistant
            token={token}
            personId={personId}
            onAddSuggestion={(text) => setPendingSuggestions((prev) => [...prev, text])}
          />
        ) : null;
      })()}

      {/* Extract Outcomes button — disabled on create page (entry not yet saved) */}
      {settings?.aiEnabled && (
        <div style={{ marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
          <span
            data-testid="extract-outcomes-hint"
            style={{
              fontSize: 'var(--text-small)',
              color: 'var(--color-text-secondary)',
              fontFamily: 'var(--font-mono)',
            }}
          >
            Available after saving
          </span>
          <button
            type="button"
            data-testid="extract-outcomes-btn-disabled"
            disabled
            aria-label="Extract outcomes from notes"
            title="Save the entry first to extract outcomes"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 16px',
              borderRadius: 'var(--radius-medium)',
              border: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-secondary)',
              cursor: 'not-allowed',
              fontWeight: 'var(--weight-medium)',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              opacity: 0.5,
            }}
          >
            <Sparkles size={14} />
            Extract Outcomes
          </button>
        </div>
      )}

      <OneOnOneEntryForm
        templateMarkdown={series?.templateMarkdown}
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        isSubmitting={isSubmitting}
        externalAgendaItem={pendingSuggestions[pendingSuggestions.length - 1] || null}
        actionItemsSlot={(() => {
          const token = getToken();
          return token ? (
            <OneOnOneActionItems
              token={token}
              personId={personId}
            />
          ) : null;
        })()}
      />
    </div>
  );
}
