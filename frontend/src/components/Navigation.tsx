'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { useSession, signOut } from 'next-auth/react';
import NotificationBell from './notifications/NotificationBell';

/**
 * Top navigation bar with CrewCaptain branding.
 * Includes a user menu dropdown (click username to open) with Settings and Sign out.
 * Pattern follows GitHub/Linear-style account menus.
 */
export default function Navigation() {
  const { data: session, status } = useSession();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    }
    if (menuOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [menuOpen]);

  // Close menu on Escape key
  useEffect(() => {
    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setMenuOpen(false);
      }
    }
    if (menuOpen) {
      document.addEventListener('keydown', handleEscape);
    }
    return () => {
      document.removeEventListener('keydown', handleEscape);
    };
  }, [menuOpen]);

  if (status !== 'authenticated') {
    return null;
  }

  const token = session?.accessToken || '';
  const userName = session?.user?.name || 'User';

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
            href="/triage"
            data-testid="nav-triage"
            style={{
              fontSize: '14px',
              color: 'var(--color-text-secondary)',
              textDecoration: 'none',
              fontWeight: 500,
              transition: 'color 0.2s',
            }}
          >
            Triage
          </Link>
          <Link
            href="/strategy"
            data-testid="nav-strategy"
            style={{
              fontSize: '14px',
              color: 'var(--color-text-secondary)',
              textDecoration: 'none',
              fontWeight: 500,
              transition: 'color 0.2s',
            }}
          >
            Strategy
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
          <Link
            href="/search"
            data-testid="nav-search"
            style={{
              fontSize: '14px',
              color: 'var(--color-text-secondary)',
              textDecoration: 'none',
              fontWeight: 500,
              transition: 'color 0.2s',
            }}
          >
            Search
          </Link>
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <NotificationBell token={token} />

        {/* User Menu */}
        <div ref={menuRef} style={{ position: 'relative' }}>
          <button
            type="button"
            className="dropdown-trigger"
            data-testid="nav-user-menu-trigger"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-expanded={menuOpen}
            aria-haspopup="true"
          >
            <span data-testid="nav-user-name">{userName}</span>
            <svg
              width="12"
              height="12"
              viewBox="0 0 12 12"
              fill="none"
              aria-hidden="true"
              style={{
                transform: menuOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                transition: 'transform 0.2s',
              }}
            >
              <path
                d="M3 4.5L6 7.5L9 4.5"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>

          {menuOpen && (
            <div
              className="dropdown-panel"
              data-testid="nav-user-menu"
              role="menu"
            >
              <Link
                href="/settings"
                className="dropdown-item"
                data-testid="nav-settings"
                role="menuitem"
                onClick={() => setMenuOpen(false)}
              >
                Settings
              </Link>
              <Link
                href="/my-notes"
                className="dropdown-item"
                data-testid="nav-my-notes"
                role="menuitem"
                onClick={() => setMenuOpen(false)}
              >
                My Notes
              </Link>
              <Link
                href="/audit-log"
                className="dropdown-item"
                data-testid="nav-audit-log"
                role="menuitem"
                onClick={() => setMenuOpen(false)}
              >
                Audit Log
              </Link>
              <Link
                href="/workspaces"
                className="dropdown-item"
                data-testid="nav-workspaces"
                role="menuitem"
                onClick={() => setMenuOpen(false)}
              >
                Workspaces
              </Link>
              <div className="dropdown-divider" />
              <button
                type="button"
                className="dropdown-item dropdown-item--danger"
                data-testid="nav-signout"
                role="menuitem"
                onClick={() => signOut({ callbackUrl: '/' })}
              >
                Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
