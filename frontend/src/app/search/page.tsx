'use client';

import { useEffect, useState, useCallback, useRef } from 'react';
import { useSession } from 'next-auth/react';
import { useSearchParams, useRouter } from 'next/navigation';
import { SearchResponse, SearchResultType } from '@/types/search';
import { search } from '@/lib/api-client';
import SearchResultCard from '@/components/search/SearchResultCard';
import Pagination from '@/components/Pagination';

const ALL_TYPES: { value: SearchResultType; label: string }[] = [
  { value: 'PERSON', label: 'People' },
  { value: 'ONE_ON_ONE_ENTRY', label: '1:1 Entries' },
  { value: 'QUICK_NOTE', label: 'Quick Notes' },
  { value: 'ACTION_ITEM', label: 'Action Items' },
  { value: 'PDP_GOAL', label: 'PDP Goals' },
  { value: 'PDP_UPDATE', label: 'PDP Updates' },
  { value: 'KUDOS', label: 'Kudos' },
];

export default function SearchPage() {
  const { data: session, status } = useSession();
  const searchParams = useSearchParams();
  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);

  const initialQuery = searchParams.get('q') || '';
  const initialPage = parseInt(searchParams.get('page') || '0', 10);
  const initialTypes = searchParams.getAll('type') as SearchResultType[];

  const [query, setQuery] = useState(initialQuery);
  const [searchResponse, setSearchResponse] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(initialPage);
  const [selectedTypes, setSelectedTypes] = useState<SearchResultType[]>(initialTypes);

  const performSearch = useCallback(async (q: string, p: number, types: SearchResultType[]) => {
    if (status !== 'authenticated' || !session?.accessToken || !q.trim()) {
      setSearchResponse(null);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const result = await search(session.accessToken as string, {
        q: q.trim(),
        type: types.length > 0 ? types : undefined,
        page: p,
        size: 20,
      });
      setSearchResponse(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed');
    } finally {
      setLoading(false);
    }
  }, [session, status]);

  // Update URL when search params change
  const updateUrl = useCallback((q: string, p: number, types: SearchResultType[]) => {
    const params = new URLSearchParams();
    if (q.trim()) params.set('q', q.trim());
    if (p > 0) params.set('page', p.toString());
    types.forEach((t) => params.append('type', t));
    const queryString = params.toString();
    router.replace(`/search${queryString ? `?${queryString}` : ''}`, { scroll: false });
  }, [router]);

  // Perform search on initial load if query exists
  useEffect(() => {
    if (initialQuery) {
      performSearch(initialQuery, initialPage, initialTypes);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Focus input on mount
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    setPage(0);
    updateUrl(query, 0, selectedTypes);
    performSearch(query, 0, selectedTypes);
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    updateUrl(query, newPage, selectedTypes);
    performSearch(query, newPage, selectedTypes);
  };

  const handleTypeToggle = (type: SearchResultType) => {
    const newTypes = selectedTypes.includes(type)
      ? selectedTypes.filter((t) => t !== type)
      : [...selectedTypes, type];
    setSelectedTypes(newTypes);
    setPage(0);
    if (query.trim()) {
      updateUrl(query, 0, newTypes);
      performSearch(query, 0, newTypes);
    }
  };

  if (status === 'loading') {
    return (
      <div
        data-testid="search-loading"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '60vh',
          color: 'var(--color-text-muted)',
        }}
      >
        Loading...
      </div>
    );
  }

  return (
    <div
      data-testid="search-page"
      style={{
        padding: 'var(--space-6)',
        maxWidth: '900px',
        margin: '0 auto',
        fontFamily: 'var(--font-ui)',
      }}
    >
      {/* Header */}
      <h1
        data-testid="search-title"
        style={{
          fontSize: 'var(--text-heading)',
          fontFamily: 'var(--font-heading)',
          fontWeight: 'var(--weight-bold)',
          color: 'var(--color-text-primary)',
          margin: '0 0 var(--space-5) 0',
        }}
      >
        Search
      </h1>

      {/* Search Form */}
      <form onSubmit={handleSubmit} style={{ marginBottom: 'var(--space-4)' }}>
        <div style={{ display: 'flex', gap: '8px' }}>
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search across all your data..."
            data-testid="search-input"
            style={{
              flex: 1,
              padding: '10px 16px',
              fontSize: '14px',
              borderRadius: 'var(--radius-medium)',
              border: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-bg-surface)',
              color: 'var(--color-text-primary)',
              outline: 'none',
              transition: 'border-color 0.2s',
            }}
          />
          <button
            type="submit"
            data-testid="search-submit"
            disabled={!query.trim() || loading}
            style={{
              padding: '10px 20px',
              fontSize: '14px',
              fontWeight: 600,
              borderRadius: 'var(--radius-medium)',
              border: '1px solid var(--color-border-glow)',
              backgroundColor: 'var(--color-primary-muted)',
              color: 'var(--color-primary)',
              cursor: query.trim() && !loading ? 'pointer' : 'not-allowed',
              opacity: query.trim() && !loading ? 1 : 0.5,
              transition: 'background-color 0.2s',
            }}
          >
            {loading ? 'Searching...' : 'Search'}
          </button>
        </div>
      </form>

      {/* Type Filters */}
      <div
        data-testid="search-type-filters"
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '8px',
          marginBottom: 'var(--space-5)',
        }}
      >
        {ALL_TYPES.map(({ value, label }) => (
          <button
            key={value}
            type="button"
            onClick={() => handleTypeToggle(value)}
            data-testid={`search-filter-${value}`}
            style={{
              padding: '4px 12px',
              fontSize: '12px',
              fontWeight: 500,
              borderRadius: '14px',
              border: `1px solid ${selectedTypes.includes(value) ? 'var(--color-primary)' : 'var(--color-border)'}`,
              backgroundColor: selectedTypes.includes(value) ? 'var(--color-primary-muted)' : 'transparent',
              color: selectedTypes.includes(value) ? 'var(--color-primary)' : 'var(--color-text-secondary)',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Error */}
      {error && (
        <div
          data-testid="search-error"
          style={{
            padding: 'var(--space-4)',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-bg-surface)',
            color: 'var(--color-alert)',
            marginBottom: 'var(--space-4)',
          }}
        >
          {error}
        </div>
      )}

      {/* Results */}
      {searchResponse && (
        <>
          <div
            data-testid="search-results-summary"
            style={{
              fontSize: '13px',
              color: 'var(--color-text-muted)',
              marginBottom: 'var(--space-4)',
            }}
          >
            {searchResponse.totalCount === 0
              ? `No results found for "${searchResponse.query}"`
              : `${searchResponse.totalCount} result${searchResponse.totalCount === 1 ? '' : 's'} for "${searchResponse.query}"`}
          </div>

          {searchResponse.results.length > 0 && (
            <div
              data-testid="search-results-list"
              style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}
            >
              {searchResponse.results.map((result) => (
                <SearchResultCard key={result.id} result={result} />
              ))}
            </div>
          )}

          {/* Empty State */}
          {searchResponse.totalCount === 0 && (
            <div
              data-testid="search-empty-state"
              style={{
                textAlign: 'center',
                padding: 'var(--space-8)',
                color: 'var(--color-text-muted)',
              }}
            >
              <p style={{ fontSize: '14px', margin: '0 0 8px 0' }}>
                No matches found. Try different keywords or remove type filters.
              </p>
            </div>
          )}

          {/* Pagination */}
          {searchResponse.totalPages > 1 && (
            <div style={{ marginTop: 'var(--space-5)' }}>
              <Pagination
                currentPage={searchResponse.page}
                totalPages={searchResponse.totalPages}
                onPageChange={handlePageChange}
              />
            </div>
          )}
        </>
      )}

      {/* Initial State (no search performed yet) */}
      {!searchResponse && !loading && !error && (
        <div
          data-testid="search-initial-state"
          style={{
            textAlign: 'center',
            padding: 'var(--space-8)',
            color: 'var(--color-text-muted)',
          }}
        >
          <p style={{ fontSize: '14px', margin: 0 }}>
            Search across people, 1:1 notes, quick notes, action items, PDP goals, and kudos.
          </p>
        </div>
      )}
    </div>
  );
}
