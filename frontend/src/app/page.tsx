'use client';

import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import LandingPage from '@/components/LandingPage';
import LoadingScreen from '@/components/LoadingScreen';
import './landing.css';

export default function Home() {
  const { status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (status === 'authenticated') {
      router.replace('/dashboard');
    }
  }, [status, router]);

  if (status === 'loading') {
    return <LoadingScreen fullScreen message="Initializing cockpit" />;
  }

  return <LandingPage />;
}
