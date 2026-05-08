'use client';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <nav data-testid="pagination" aria-label="Pagination" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', justifyContent: 'center', marginTop: 'var(--space-6)' }}>
      <button
        type="button"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        aria-label="Previous page"
        style={{
          padding: '8px 12px',
          border: '1px solid var(--color-neutral-border)',
          borderRadius: 'var(--radius-medium)',
          fontSize: 'var(--text-body)',
          cursor: currentPage === 0 ? 'not-allowed' : 'pointer',
          opacity: currentPage === 0 ? 0.5 : 1,
          background: 'var(--color-neutral-surface)',
          color: 'var(--color-primary)',
          fontWeight: 'var(--weight-medium)',
        }}
      >
        Previous
      </button>
      <span data-testid="page-info" style={{ fontSize: 'var(--text-body)', color: 'var(--color-neutral-text)' }}>
        Page {currentPage + 1} of {totalPages}
      </span>
      <button
        type="button"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages - 1}
        aria-label="Next page"
        style={{
          padding: '8px 12px',
          border: '1px solid var(--color-neutral-border)',
          borderRadius: 'var(--radius-medium)',
          fontSize: 'var(--text-body)',
          cursor: currentPage >= totalPages - 1 ? 'not-allowed' : 'pointer',
          opacity: currentPage >= totalPages - 1 ? 0.5 : 1,
          background: 'var(--color-neutral-surface)',
          color: 'var(--color-primary)',
          fontWeight: 'var(--weight-medium)',
        }}
      >
        Next
      </button>
    </nav>
  );
}
