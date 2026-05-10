import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import Navigation from '@/components/Navigation';

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
  signOut: jest.fn(),
}));

import { useSession, signOut } from 'next-auth/react';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockSignOut = signOut as jest.MockedFunction<typeof signOut>;

describe('Navigation', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should not render when unauthenticated', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    const { container } = render(<Navigation />);
    expect(container.innerHTML).toBe('');
  });

  it('should not render when loading', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'loading',
      update: jest.fn(),
    });

    const { container } = render(<Navigation />);
    expect(container.innerHTML).toBe('');
  });

  it('should render navigation when authenticated', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    expect(screen.getByTestId('navigation')).toBeInTheDocument();
  });

  it('should display brand name with link to people', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    const brandLink = screen.getByTestId('nav-brand');
    expect(brandLink).toHaveTextContent('CrewCaptain');
    expect(brandLink).toHaveAttribute('href', '/people');
  });

  it('should display People nav link', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    const peopleLink = screen.getByTestId('nav-people');
    expect(peopleLink).toHaveTextContent('People');
    expect(peopleLink).toHaveAttribute('href', '/people');
  });

  it('should display Dashboard nav link', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    const dashboardLink = screen.getByTestId('nav-dashboard');
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '/dashboard');
  });

  it('should display user name when available', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Jane Manager' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    expect(screen.getByTestId('nav-user-name')).toHaveTextContent('Jane Manager');
  });

  it('should not display user name when not available', () => {
    mockUseSession.mockReturnValue({
      data: { user: {}, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    expect(screen.queryByTestId('nav-user-name')).not.toBeInTheDocument();
  });

  it('should call signOut when sign out button is clicked', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    screen.getByTestId('nav-signout').click();
    expect(mockSignOut).toHaveBeenCalledWith({ callbackUrl: '/' });
  });

  it('should have proper aria-label for accessibility', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    expect(screen.getByLabelText('Main navigation')).toBeInTheDocument();
  });
});
