/**
 * Tests for the middleware configuration.
 * Verifies that all authenticated routes are protected by the auth middleware.
 */

// Mock next-auth to avoid ESM import issues
jest.mock('next-auth', () => ({
  __esModule: true,
  default: () => ({
    handlers: { GET: jest.fn(), POST: jest.fn() },
    auth: jest.fn(),
    signIn: jest.fn(),
    signOut: jest.fn(),
  }),
}));

// Mock the auth module to avoid next-auth internals
jest.mock('@/auth', () => ({
  auth: jest.fn(),
  handlers: { GET: jest.fn(), POST: jest.fn() },
  signIn: jest.fn(),
  signOut: jest.fn(),
  authConfig: {},
}));

import { config } from '@/middleware';

describe('Middleware configuration', () => {
  describe('matcher patterns', () => {
    const matchers = config.matcher;

    it('should protect /people routes', () => {
      expect(matchers).toContain('/people/:path*');
    });

    it('should protect /dashboard routes', () => {
      expect(matchers).toContain('/dashboard/:path*');
    });

    it('should protect /quick-notes routes', () => {
      expect(matchers).toContain('/quick-notes/:path*');
    });

    it('should protect /search routes', () => {
      expect(matchers).toContain('/search/:path*');
    });

    it('should protect /settings routes', () => {
      expect(matchers).toContain('/settings/:path*');
    });

    it('should protect /notifications routes', () => {
      expect(matchers).toContain('/notifications/:path*');
    });

    it('should not protect the root path (redirect page)', () => {
      // The root page handles its own redirect logic
      const rootMatched = matchers.some((m: string) => m === '/' || m === '/:path*');
      expect(rootMatched).toBe(false);
    });

    it('should not protect /api/auth routes (NextAuth endpoints)', () => {
      const authApiMatched = matchers.some((m: string) => m.includes('/api/auth'));
      expect(authApiMatched).toBe(false);
    });

    it('should cover all known authenticated page routes', () => {
      const expectedRoutes = [
        '/people/:path*',
        '/dashboard/:path*',
        '/quick-notes/:path*',
        '/search/:path*',
        '/settings/:path*',
        '/notifications/:path*',
      ];
      expectedRoutes.forEach((route) => {
        expect(matchers).toContain(route);
      });
    });
  });
});
