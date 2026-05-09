'use client';

import Link from 'next/link';
import { useSession, signOut } from 'next-auth/react';

/**
 * Top navigation bar with CrewCaptain branding.
 * Shows brand name (Plus Jakarta Sans heading font), nav links, and user session controls.
 */
export default function Navigation() {
  const { data: session, status } = useSession();

  if (status !== 'authenticated') {
    return null;
  }

  return (
    <nav
      data-testid="navigation"
      aria-label="Main navigation"
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 24px',
        backgroundColor: 'var(--color-primary)',
        color: '#fff',
        boxShadow: 'var(--shadow-md)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
        <Link
          href="/people"
          data-testid="nav-brand"
          style={{
            fontSize: '18px',
            fontFamily: 'var(--font-heading)',
            fontWeight: 700,
            color: '#fff',
            textDecoration: 'none',
            letterSpacing: '-0.3px',
          }}
        >
          CrewCaptain
        </Link>
        <div style={{ display: 'flex', gap: '16px' }}>
          <Link
            href="/people"
            data-testid="nav-people"
            style={{
              fontSize: '14px',
              color: 'rgba(255, 255, 255, 0.85)',
              textDecoration: 'none',
              fontWeight: 500,
              transition: 'color 100ms ease',
            }}
          >
            People
          </Link>
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {session?.user?.name && (
          <span
            data-testid="nav-user-name"
            style={{ fontSize: '13px', color: 'rgba(255, 255, 255, 0.75)' }}
          >
            {session.user.name}
          </span>
        )}
        <button
          type="button"
          onClick={() => signOut({ callbackUrl: '/' })}
          data-testid="nav-signout"
          style={{
            padding: '6px 14px',
            fontSize: '13px',
            fontWeight: 500,
            color: '#fff',
            backgroundColor: 'rgba(255, 255, 255, 0.15)',
            border: '1px solid rgba(255, 255, 255, 0.25)',
            borderRadius: 'var(--radius-small)',
            cursor: 'pointer',
            transition: 'background-color var(--transition-fast)',
          }}
        >
          Sign out
        </button>
      </div>
    </nav>
  );
}
