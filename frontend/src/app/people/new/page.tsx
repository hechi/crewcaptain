'use client';

import { useRouter } from 'next/navigation';
import { useSession } from 'next-auth/react';
import { createPerson } from '@/lib/api-client';
import PersonForm from '@/components/PersonForm';
import { useState } from 'react';

export default function CreatePersonPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (data: {
    name: string;
    preferredName?: string;
    roleTitle?: string;
    timezone?: string;
    startDate?: string;
    email?: string;
    tags?: string[];
  }) => {
    if (!session?.accessToken) return;

    setError(null);
    try {
      const person = await createPerson(session.accessToken as string, data);
      router.push(`/people/${person.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create person');
    }
  };

  if (status === 'loading') {
    return <div data-testid="loading">Loading...</div>;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto', padding: 'var(--space-6)' }}>
      <button
        type="button"
        onClick={() => router.push('/people')}
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
        ← Back to People
      </button>

      <h1 style={{ margin: '0 0 var(--space-6)', fontSize: 'var(--text-h2)', fontWeight: 'var(--weight-bold)', color: 'var(--color-primary)' }}>Add New Person</h1>

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-error)', marginBottom: 'var(--space-4)' }}>
          {error}
        </div>
      )}

      <PersonForm
        mode="create"
        onSubmit={handleSubmit}
        onCancel={() => router.push('/people')}
      />
    </div>
  );
}
