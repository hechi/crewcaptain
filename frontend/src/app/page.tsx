'use client';

import { useSession, signIn } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export default function Home() {
  const { status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (status === 'authenticated') {
      router.replace('/dashboard');
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
          backgroundColor: 'var(--color-bg-base)',
        }}
      >
        <p style={{ color: 'var(--color-text-muted)' }}>Loading...</p>
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
        backgroundColor: 'var(--color-bg-base)',
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
            fontFamily: 'var(--font-heading)',
            fontWeight: 'var(--weight-bold)',
            fontFamily: 'var(--font-heading)',
            color: 'var(--color-primary)',
            marginBottom: 'var(--space-2)',
            letterSpacing: '-0.5px',
            textShadow: '0 0 20px rgba(0, 240, 255, 0.3)',
          }}
        >
          CrewCaptain
        </h1>
        <p
          style={{
            fontSize: '18px',
            color: 'var(--color-text-secondary)',
            marginBottom: 'var(--space-8)',
            lineHeight: '1.5',
          }}
        >
          Your private cockpit for people context
        </p>
        <button
          type="button"
          onClick={() => signIn('oidc', { callbackUrl: '/dashboard' })}
          data-testid="signin-button"
          style={{
            padding: '14px 32px',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: '16px',
            fontWeight: 'var(--weight-bold)',
            fontFamily: 'var(--font-mono)',
            cursor: 'pointer',
            boxShadow: 'var(--glow-primary-strong)',
            transition: 'box-shadow 0.2s',
          }}
        >
          Sign in
        </button>
        <p
          style={{
            marginTop: 'var(--space-6)',
            fontSize: 'var(--text-caption)',
            color: 'var(--color-text-muted)',
            fontFamily: 'var(--font-mono)',
          }}
        >
          Lead with memory. Act with clarity.
        </p>
      </div>
    </main>
  );
}
