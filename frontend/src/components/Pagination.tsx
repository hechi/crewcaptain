'use client';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const buttonStyle = (disabled: boolean) => ({
    padding: '8px 12px',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-medium)',
    fontSize: 'var(--text-body)',
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.5 : 1,
    background: 'var(--color-bg-elevated)',
    color: 'var(--color-primary)',
    fontWeight: 500,
    fontFamily: 'var(--font-mono)',
    transition: 'border-color 0.2s, box-shadow 0.2s',
  });

  return (
    <nav data-testid="pagination" aria-label="Pagination" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', justifyContent: 'center', marginTop: 'var(--space-6)' }}>
      <button
        type="button"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        aria-label="Previous page"
        style={buttonStyle(currentPage === 0)}
      >
        Previous
      </button>
      <span data-testid="page-info" style={{
        fontSize: 'var(--text-body)',
        color: 'var(--color-text-secondary)',
        fontFamily: 'var(--font-mono)',
      }}>
        Page {currentPage + 1} of {totalPages}
      </span>
      <button
        type="button"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages - 1}
        aria-label="Next page"
        style={buttonStyle(currentPage >= totalPages - 1)}
      >
        Next
      </button>
    </nav>
  );
}
