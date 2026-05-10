'use client';

import Link from 'next/link';
import { useSession, signOut } from 'next-auth/react';
import NotificationBell from './notifications/NotificationBell';

/**
 * Top navigation bar with CrewCaptain branding.
 * Dark theme with subtle bottom glow border and monospace brand name.
 */
export default function Navigation() {
  const { data: session, status } = useSession();

  if (status !== 'authenticated') {
    return null;
  }

  const token = session?.accessToken || '';

  return (
    <nav
      data-testid="navigation"
      aria-label="Main navigation"
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 24px',
        backgroundColor: 'var(--color-bg-surface)',
        color: 'var(--color-text-primary)',
        borderBottom: '1px solid var(--color-border)',
        boxShadow: '0 1px 8px rgba(0, 240, 255, 0.05)',
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
            color: 'var(--color-primary)',
            textDecoration: 'none',
            letterSpacing: '-0.5px',
          }}
        >
          CrewCaptain
        </Link>
        <div style={{ display: 'flex', gap: '16px' }}>
          <Link
            href="/dashboard"
            data-testid="nav-dashboard"
            style={{
              fontSize: '14px',
              color: 'var(--color-text-secondary)',
              textDecoration: 'none',
              fontWeight: 500,
              transition: 'color 0.2s',
            }}
          >
            Dashboard
          </Link>
          <Link
            href="/people"
            data-testid="nav-people"
            style={{
              fontSize: '14px',
              color: 'var(--color-text-secondary)',
              textDecoration: 'none',
              fontWeight: 500,
              transition: 'color 0.2s',
            }}
          >
            People
          </Link>
          <Link
            href="/quick-notes"
            data-testid="nav-quick-notes"
            style={{
              fontSize: '14px',
              color: 'var(--color-text-secondary)',
              textDecoration: 'none',
              fontWeight: 500,
              transition: 'color 0.2s',
            }}
          >
            Quick Notes
          </Link>
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <NotificationBell token={token} />
        {session?.user?.name && (
          <span
            data-testid="nav-user-name"
            style={{
              fontSize: '13px',
              color: 'var(--color-text-secondary)',
              fontFamily: 'var(--font-mono)',
            }}
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
            color: 'var(--color-primary)',
            backgroundColor: 'var(--color-primary-muted)',
            border: '1px solid var(--color-border-glow)',
            borderRadius: 'var(--radius-small)',
            cursor: 'pointer',
            transition: 'background-color 0.2s, box-shadow 0.2s',
          }}
        >
          Sign out
        </button>
      </div>
    </nav>
  );
}
