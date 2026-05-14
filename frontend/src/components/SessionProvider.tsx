'use client';

import { SessionProvider as NextAuthSessionProvider } from 'next-auth/react';
import { ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

export default function SessionProvider({ children }: Props) {
  return (
    <NextAuthSessionProvider refetchInterval={3 * 60} refetchOnWindowFocus={true}>
      {children}
    </NextAuthSessionProvider>
  );
}
