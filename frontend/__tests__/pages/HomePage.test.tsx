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
    expect(screen.getByTestId('loading')).toBeInTheDocument();
  });

  it('should show sign in button when unauthenticated', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    render(<Home />);
    expect(screen.getByTestId('home-page')).toBeInTheDocument();
    expect(screen.getByTestId('signin-button')).toBeInTheDocument();
    expect(screen.getByText('CrewCaptain')).toBeInTheDocument();
  });

  it('should redirect to /people when authenticated', async () => {
    mockUseSession.mockReturnValue({
      data: { user: {}, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Home />);

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/people');
    });
  });

  it('should call signIn with oidc provider when sign in button is clicked', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    render(<Home />);
    screen.getByTestId('signin-button').click();
    expect(mockSignIn).toHaveBeenCalledWith('oidc', { callbackUrl: '/people' });
  });
});
