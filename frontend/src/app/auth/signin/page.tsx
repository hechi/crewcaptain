'use client';

import { signIn } from 'next-auth/react';

export default function SignInPage() {
  return (
    <div
      data-testid="signin-page"
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: 'var(--space-6)',
        fontFamily: 'var(--font-ui)',
        backgroundColor: 'var(--color-neutral-bg)',
      }}
    >
      <div
        style={{
          textAlign: 'center',
          padding: 'var(--space-10)',
          backgroundColor: 'var(--color-neutral-surface)',
          borderRadius: 'var(--radius-large)',
          boxShadow: 'var(--shadow-lg)',
          maxWidth: '400px',
          width: '100%',
        }}
      >
        <h1
          style={{
            fontSize: 'var(--text-h1)',
            fontFamily: 'var(--font-heading)',
            fontWeight: 'var(--weight-bold)',
            color: 'var(--color-primary)',
            marginBottom: 'var(--space-2)',
            letterSpacing: '-0.5px',
          }}
        >
          CrewCaptain
        </h1>
        <p
          style={{
            fontSize: '16px',
            color: 'var(--color-neutral-text-secondary)',
            marginBottom: 'var(--space-8)',
          }}
        >
          Sign in to manage your team
        </p>
        <button
          type="button"
          onClick={() => signIn('oidc', { callbackUrl: '/people' })}
          data-testid="signin-button"
          style={{
            width: '100%',
            padding: '14px 24px',
            backgroundColor: 'var(--color-accent)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: '16px',
            fontWeight: 'var(--weight-semibold)',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          Sign in with SSO
        </button>
      </div>
    </div>
  );
}
