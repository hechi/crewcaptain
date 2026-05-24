'use client';

import { useEffect } from 'react';
import { useSession, signOut } from 'next-auth/react';

/**
 * Component that monitors session errors and triggers sign-out when
 * token refresh fails permanently.
 *
 * This prevents users from being stuck in a "zombie" state where they
 * appear logged in but all API calls fail due to expired tokens.
 *
 * The component handles the RefreshAccessTokenError that is set when:
 * - No refresh token is available (offline_access scope not granted)
 * - Refresh token has expired (30 days in your authentik config)
 * - OIDC discovery fails repeatedly
 * - Token endpoint returns an error (e.g., invalid_grant)
 */
export default function SessionErrorHandler() {
  const { data: session, status } = useSession();

  useEffect(() => {
    if (status === 'authenticated' && session?.error === 'RefreshAccessTokenError') {
      console.warn('[SessionErrorHandler] Token refresh failed permanently, signing out');
      
      signOut({ 
        callbackUrl: '/auth/signin?error=SessionExpired',
        redirect: true 
      });
    }
  }, [session, status]);

  return null;
}
