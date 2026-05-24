/**
 * Tests for SessionProvider component.
 *
 * Verifies that:
 * 1. It wraps children with NextAuth SessionProvider with appropriate refetch settings
 * 2. refetchOnWindowFocus is enabled for background token refresh
 * 3. refetchInterval is set to 2 minutes for background token refresh
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

jest.mock('next-auth/react', () => ({
  SessionProvider: ({ children, refetchInterval, refetchOnWindowFocus }: {
    children: React.ReactNode;
    refetchInterval?: number;
    refetchOnWindowFocus?: boolean;
  }) => (
    <div
      data-testid="next-auth-session-provider"
      data-refetch-interval={refetchInterval}
      data-refetch-on-window-focus={String(refetchOnWindowFocus)}
    >
      {children}
    </div>
  ),
  useSession: () => ({ data: null, status: 'loading' }),
}));

import SessionProvider from '@/components/SessionProvider';

describe('SessionProvider', () => {
  it('should render children', () => {
    render(
      <SessionProvider>
        <div data-testid="child">Content</div>
      </SessionProvider>
    );

    expect(screen.getByTestId('child')).toBeInTheDocument();
  });

  it('should configure refetchInterval to 2 minutes', () => {
    render(
      <SessionProvider>
        <div>Content</div>
      </SessionProvider>
    );

    const provider = screen.getByTestId('next-auth-session-provider');
    expect(provider.getAttribute('data-refetch-interval')).toBe('120');
  });

  it('should enable refetchOnWindowFocus for background token refresh', () => {
    render(
      <SessionProvider>
        <div>Content</div>
      </SessionProvider>
    );

    const provider = screen.getByTestId('next-auth-session-provider');
    expect(provider.getAttribute('data-refetch-on-window-focus')).toBe('true');
  });
});
