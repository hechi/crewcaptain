/**
 * Tests for SessionErrorHandler component.
 *
 * This component monitors the session for RefreshAccessTokenError
 * and triggers signOut when detected, preventing users from being
 * stuck in a "zombie" authenticated state with broken tokens.
 */

import React from 'react';
import { render, waitFor } from '@testing-library/react';
import { useSession, signOut } from 'next-auth/react';
import SessionErrorHandler from '@/components/SessionErrorHandler';

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
  signOut: jest.fn(),
}));

const mockedUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockedSignOut = signOut as jest.MockedFunction<typeof signOut>;

describe('SessionErrorHandler', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders nothing (returns null)', () => {
    mockedUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    const { container } = render(<SessionErrorHandler />);
    expect(container.firstChild).toBeNull();
  });

  it('does not sign out when session is valid', () => {
    mockedUseSession.mockReturnValue({
      data: {
        accessToken: 'valid-token',
        expires: 'future-date',
      },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<SessionErrorHandler />);

    expect(mockedSignOut).not.toHaveBeenCalled();
  });

  it('does not sign out when session has no error', () => {
    mockedUseSession.mockReturnValue({
      data: {
        accessToken: 'valid-token',
        expires: 'future-date',
        error: undefined,
      },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<SessionErrorHandler />);

    expect(mockedSignOut).not.toHaveBeenCalled();
  });

  it('does not sign out when status is loading', () => {
    mockedUseSession.mockReturnValue({
      data: null,
      status: 'loading',
      update: jest.fn(),
    });

    render(<SessionErrorHandler />);

    expect(mockedSignOut).not.toHaveBeenCalled();
  });

  it('does not sign out when status is unauthenticated', () => {
    mockedUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    render(<SessionErrorHandler />);

    expect(mockedSignOut).not.toHaveBeenCalled();
  });

  it('signs out when RefreshAccessTokenError is present', async () => {
    mockedUseSession.mockReturnValue({
      data: {
        accessToken: 'expired-token',
        expires: 'past-date',
        error: 'RefreshAccessTokenError',
      },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<SessionErrorHandler />);

    await waitFor(() => {
      expect(mockedSignOut).toHaveBeenCalledWith({
        callbackUrl: '/auth/signin?error=SessionExpired',
        redirect: true,
      });
    });
  });

  it('signs out immediately when error appears after initial valid session', async () => {
    mockedUseSession.mockReturnValue({
      data: {
        accessToken: 'valid-token',
        expires: 'future-date',
      },
      status: 'authenticated',
      update: jest.fn(),
    });

    const { rerender } = render(<SessionErrorHandler />);

    expect(mockedSignOut).not.toHaveBeenCalled();

    mockedUseSession.mockReturnValue({
      data: {
        accessToken: 'expired-token',
        expires: 'past-date',
        error: 'RefreshAccessTokenError',
      },
      status: 'authenticated',
      update: jest.fn(),
    });

    rerender(<SessionErrorHandler />);

    await waitFor(() => {
      expect(mockedSignOut).toHaveBeenCalledWith({
        callbackUrl: '/auth/signin?error=SessionExpired',
        redirect: true,
      });
    });
  });

  it('logs a warning before signing out', async () => {
    const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

    mockedUseSession.mockReturnValue({
      data: {
        accessToken: 'expired-token',
        expires: 'past-date',
        error: 'RefreshAccessTokenError',
      },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<SessionErrorHandler />);

    await waitFor(() => {
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        '[SessionErrorHandler] Token refresh failed permanently, signing out'
      );
    });

    consoleWarnSpy.mockRestore();
  });

  it('does not sign out for other error types', () => {
    mockedUseSession.mockReturnValue({
      data: {
        accessToken: 'some-token',
        expires: 'future-date',
        error: 'SomeOtherError',
      },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<SessionErrorHandler />);

    expect(mockedSignOut).not.toHaveBeenCalled();
  });
});
