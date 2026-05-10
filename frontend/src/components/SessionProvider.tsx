'use client';

import { SessionProvider as NextAuthSessionProvider } from 'next-auth/react';
import { ReactNode } from 'react';
import SessionRefreshGuard from './SessionRefreshGuard';

interface Props {
  children: ReactNode;
}

export default function SessionProvider({ children }: Props) {
  return (
    <NextAuthSessionProvider refetchInterval={4 * 60} refetchOnWindowFocus={true}>
      <SessionRefreshGuard>
        {children}
      </SessionRefreshGuard>
    </NextAuthSessionProvider>
  );
}
