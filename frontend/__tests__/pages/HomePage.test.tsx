import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Home from '@/app/page';

const mockPush = jest.fn();
const mockReplace = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
  signIn: jest.fn(),
}));

import { useSession, signIn } from 'next-auth/react';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockSignIn = signIn as jest.MockedFunction<typeof signIn>;

describe('Home Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state when session is loading', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'loading',
      update: jest.fn(),
    });

    render(<Home />);
    expect(screen.getByTestId('loading-screen')).toBeInTheDocument();
  });

  it('should show landing page when unauthenticated', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    render(<Home />);
    expect(screen.getByTestId('landing-page')).toBeInTheDocument();
    expect(screen.getByTestId('signin-button')).toBeInTheDocument();
  });

  it('should redirect to /dashboard when authenticated', async () => {
    mockUseSession.mockReturnValue({
      data: { user: {}, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Home />);

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/dashboard');
    });
  });

  it('should call signIn with oidc provider when Get Started button is clicked', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    render(<Home />);
    screen.getByTestId('signin-button').click();
    expect(mockSignIn).toHaveBeenCalledWith('oidc', { callbackUrl: '/dashboard' });
  });

  it('should render the full landing page with all sections when unauthenticated', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    render(<Home />);
    expect(screen.getByTestId('hero-section')).toBeInTheDocument();
    expect(screen.getByTestId('features-section')).toBeInTheDocument();
    expect(screen.getByTestId('how-section')).toBeInTheDocument();
    expect(screen.getByTestId('privacy-section')).toBeInTheDocument();
    expect(screen.getByTestId('cta-section')).toBeInTheDocument();
    expect(screen.getByTestId('footer')).toBeInTheDocument();
  });
});
