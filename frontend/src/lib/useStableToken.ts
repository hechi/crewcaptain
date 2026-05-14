'use client';

import { useSession } from 'next-auth/react';
import { useRef, useCallback } from 'react';

/**
 * Hook that provides a stable reference to the current access token.
 *
 * Unlike directly using `session?.accessToken`, this hook returns a `getToken()`
 * function with a stable reference (via useCallback + ref). This means:
 *
 * - Token refreshes (background session refetch) do NOT cause useCallback/useEffect
 *   dependency arrays to change
 * - Pages won't re-fetch data or reset form state when the token is silently refreshed
 * - The latest token is always available at call-time for API requests
 *
 * Usage:
 * ```tsx
 * const { getToken, isAuthenticated, status } = useStableToken();
 *
 * const fetchData = useCallback(async () => {
 *   const token = getToken();
 *   if (!token) return;
 *   const result = await apiCall(token, ...);
 * }, [getToken]); // getToken is stable — this won't re-run on token refresh
 * ```
 */
export function useStableToken() {
  const { data: session, status } = useSession();

  // Store the token in a ref so it's always current without triggering re-renders
  const tokenRef = useRef<string | null>(null);
  tokenRef.current = (session?.accessToken as string) ?? null;

  // Stable function reference that always returns the latest token
  const getToken = useCallback((): string | null => {
    return tokenRef.current;
  }, []);

  return {
    /** Stable function that returns the current access token. Safe to use in dependency arrays. */
    getToken,
    /** Whether the user is currently authenticated. Changes when auth status changes. */
    isAuthenticated: status === 'authenticated',
    /** Raw session status: 'loading' | 'authenticated' | 'unauthenticated' */
    status,
  };
}
