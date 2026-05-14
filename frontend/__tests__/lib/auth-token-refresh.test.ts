/**
 * Tests for the Auth.js token refresh logic.
 *
 * We test the authConfig callbacks directly to verify:
 * 1. Initial sign-in stores access token, refresh token, and expiry
 * 2. Subsequent requests with valid token pass through unchanged
 * 3. Expired tokens trigger a refresh using the refresh token
 * 4. Failed refresh sets an error flag on the token
 * 5. Session callback exposes the error to the client
 */

// Mock next-auth before importing auth.ts
jest.mock('next-auth', () => {
  return {
    __esModule: true,
    default: (config: unknown) => ({
      handlers: {},
      auth: jest.fn(),
      signIn: jest.fn(),
      signOut: jest.fn(),
      _config: config,
    }),
  };
});

// Mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

// Set env vars before importing
process.env.OIDC_ISSUER = 'https://auth.example.com/application/o/app/';
process.env.OIDC_CLIENT_ID = 'test-client-id';
process.env.OIDC_CLIENT_SECRET = 'test-client-secret';

import { authConfig, _resetTokenEndpointCache } from '@/auth';

describe('Auth token refresh', () => {
  const jwtCallback = authConfig.callbacks!.jwt! as Function;
  const sessionCallback = authConfig.callbacks!.session! as Function;

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    _resetTokenEndpointCache();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('jwt callback - initial sign-in', () => {
    it('should store access token, refresh token, and expiry from account', async () => {
      const token = {};
      const account = {
        access_token: 'initial-access-token',
        refresh_token: 'initial-refresh-token',
        expires_at: Math.floor(Date.now() / 1000) + 3600, // 1 hour from now
      };

      const result = await jwtCallback({ token, account });

      expect(result.accessToken).toBe('initial-access-token');
      expect(result.refreshToken).toBe('initial-refresh-token');
      expect(result.accessTokenExpires).toBe(account.expires_at * 1000);
      expect(result.error).toBeUndefined();
    });

    it('should default expiry to 1 hour if expires_at is not provided', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {};
      const account = {
        access_token: 'access-token',
        refresh_token: 'refresh-token',
        expires_at: undefined,
      };

      const result = await jwtCallback({ token, account });

      expect(result.accessTokenExpires).toBe(now + 3600 * 1000);
    });
  });

  describe('jwt callback - subsequent requests with valid token', () => {
    it('should return token unchanged when not expired', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'valid-access-token',
        refreshToken: 'refresh-token',
        accessTokenExpires: now + 10 * 60 * 1000, // 10 minutes from now
      };

      const result = await jwtCallback({ token, account: undefined });

      expect(result.accessToken).toBe('valid-access-token');
      expect(result.refreshToken).toBe('refresh-token');
      expect(result.error).toBeUndefined();
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should not refresh when token expires in more than 30 seconds', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'valid-access-token',
        refreshToken: 'refresh-token',
        accessTokenExpires: now + 31 * 1000, // 31 seconds from now
      };

      const result = await jwtCallback({ token, account: undefined });

      expect(result.accessToken).toBe('valid-access-token');
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });

  describe('jwt callback - token refresh', () => {
    it('should refresh token when expired', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'expired-access-token',
        refreshToken: 'valid-refresh-token',
        accessTokenExpires: now - 1000, // expired 1 second ago
      };

      // Mock OIDC discovery
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          token_endpoint: 'https://auth.example.com/application/o/token/',
        }),
      });

      // Mock token refresh
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          access_token: 'new-access-token',
          refresh_token: 'new-refresh-token',
          expires_in: 3600,
        }),
      });

      const result = await jwtCallback({ token, account: undefined });

      expect(result.accessToken).toBe('new-access-token');
      expect(result.refreshToken).toBe('new-refresh-token');
      expect(result.accessTokenExpires).toBe(now + 3600 * 1000);
      expect(result.error).toBeUndefined();
    });

    it('should refresh token when within 30 seconds of expiry', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'about-to-expire-token',
        refreshToken: 'valid-refresh-token',
        accessTokenExpires: now + 20 * 1000, // 20 seconds from now (within 30s buffer)
      };

      // Mock OIDC discovery
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          token_endpoint: 'https://auth.example.com/application/o/token/',
        }),
      });

      // Mock token refresh
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          access_token: 'refreshed-access-token',
          refresh_token: 'refreshed-refresh-token',
          expires_in: 3600,
        }),
      });

      const result = await jwtCallback({ token, account: undefined });

      expect(result.accessToken).toBe('refreshed-access-token');
      expect(result.refreshToken).toBe('refreshed-refresh-token');
      expect(result.error).toBeUndefined();
    });

    it('should keep existing refresh token if new one is not provided', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'expired-token',
        refreshToken: 'original-refresh-token',
        accessTokenExpires: now - 1000,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          token_endpoint: 'https://auth.example.com/application/o/token/',
        }),
      });

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          access_token: 'new-access-token',
          // No refresh_token in response
          expires_in: 3600,
        }),
      });

      const result = await jwtCallback({ token, account: undefined });

      expect(result.refreshToken).toBe('original-refresh-token');
    });

    it('should set error when refresh token is missing', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'expired-token',
        refreshToken: undefined,
        accessTokenExpires: now - 1000,
      };

      const result = await jwtCallback({ token, account: undefined });

      expect(result.error).toBe('RefreshAccessTokenError');
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should set error when OIDC discovery fails', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'expired-token',
        refreshToken: 'valid-refresh-token',
        accessTokenExpires: now - 1000,
      };

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
      });

      const result = await jwtCallback({ token, account: undefined });

      expect(result.error).toBe('RefreshAccessTokenError');
    });

    it('should set error when token endpoint returns error', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'expired-token',
        refreshToken: 'invalid-refresh-token',
        accessTokenExpires: now - 1000,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          token_endpoint: 'https://auth.example.com/application/o/token/',
        }),
      });

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          error: 'invalid_grant',
          error_description: 'Refresh token expired',
        }),
      });

      const result = await jwtCallback({ token, account: undefined });

      expect(result.error).toBe('RefreshAccessTokenError');
    });

    it('should set error when fetch throws a network error', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'expired-token',
        refreshToken: 'valid-refresh-token',
        accessTokenExpires: now - 1000,
      };

      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      const result = await jwtCallback({ token, account: undefined });

      expect(result.error).toBe('RefreshAccessTokenError');
    });

    it('should send correct parameters to token endpoint', async () => {
      const now = Date.now();
      jest.setSystemTime(now);

      const token = {
        accessToken: 'expired-token',
        refreshToken: 'my-refresh-token',
        accessTokenExpires: now - 1000,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          token_endpoint: 'https://auth.example.com/application/o/token/',
        }),
      });

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          access_token: 'new-token',
          expires_in: 3600,
        }),
      });

      await jwtCallback({ token, account: undefined });

      // Verify discovery call (includes timeout signal)
      expect(mockFetch.mock.calls[0][0]).toBe(
        'https://auth.example.com/application/o/app/.well-known/openid-configuration'
      );

      // Verify token refresh call
      const tokenCall = mockFetch.mock.calls[1];
      expect(tokenCall[0]).toBe('https://auth.example.com/application/o/token/');
      expect(tokenCall[1].method).toBe('POST');
      expect(tokenCall[1].headers['Content-Type']).toBe('application/x-www-form-urlencoded');

      const body = new URLSearchParams(tokenCall[1].body);
      expect(body.get('grant_type')).toBe('refresh_token');
      expect(body.get('client_id')).toBe('test-client-id');
      expect(body.get('client_secret')).toBe('test-client-secret');
      expect(body.get('refresh_token')).toBe('my-refresh-token');
    });
  });

  describe('session callback', () => {
    it('should expose access token to session', async () => {
      const session = { user: {}, expires: '' } as any;
      const token = { accessToken: 'my-access-token' };

      const result = await sessionCallback({ session, token });

      expect(result.accessToken).toBe('my-access-token');
    });

    it('should expose error to session when refresh fails', async () => {
      const session = { user: {}, expires: '' } as any;
      const token = { accessToken: 'old-token', error: 'RefreshAccessTokenError' };

      const result = await sessionCallback({ session, token });

      expect(result.error).toBe('RefreshAccessTokenError');
    });

    it('should not expose error when token is valid', async () => {
      const session = { user: {}, expires: '' } as any;
      const token = { accessToken: 'valid-token', error: undefined };

      const result = await sessionCallback({ session, token });

      expect(result.error).toBeUndefined();
    });
  });

  describe('provider configuration', () => {
    it('should request offline_access scope for refresh tokens', () => {
      const provider = authConfig.providers[0] as any;
      expect(provider.authorization.params.scope).toContain('offline_access');
    });
  });
});
