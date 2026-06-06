'use client';

import Link from 'next/link';

export default function TriageEmptyState() {
  return (
    <div
      data-testid="triage-empty-state"
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 'var(--space-12) var(--space-6)',
        background: 'var(--glass-bg)',
        backdropFilter: 'var(--glass-blur)',
        border: 'var(--glass-border)',
        borderRadius: 'var(--radius-large)',
        textAlign: 'center',
      }}
    >
      <div
        style={{
          fontSize: '48px',
          marginBottom: 'var(--space-4)',
        }}
        aria-hidden="true"
      >
        ✨
      </div>
      <h2
        style={{
          fontSize: 'var(--text-h3)',
          fontFamily: 'var(--font-heading)',
          fontWeight: 'var(--weight-semibold)',
          color: 'var(--color-text-primary)',
          margin: '0 0 var(--space-2) 0',
        }}
      >
        You&apos;re all clear
      </h2>
      <p
        style={{
          fontSize: 'var(--text-body)',
          color: 'var(--color-text-secondary)',
          margin: '0 0 var(--space-5) 0',
        }}
      >
        No overdue items, stale meetings, or upcoming anniversaries need attention right now.
      </p>
      <Link
        href="/people"
        style={{
          padding: 'var(--space-2) var(--space-5)',
          borderRadius: 'var(--radius-medium)',
          backgroundColor: 'var(--color-primary-muted)',
          color: 'var(--color-primary)',
          fontFamily: 'var(--font-mono)',
          fontSize: 'var(--text-small)',
          fontWeight: 'var(--weight-medium)',
          textDecoration: 'none',
          border: '1px solid var(--color-border-glow)',
          transition: 'box-shadow 0.2s',
        }}
      >
        Open People
      </Link>
    </div>
  );
}
