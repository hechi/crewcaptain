import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import SessionProvider from '@/components/SessionProvider';

jest.mock('next-auth/react', () => ({
  SessionProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="session-provider">{children}</div>
  ),
}));

describe('SessionProvider', () => {
  it('should wrap children with NextAuth SessionProvider', () => {
    render(
      <SessionProvider>
        <div data-testid="child">Hello</div>
      </SessionProvider>
    );

    expect(screen.getByTestId('session-provider')).toBeInTheDocument();
    expect(screen.getByTestId('child')).toBeInTheDocument();
  });
});
