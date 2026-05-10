'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useSession } from 'next-auth/react';
import { Person, PaginatedResponse, MoraleStatus } from '@/types/person';
import { Workspace } from '@/types/workspace';
import { listPersons, listWorkspaces } from '@/lib/api-client';
import PersonCard from '@/components/PersonCard';
import FilterBar from '@/components/FilterBar';
import WorkspaceSelector from '@/components/workspace/WorkspaceSelector';
import Pagination from '@/components/Pagination';
import EmptyState from '@/components/EmptyState';
import CsvImportModal from '@/components/CsvImportModal';

export default function PeopleListPage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  const [people, setPeople] = useState<PaginatedResponse<Person> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<{ tag: string; morale: MoraleStatus | '' }>({ tag: '', morale: '' });
  const [workspaceFilter, setWorkspaceFilter] = useState<string | null>(null);
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [showImportModal, setShowImportModal] = useState(false);

  const fetchWorkspaces = useCallback(async () => {
    if (status !== 'authenticated' || !session?.accessToken) return;
    try {
      const result = await listWorkspaces(session.accessToken as string);
      setWorkspaces(result);
    } catch {
      // Silently fail — workspace feature is opt-in
    }
  }, [session, status]);

  useEffect(() => {
    fetchWorkspaces();
  }, [fetchWorkspaces]);

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
        workspace: workspaceFilter || undefined,
      });
      setPeople(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load people');
    } finally {
      setLoading(false);
    }
  }, [session, status, page, filters, workspaceFilter]);

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
        <h1 style={{
          margin: 0,
          fontSize: 'var(--text-h2)',
          fontWeight: 'var(--weight-bold)',
          fontFamily: 'var(--font-heading)',
          color: 'var(--color-text-primary)',
          letterSpacing: '-0.3px',
        }}>
          People
        </h1>
        <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
          <button
            type="button"
            onClick={() => router.push('/people/trash')}
            data-testid="trash-button"
            style={{
              padding: '10px 20px',
              backgroundColor: 'transparent',
              color: 'var(--color-text-secondary)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              fontWeight: 'var(--weight-semibold)',
              fontFamily: 'var(--font-mono)',
              cursor: 'pointer',
              transition: 'box-shadow 0.2s',
            }}
          >
            Trash
          </button>
          <button
            type="button"
            onClick={() => setShowImportModal(true)}
            data-testid="import-csv-button"
            style={{
              padding: '10px 20px',
              backgroundColor: 'transparent',
              color: 'var(--color-primary)',
              border: '1px solid var(--color-primary)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              fontWeight: 'var(--weight-semibold)',
              fontFamily: 'var(--font-mono)',
              cursor: 'pointer',
              transition: 'box-shadow 0.2s',
            }}
          >
            Import CSV
          </button>
          <button
            type="button"
            onClick={() => router.push('/people/new')}
            data-testid="add-person-button"
            style={{
              padding: '10px 20px',
              backgroundColor: 'var(--color-primary)',
              color: 'var(--color-bg-base)',
              border: 'none',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              fontWeight: 'var(--weight-semibold)',
              fontFamily: 'var(--font-mono)',
              cursor: 'pointer',
              boxShadow: 'var(--glow-primary)',
              transition: 'box-shadow 0.2s',
            }}
          >
            Add Person
          </button>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'center', marginBottom: 'var(--space-4)', flexWrap: 'wrap' }}>
        <FilterBar
          onFilterChange={(newFilters) => {
            setFilters(newFilters);
            setPage(0);
          }}
          initialTag={filters.tag}
          initialMorale={filters.morale}
        />
        <WorkspaceSelector
          workspaces={workspaces}
          selectedWorkspaceId={workspaceFilter}
          onWorkspaceChange={(wsId) => {
            setWorkspaceFilter(wsId);
            setPage(0);
          }}
        />
      </div>

      {loading && <div data-testid="loading" style={{ color: 'var(--color-text-secondary)' }}>Loading...</div>}

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-alert)', padding: 'var(--space-4)' }}>
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

      {session?.accessToken && (
        <CsvImportModal
          isOpen={showImportModal}
          onClose={() => setShowImportModal(false)}
          onImportComplete={() => {
            fetchPeople();
          }}
          token={session.accessToken as string}
        />
      )}
    </div>
  );
}
