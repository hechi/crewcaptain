'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useSession } from 'next-auth/react';
import {
  createOneOnOneEntry,
  getOneOnOneSeries,
  getPerson,
} from '@/lib/api-client';
import { Person } from '@/types/person';
import { OneOnOneSeries } from '@/types/one-on-one';
import OneOnOneEntryForm, { OneOnOneEntryFormData } from '@/components/one-on-one/OneOnOneEntryForm';

export default function CreateOneOnOneEntryPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const params = useParams();
  const personId = params.id as string;

  const [person, setPerson] = useState<Person | null>(null);
  const [series, setSeries] = useState<OneOnOneSeries | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const token = session?.accessToken as string;

  const fetchData = useCallback(async () => {
    if (status !== 'authenticated' || !token) return;

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
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data');
    } finally {
      setLoading(false);
    }
  }, [token, status, personId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSubmit = async (data: OneOnOneEntryFormData) => {
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
    return <div data-testid="loading">Loading...</div>;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  if (loading) {
    return <div data-testid="loading">Loading...</div>;
  }

  if (error && !person) {
    return (
      <div data-testid="error-message" style={{ color: 'var(--color-error)', padding: 'var(--space-6)' }}>
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
          background: 'var(--color-neutral-surface)',
          border: '1px solid var(--color-neutral-border)',
          borderRadius: 'var(--radius-medium)',
          cursor: 'pointer',
          fontSize: 'var(--text-body)',
          color: 'var(--color-neutral-text-secondary)',
        }}
      >
        ← Back to {person.preferredName || person.name}
      </button>

      <h1 style={{ margin: '0 0 8px', fontSize: 'var(--text-h2)', fontWeight: 'var(--weight-bold)', color: 'var(--color-primary)' }}>
        New 1:1 with {person.preferredName || person.name}
      </h1>
      <p style={{ margin: '0 0 var(--space-6)', fontSize: 'var(--text-body)', color: 'var(--color-neutral-text-muted)' }}>
        Record your meeting notes, agenda items, and outcomes.
      </p>

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-error)', marginBottom: 'var(--space-4)', padding: '12px', border: '1px solid var(--color-error-border)', borderRadius: 'var(--radius-medium)', background: 'var(--color-error-bg)' }}>
          {error}
        </div>
      )}

      <OneOnOneEntryForm
        templateMarkdown={series?.templateMarkdown}
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        isSubmitting={isSubmitting}
      />
    </div>
  );
}
