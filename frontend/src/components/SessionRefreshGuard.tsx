'use client';

import { useSession, signIn } from 'next-auth/react';
import { useEffect, ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

/**
 * Monitors the session for token refresh errors.
 * When the refresh token is expired or invalid, automatically
 * redirects the user to sign in again.
 */
export default function SessionRefreshGuard({ children }: Props) {
  const { data: session } = useSession();

  useEffect(() => {
    if (session?.error === 'RefreshAccessTokenError') {
      // The refresh token has expired or is invalid.
      // Force re-authentication silently.
      signIn('oidc');
    }
  }, [session?.error]);

  return <>{children}</>;
}
