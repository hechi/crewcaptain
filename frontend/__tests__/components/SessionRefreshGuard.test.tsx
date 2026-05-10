/**
 * Tests for SessionRefreshGuard component.
 *
 * Verifies that:
 * 1. Children render normally when session is valid
 * 2. signIn is triggered when session has RefreshAccessTokenError
 * 3. Children still render while redirect is happening
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

const mockSignIn = jest.fn();
const mockUseSession = jest.fn();

jest.mock('next-auth/react', () => ({
  useSession: () => mockUseSession(),
  signIn: (...args: unknown[]) => mockSignIn(...args),
}));

import SessionRefreshGuard from '@/components/SessionRefreshGuard';

describe('SessionRefreshGuard', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render children when session is valid', () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'valid-token', user: { name: 'Test' }, expires: '' },
      status: 'authenticated',
    });

    render(
      <SessionRefreshGuard>
        <div data-testid="child-content">Hello</div>
      </SessionRefreshGuard>
    );

    expect(screen.getByTestId('child-content')).toBeInTheDocument();
    expect(mockSignIn).not.toHaveBeenCalled();
  });

  it('should render children when session is loading', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'loading',
    });

    render(
      <SessionRefreshGuard>
        <div data-testid="child-content">Hello</div>
      </SessionRefreshGuard>
    );

    expect(screen.getByTestId('child-content')).toBeInTheDocument();
    expect(mockSignIn).not.toHaveBeenCalled();
  });

  it('should render children when session is unauthenticated', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
    });

    render(
      <SessionRefreshGuard>
        <div data-testid="child-content">Hello</div>
      </SessionRefreshGuard>
    );

    expect(screen.getByTestId('child-content')).toBeInTheDocument();
    expect(mockSignIn).not.toHaveBeenCalled();
  });

  it('should trigger signIn when session has RefreshAccessTokenError', () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'old-token', error: 'RefreshAccessTokenError', user: {}, expires: '' },
      status: 'authenticated',
    });

    render(
      <SessionRefreshGuard>
        <div data-testid="child-content">Hello</div>
      </SessionRefreshGuard>
    );

    expect(mockSignIn).toHaveBeenCalledWith('oidc');
    // Children should still render while redirect is in progress
    expect(screen.getByTestId('child-content')).toBeInTheDocument();
  });

  it('should not trigger signIn when session has no error', () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'valid-token', error: undefined, user: {}, expires: '' },
      status: 'authenticated',
    });

    render(
      <SessionRefreshGuard>
        <div data-testid="child-content">Hello</div>
      </SessionRefreshGuard>
    );

    expect(mockSignIn).not.toHaveBeenCalled();
  });

  it('should not trigger signIn for other error types', () => {
    mockUseSession.mockReturnValue({
      data: { accessToken: 'token', error: 'SomeOtherError', user: {}, expires: '' },
      status: 'authenticated',
    });

    render(
      <SessionRefreshGuard>
        <div data-testid="child-content">Hello</div>
      </SessionRefreshGuard>
    );

    expect(mockSignIn).not.toHaveBeenCalled();
  });
});
