import NextAuth from 'next-auth';
import type { NextAuthConfig } from 'next-auth';

// Cache the token endpoint to avoid OIDC discovery on every refresh
let cachedTokenEndpoint: string | null = null;

/** Reset the cached token endpoint (exported for testing) */
export function _resetTokenEndpointCache() {
  cachedTokenEndpoint = null;
}

async function discoverTokenEndpoint(issuer: string): Promise<string | null> {
  if (cachedTokenEndpoint) return cachedTokenEndpoint;

  try {
    const wellKnownUrl = issuer.endsWith('/')
      ? `${issuer}.well-known/openid-configuration`
      : `${issuer}/.well-known/openid-configuration`;

    const discoveryResponse = await fetch(wellKnownUrl, { 
      signal: AbortSignal.timeout(5000),
    });
    if (!discoveryResponse.ok) {
      console.error(`[auth] OIDC discovery failed: ${discoveryResponse.status} ${discoveryResponse.statusText}`);
      return null;
    }
    const discovery = await discoveryResponse.json();
    cachedTokenEndpoint = discovery.token_endpoint || null;
    return cachedTokenEndpoint;
  } catch (err) {
    console.error('[auth] OIDC discovery error:', err);
    return null;
  }
}

async function refreshAccessToken(token: {
  refreshToken: string;
  [key: string]: unknown;
}): Promise<{
  accessToken: string;
  refreshToken: string;
  accessTokenExpires: number;
  error?: undefined;
} | {
  error: string;
  accessToken?: undefined;
  refreshToken?: undefined;
  accessTokenExpires?: undefined;
}> {
  const issuer = process.env.OIDC_ISSUER;
  if (!issuer) {
    console.error('[auth] OIDC_ISSUER not configured');
    return { error: 'RefreshAccessTokenError' };
  }

  try {
    const tokenEndpoint = await discoverTokenEndpoint(issuer);
    if (!tokenEndpoint) {
      return { error: 'RefreshAccessTokenError' };
    }

    const response = await fetch(tokenEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: process.env.OIDC_CLIENT_ID || '',
        client_secret: process.env.OIDC_CLIENT_SECRET || '',
        refresh_token: token.refreshToken,
      }),
      signal: AbortSignal.timeout(10000),
    });

    const refreshedTokens = await response.json();

    if (!response.ok) {
      console.error('[auth] Token refresh failed:', response.status, refreshedTokens);
      // Clear cached endpoint in case it's stale
      cachedTokenEndpoint = null;
      return { error: 'RefreshAccessTokenError' };
    }

    return {
      accessToken: refreshedTokens.access_token,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken,
      accessTokenExpires: Date.now() + (refreshedTokens.expires_in ?? 3600) * 1000,
    };
  } catch (err) {
    console.error('[auth] Token refresh error:', err);
    // Clear cached endpoint in case it's stale
    cachedTokenEndpoint = null;
    return { error: 'RefreshAccessTokenError' };
  }
}

export const authConfig: NextAuthConfig = {
  providers: [
    {
      id: 'oidc',
      name: 'OIDC',
      type: 'oidc',
      issuer: process.env.OIDC_ISSUER,
      clientId: process.env.OIDC_CLIENT_ID,
      clientSecret: process.env.OIDC_CLIENT_SECRET,
      authorization: { params: { scope: 'openid email profile offline_access' } },
    },
  ],
  callbacks: {
    async jwt({ token, account }) {
      // Initial sign-in: persist tokens and expiry from the OIDC provider
      if (account) {
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.accessTokenExpires = account.expires_at
          ? account.expires_at * 1000
          : Date.now() + 3600 * 1000;
        token.error = undefined;
        return token;
      }

      // Subsequent requests: check if access token is still valid
      // Refresh 30 seconds before actual expiry to avoid edge-case failures
      const expiresAt = (token.accessTokenExpires as number) ?? 0;
      if (Date.now() < expiresAt - 30 * 1000) {
        // Token is still valid
        return token;
      }

      // Token has expired (or is about to) — attempt refresh
      const refreshToken = token.refreshToken as string | undefined;
      if (!refreshToken) {
        console.error('[auth] No refresh token available — ensure offline_access scope is granted by the OIDC provider');
        token.error = 'RefreshAccessTokenError';
        return token;
      }

      const refreshed = await refreshAccessToken({ refreshToken });
      if (refreshed.error) {
        token.error = 'RefreshAccessTokenError';
        return token;
      }

      token.accessToken = refreshed.accessToken;
      token.refreshToken = refreshed.refreshToken;
      token.accessTokenExpires = refreshed.accessTokenExpires;
      token.error = undefined;
      return token;
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken as string;
      session.error = token.error as string | undefined;
      return session;
    },
  },
  pages: {
    signIn: '/auth/signin',
  },
};

export const { handlers, auth, signIn, signOut } = NextAuth(authConfig);
