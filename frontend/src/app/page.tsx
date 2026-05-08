'use client';

import { useSession, signIn } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export default function Home() {
  const { status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (status === 'authenticated') {
      router.replace('/people');
    }
  }, [status, router]);

  if (status === 'loading') {
    return (
      <main
        data-testid="loading"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          backgroundColor: 'var(--color-neutral-bg)',
        }}
      >
        <p style={{ color: 'var(--color-neutral-text-muted)' }}>Loading...</p>
      </main>
    );
  }

  return (
    <main
      data-testid="home-page"
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
          maxWidth: '480px',
        }}
      >
        <h1
          style={{
            fontSize: 'var(--text-h1)',
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
            fontSize: '18px',
            color: 'var(--color-neutral-text-secondary)',
            marginBottom: 'var(--space-8)',
            lineHeight: '1.5',
          }}
        >
          Your private cockpit for people context
        </p>
        <button
          type="button"
          onClick={() => signIn('oidc', { callbackUrl: '/people' })}
          data-testid="signin-button"
          style={{
            padding: '14px 32px',
            backgroundColor: 'var(--color-accent)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: '16px',
            fontWeight: 'var(--weight-semibold)',
            cursor: 'pointer',
            boxShadow: 'var(--shadow-md)',
            transition: 'background-color 0.15s, box-shadow 0.15s',
          }}
        >
          Sign in
        </button>
        <p
          style={{
            marginTop: 'var(--space-6)',
            fontSize: 'var(--text-caption)',
            color: 'var(--color-neutral-text-muted)',
          }}
        >
          Lead with memory. Act with clarity.
        </p>
      </div>
    </main>
  );
}
