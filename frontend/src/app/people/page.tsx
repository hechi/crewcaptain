'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useSession } from 'next-auth/react';
import { Person, PaginatedResponse, MoraleStatus } from '@/types/person';
import { listPersons } from '@/lib/api-client';
import PersonCard from '@/components/PersonCard';
import FilterBar from '@/components/FilterBar';
import Pagination from '@/components/Pagination';
import EmptyState from '@/components/EmptyState';

export default function PeopleListPage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  const [people, setPeople] = useState<PaginatedResponse<Person> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<{ tag: string; morale: MoraleStatus | '' }>({ tag: '', morale: '' });

  const fetchPeople = useCallback(async () => {
    if (status !== 'authenticated' || !session?.accessToken) return;

    setLoading(true);
    setError(null);
    try {
      const result = await listPersons(session.accessToken as string, {
        page,
        size: 20,
        tag: filters.tag || undefined,
        morale: (filters.morale || undefined) as MoraleStatus | undefined,
      });
      setPeople(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load people');
    } finally {
      setLoading(false);
    }
  }, [session, status, page, filters]);

  useEffect(() => {
    fetchPeople();
  }, [fetchPeople]);

  if (status === 'loading') {
    return <div data-testid="loading">Loading...</div>;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: 'var(--space-6)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-6)' }}>
        <h1 style={{ margin: 0, fontSize: 'var(--text-h2)', fontFamily: 'var(--font-heading)', fontWeight: 'var(--weight-bold)', color: 'var(--color-primary)' }}>People</h1>
        <button
          type="button"
          onClick={() => router.push('/people/new')}
          data-testid="add-person-button"
          style={{
            padding: '10px 20px',
            backgroundColor: 'var(--color-accent)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-medium)',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          Add Person
        </button>
      </div>

      <FilterBar
        onFilterChange={(newFilters) => {
          setFilters(newFilters);
          setPage(0);
        }}
        initialTag={filters.tag}
        initialMorale={filters.morale}
      />

      {loading && <div data-testid="loading">Loading...</div>}

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-error)', padding: 'var(--space-4)' }}>
          {error}
        </div>
      )}

      {!loading && !error && people && people.content.length === 0 && (
        <EmptyState
          message="No people in your directory yet."
          actionLabel="Add your first person"
          onAction={() => router.push('/people/new')}
        />
      )}

      {!loading && !error && people && people.content.length > 0 && (
        <>
          <div data-testid="people-list" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
            {people.content.map((person) => (
              <PersonCard key={person.id} person={person} />
            ))}
          </div>

          <Pagination
            currentPage={people.page}
            totalPages={people.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  );
}
