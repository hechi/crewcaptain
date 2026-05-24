import NextAuth from 'next-auth';
import type { NextAuthConfig } from 'next-auth';

let cachedTokenEndpoint: string | null = null;

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

interface RefreshSuccess {
  accessToken: string;
  refreshToken: string;
  accessTokenExpires: number;
  error?: undefined;
  isRetryable?: undefined;
}

interface RefreshFailure {
  error: string;
  isRetryable: boolean;
  accessToken?: undefined;
  refreshToken?: undefined;
  accessTokenExpires?: undefined;
}

async function refreshAccessToken(token: {
  refreshToken: string;
  [key: string]: unknown;
}): Promise<RefreshSuccess | RefreshFailure> {
  const issuer = process.env.OIDC_ISSUER;
  if (!issuer) {
    console.error('[auth] OIDC_ISSUER not configured');
    return { error: 'RefreshAccessTokenError', isRetryable: false };
  }

  try {
    const tokenEndpoint = await discoverTokenEndpoint(issuer);
    if (!tokenEndpoint) {
      return { error: 'RefreshAccessTokenError', isRetryable: true };
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
      const isRetryable = response.status >= 500;
      if (isRetryable) {
        cachedTokenEndpoint = null;
      }
      return { error: 'RefreshAccessTokenError', isRetryable };
    }

    return {
      accessToken: refreshedTokens.access_token,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken,
      accessTokenExpires: Date.now() + (refreshedTokens.expires_in ?? 3600) * 1000,
    };
  } catch (err) {
    console.error('[auth] Token refresh error:', err);
    cachedTokenEndpoint = null;
    return { error: 'RefreshAccessTokenError', isRetryable: true };
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
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
      if (account) {
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.accessTokenExpires = account.expires_at
          ? account.expires_at * 1000
          : Date.now() + 3600 * 1000;
        token.error = undefined;
        token.refreshAttempts = 0;
        return token;
      }

      const expiresAt = (token.accessTokenExpires as number) ?? 0;
      if (Date.now() < expiresAt - 30 * 1000) {
        return token;
      }

      const refreshToken = token.refreshToken as string | undefined;
      if (!refreshToken) {
        console.error('[auth] No refresh token available — ensure offline_access scope is granted by the OIDC provider');
        token.error = 'RefreshAccessTokenError';
        return token;
      }

      const currentAttempts = (token.refreshAttempts as number) ?? 0;
      const maxRetries = 2;

      let lastError: string | undefined;

      for (let attempt = 0; attempt <= maxRetries; attempt++) {
        if (attempt > 0) {
          const delay = Math.pow(2, attempt - 1) * 1000;
          console.warn(`[auth] Token refresh attempt ${attempt}/${maxRetries} failed, retrying in ${delay}ms...`);
          await sleep(delay);
        }

        const refreshed = await refreshAccessToken({ refreshToken });
        
        if (!refreshed.error) {
          token.accessToken = refreshed.accessToken;
          token.refreshToken = refreshed.refreshToken;
          token.accessTokenExpires = refreshed.accessTokenExpires;
          token.error = undefined;
          token.refreshAttempts = 0;
          console.info('[auth] Token refreshed successfully');
          return token;
        }

        lastError = refreshed.error;

        if (!refreshed.isRetryable) {
          console.error(`[auth] Token refresh failed with non-retryable error, not retrying`);
          break;
        }
      }

      console.error(`[auth] Token refresh failed after ${maxRetries + 1} attempts, marking session as errored`);
      token.error = lastError;
      token.refreshAttempts = currentAttempts + 1;
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
