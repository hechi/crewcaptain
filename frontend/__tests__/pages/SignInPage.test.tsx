import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import SignInPage from '@/app/auth/signin/page';

jest.mock('next-auth/react', () => ({
  signIn: jest.fn(),
}));

import { signIn } from 'next-auth/react';

const mockSignIn = signIn as jest.MockedFunction<typeof signIn>;

describe('SignIn Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the sign in page with title and button', () => {
    render(<SignInPage />);
    expect(screen.getByTestId('signin-page')).toBeInTheDocument();
    expect(screen.getByText('CrewCaptain')).toBeInTheDocument();
    expect(screen.getByText('Sign in to manage your team')).toBeInTheDocument();
    expect(screen.getByTestId('signin-button')).toBeInTheDocument();
  });

  it('should call signIn with oidc provider when button is clicked', () => {
    render(<SignInPage />);
    screen.getByTestId('signin-button').click();
    expect(mockSignIn).toHaveBeenCalledWith('oidc', { callbackUrl: '/people' });
  });
});
